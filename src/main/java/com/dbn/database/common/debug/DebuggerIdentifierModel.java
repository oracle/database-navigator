/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.database.common.debug;

import com.dbn.data.type.DBDataType;
import com.dbn.object.DBType;
import com.dbn.object.DBTypeAttribute;
import com.dbn.object.lookup.DBObjectRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static com.dbn.common.util.Strings.cachedUpperCase;
import static com.dbn.common.util.Strings.equalsIgnoreCase;

/**
 * PL/Scope identifier metadata for one stored program unit.
 *
 * <p>The model resolves the active program context and exposes declarations
 * belonging to that context and its enclosing contexts. This keeps variables
 * from sibling procedures out of a debug frame while retaining package-level
 * declarations.</p>
 */
public final class DebuggerIdentifierModel {
    private final Map<String, DebuggerIdentifierInfo> identifiers;
    private final Map<String, DBObjectRef<DBType>> types;

    public DebuggerIdentifierModel(List<DebuggerIdentifierInfo> identifiers) {
        this.identifiers = new LinkedHashMap<>();
        this.types = new HashMap<>();
        for (DebuggerIdentifierInfo identifier : identifiers) {
            this.identifiers.put(key(identifier.getObjectType(), identifier.getUsageId()), identifier);
        }
    }

    public static DebuggerIdentifierModel empty() {
        return new DebuggerIdentifierModel(Collections.emptyList());
    }

    public boolean isEmpty() {
        return identifiers.isEmpty();
    }

    public List<DebuggerIdentifierInfo> getVisibleVariables(String scopeName, String scopeType, int currentLine) {
        DebuggerIdentifierInfo scope = findScope(scopeName, scopeType);
        return getVisibleVariables(scope, currentLine);
    }

    /**
     * Resolves the innermost procedure or function whose declaration precedes the current line.
     * This is useful when the source position points at a closing {@code END} line and PSI cannot
     * resolve the enclosing subject reliably.
     */
    public List<DebuggerIdentifierInfo> getVisibleVariables(int currentLine, String scopeType) {
        DebuggerIdentifierInfo scope = findScope(currentLine, scopeType);
        return getVisibleVariables(scope, currentLine);
    }

    private List<DebuggerIdentifierInfo> getVisibleVariables(DebuggerIdentifierInfo scope, int currentLine) {
        if (scope == null) return Collections.emptyList();

        List<Integer> contextIds = getContextIds(scope);
        Map<String, DebuggerIdentifierInfo> variables = new LinkedHashMap<>();
        for (Integer contextId : contextIds) {
            for (DebuggerIdentifierInfo identifier : identifiers.values()) {
                if (!equalsIgnoreCase(identifier.getObjectType(), scope.getObjectType())) continue;
                if (identifier.getUsageContextId() != contextId) continue;
                if (!identifier.isValueDeclaration()) continue;
                if (identifier.getLine() > currentLine) continue;

                // The innermost context wins when a variable shadows an outer one.
                variables.putIfAbsent(cachedUpperCase(identifier.getName()), identifier);
            }
        }
        return new ArrayList<>(variables.values());
    }

    /**
     * Resolves all declared identifier types and loads their complete attribute and collection type graphs.
     * Types are registered only after the preload completes so debugger value rendering never triggers lazy
     * database access on the UI thread.
     *
     * @param resolver resolves a PL/Scope type identifier to its database object
     */
    public void preloadTypes(@NotNull Function<DebuggerIdentifierInfo, DBType> resolver) {
        Map<DebuggerIdentifierInfo, DBType> resolvedTypes = new LinkedHashMap<>();

        for (DebuggerIdentifierInfo typeIdentifier : getTypeIdentifiers()) {
            if (hasType(typeIdentifier)) continue;

            DBType type = resolver.apply(typeIdentifier);
            if (type != null) {
                resolvedTypes.put(typeIdentifier, type);
            }
        }

        Set<DBObjectRef<DBType>> preloadedTypes = new HashSet<>();
        for (DBType type : resolvedTypes.values()) {
            preloadType(type, preloadedTypes);
        }

        resolvedTypes.forEach(this::registerType);
    }

    private List<DebuggerIdentifierInfo> getTypeIdentifiers() {
        List<DebuggerIdentifierInfo> typeIdentifiers = new ArrayList<>();
        for (DebuggerIdentifierInfo identifier : identifiers.values()) {
            if (identifier.isType()) typeIdentifiers.add(identifier);
        }
        return typeIdentifiers;
    }

    private void registerType(DebuggerIdentifierInfo identifier, @NotNull DBType type) {
        String typeIdentifier = identifier.getTypeQualifiedName();
        if (typeIdentifier != null) {
            types.put(typeIdentifier, DBObjectRef.of(type));
        }
    }

    private boolean hasType(DebuggerIdentifierInfo identifier) {
        String typeIdentifier = identifier.getTypeQualifiedName();
        return typeIdentifier != null && types.containsKey(typeIdentifier);
    }

    private void preloadType(@Nullable DBType type, Set<DBObjectRef<DBType>> preloadedTypes) {
        if (type == null) return;

        DBObjectRef<DBType> typeRef = DBObjectRef.of(type);
        if (!preloadedTypes.add(typeRef)) return;

        for (DBTypeAttribute attribute : type.getAttributes()) {
            preloadType(attribute.getDataType().getDeclaredType(), preloadedTypes);
        }

        if (type.isCollection()) {
            DBDataType elementType = type.getCollectionElementType();
            if (elementType != null) {
                preloadType(elementType.getDeclaredType(), preloadedTypes);
            }
        }
    }

    public boolean isTypeVariable(DebuggerIdentifierInfo identifier) {
        return getTypeIdentifier(identifier) != null;
    }

    @Nullable
    public DBType getType(DebuggerIdentifierInfo identifier) {
        DebuggerIdentifierInfo typeIdentifier = getTypeIdentifier(identifier);
        if (typeIdentifier == null) return null;

        String qualifiedName = typeIdentifier.getTypeQualifiedName();
        DBObjectRef<DBType> type = types.get(qualifiedName);
        return type == null ? null : type.value();
    }

    @Nullable
    private DebuggerIdentifierInfo getTypeIdentifier(DebuggerIdentifierInfo identifier) {
        for (DebuggerIdentifierInfo child : identifiers.values()) {
            if (!equalsIgnoreCase(child.getObjectType(), identifier.getObjectType())) continue;
            if (child.getUsageContextId() != identifier.getUsageId()) continue;
            if (child.isType()) return child;
        }
        return null;
    }

    private DebuggerIdentifierInfo findScope(String scopeName, String scopeType) {
        DebuggerIdentifierInfo declaration = null;
        for (DebuggerIdentifierInfo identifier : identifiers.values()) {
            if (!identifier.isScope()) continue;
            if (!identifier.matches(scopeName, scopeType)) continue;

            if (identifier.isDefinition()) return identifier;
            if (identifier.isDeclaration()) declaration = identifier;
        }
        return declaration;
    }

    private DebuggerIdentifierInfo findScope(int currentLine, String scopeType) {
        DebuggerIdentifierInfo scope = null;
        for (DebuggerIdentifierInfo identifier : identifiers.values()) {
            if (!identifier.isScope()) continue;
            if (!identifier.isDefinition()) continue;
            if (!equalsIgnoreCase(identifier.getObjectType(), scopeType)) continue;
            if (identifier.getLine() > currentLine) continue;

            if (scope == null || identifier.getLine() > scope.getLine()) {
                scope = identifier;
            }
        }
        return scope;
    }

    private List<Integer> getContextIds(DebuggerIdentifierInfo scope) {
        List<Integer> contextIds = new ArrayList<>();
        DebuggerIdentifierInfo context = scope;
        while (context != null && context.getUsageId() != 0) {
            contextIds.add(context.getUsageId());
            context = identifiers.get(key(context.getObjectType(), context.getUsageContextId()));
        }
        return contextIds;
    }

    private static String key(String objectType, int usageId) {
        return objectType + ':' + usageId;
    }
}
