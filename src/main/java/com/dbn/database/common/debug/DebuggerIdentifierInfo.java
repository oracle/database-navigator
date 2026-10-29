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

import lombok.Getter;
import org.jetbrains.annotations.NonNls;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;

import static com.dbn.common.util.Strings.equalsIgnoreCase;

/**
 * A single PL/Scope identifier usage.
 *
 * <p>The information is static metadata. It identifies a variable or its
 * scope, but does not contain the variable's runtime value.</p>
 */
@NonNls
@Getter
public final class DebuggerIdentifierInfo {
    private static final Set<String> PACKAGE_TYPE_TYPES = Set.of(
            "ASSOCIATIVE ARRAY",
            "INDEX TABLE",
            "NESTED TABLE",
            "RECORD",
            "REF CURSOR",
            "REFCURSOR",
            "SUBTYPE",
            "VARRAY");
    private static final Set<String> SCOPE_TYPES = Set.of("FUNCTION", "PROCEDURE");
    private static final Set<String> VALUE_TYPES = Set.of(
            "CONSTANT",
            "CURSOR",
            "FORMAL IN",
            "FORMAL IN OUT",
            "FORMAL OUT",
            "ITERATOR",
            "RECORD",
            "REF CURSOR",
            "REFCURSOR",
            "VARIABLE");

    private final String owner;
    private final String name;
    private final String signature;
    private final String type;
    private final String objectName;
    private final String objectType;
    private final String usage;
    private final int usageId;
    private final int line;
    private final int column;
    private final int usageContextId;
    private final String declaredOwner;
    private final String declaredObjectName;
    private final String declaredObjectType;

    private DebuggerIdentifierInfo(
            String owner,
            String name,
            String signature,
            String type,
            String objectName,
            String objectType,
            String usage,
            int usageId,
            int line,
            int column,
            int usageContextId,
            String declaredOwner,
            String declaredObjectName,
            String declaredObjectType) {
        this.owner = owner;
        this.name = name;
        this.signature = signature;
        this.type = type;
        this.objectName = objectName;
        this.objectType = objectType;
        this.usage = usage;
        this.usageId = usageId;
        this.line = line;
        this.column = column;
        this.usageContextId = usageContextId;
        this.declaredOwner = declaredOwner;
        this.declaredObjectName = declaredObjectName;
        this.declaredObjectType = declaredObjectType;
    }

    public static DebuggerIdentifierInfo read(ResultSet resultSet) throws SQLException {
        return new DebuggerIdentifierInfo(
                resultSet.getString("OWNER"),
                resultSet.getString("NAME"),
                resultSet.getString("SIGNATURE"),
                resultSet.getString("TYPE"),
                resultSet.getString("OBJECT_NAME"),
                resultSet.getString("OBJECT_TYPE"),
                resultSet.getString("USAGE"),
                resultSet.getInt("USAGE_ID"),
                resultSet.getInt("LINE"),
                resultSet.getInt("COL"),
                resultSet.getInt("USAGE_CONTEXT_ID"),
                resultSet.getString("DECLARED_OWNER"),
                resultSet.getString("DECLARED_OBJECT_NAME"),
                resultSet.getString("DECLARED_OBJECT_TYPE"));
    }

    public boolean isDeclaration() {
        return equalsIgnoreCase(usage, "DECLARATION");
    }

    public boolean isDefinition() {
        return equalsIgnoreCase(usage, "DEFINITION");
    }

    public boolean isType() {
        return equalsIgnoreCase(declaredObjectType, "TYPE") || isPackageType();
    }

    public boolean isPackageType() {
        return equalsIgnoreCase(declaredObjectType, "PACKAGE") && hasType(PACKAGE_TYPE_TYPES);
    }

    public String getTypeName() {
        return isPackageType() ? name : declaredObjectName;
    }

    public String getTypePackageName() {
        return isPackageType() ? declaredObjectName : null;
    }

    public String getTypeQualifiedName() {
        String typeName = getTypeName();
        if (typeName == null) return null;

        String packageName = getTypePackageName();
        String qualifiedName = packageName == null ? typeName : packageName + "." + typeName;
        return declaredOwner == null ? qualifiedName : declaredOwner + "." + qualifiedName;
    }

    public boolean isScope() {
        return hasType(SCOPE_TYPES);
    }

    public boolean isValueDeclaration() {
        return isDeclaration() && hasType(VALUE_TYPES);
    }

    public boolean isFormalParameter() {
        return type != null && type.toUpperCase(Locale.ROOT).startsWith("FORMAL ");
    }

    public boolean isCursor() {
        return equalsIgnoreCase(type, "CURSOR") ||
               equalsIgnoreCase(type, "REF CURSOR") ||
               equalsIgnoreCase(type, "REFCURSOR");
    }

    public boolean matches(String identifierName, String identifierType) {
        return equalsIgnoreCase(name, identifierName) && equalsIgnoreCase(objectType, identifierType);
    }

    private boolean hasType(Set<String> types) {
        return type != null && types.contains(type.toUpperCase(Locale.ROOT));
    }
}
