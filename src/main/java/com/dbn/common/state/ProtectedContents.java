/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.common.state;

import com.dbn.common.util.Strings;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.state.StateEncryptionScopes.EXECUTION_STATEMENT_VARIABLE;
import static com.dbn.common.state.StateEncryptionScopes.EXECUTION_VARIABLE_EXPRESSION;
import static com.dbn.common.state.StateEncryptionScopes.EXECUTION_VARIABLE_VALUE;
import static com.dbn.common.util.Strings.isNotEmpty;

public final class ProtectedContents implements Iterable<ProtectedContent> {
    private final List<ProtectedContent> contents = new ArrayList<>();
    private final @NonNls String encryptionScope;
    private final int limit;

    private ProtectedContents(@NonNls String encryptionScope, int limit) {
        this.encryptionScope = encryptionScope;
        this.limit = limit;
    }

    public static ProtectedContents executionVariableValues() {
        return new ProtectedContents(EXECUTION_VARIABLE_VALUE, 10);
    }

    public static ProtectedContents executionVariableExpressions() {
        return new ProtectedContents(EXECUTION_VARIABLE_EXPRESSION, 10);
    }

    public static ProtectedContents statementExecutionVariableValues() {
        return new ProtectedContents(EXECUTION_STATEMENT_VARIABLE, 10);
    }

    @Nullable
    public String getValue() {
        ProtectedContent value = contents.isEmpty() ? null : contents.get(0);
        if (value == null || value.isNullMarker()) return null;

        String stringValue = value.get();
        return Strings.isEmpty(stringValue) ? null : stringValue;
    }

    public void setValue(@Nullable String value) {
        if (Strings.isEmpty(value)) {
            contents.removeIf(v -> v.isNullMarker() || v.isEmpty());
            add(0, ProtectedContent.nullMarker(encryptionScope));
        } else {
            contents.removeIf(v -> v.isNullMarker() || v.isEmpty() || Objects.equals(v.get(), value));
            add(0, newContent(value));
        }
    }

    public ProtectedContent newContent() {
        return new ProtectedContent(encryptionScope);
    }

    public ProtectedContent newContent(@Nullable String value) {
        return new ProtectedContent(encryptionScope, value);
    }

    public boolean add(ProtectedContent value) {
        if (value == null || value.isEmpty()) return false;

        boolean added = contents.add(value);
        trim();
        return added;
    }

    public void add(int index, ProtectedContent value) {
        if (value == null || value.isEmpty()) return;

        contents.add(index, value);
        trim();
    }

    public boolean addAll(Collection<? extends ProtectedContent> values) {
        boolean changed = false;
        for (ProtectedContent value : values) {
            changed |= add(value);
        }
        return changed;
    }

    public boolean addAll(int index, Collection<? extends ProtectedContent> values) {
        boolean changed = false;
        for (ProtectedContent value : values) {
            if (value == null || value.isEmpty()) continue;

            contents.add(index++, value);
            changed = true;
        }
        trim();
        return changed;
    }

    public List<String> values() {
        List<String> values = new ArrayList<>();
        for (ProtectedContent content : contents) {
            String value = content.get();
            if (isNotEmpty(value)) {
                values.add(value);
            }
        }
        return values;
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    public void clear() {
        contents.clear();
    }

    public void copyFrom(ProtectedContents source) {
        if (source == this) return;

        List<ProtectedContent> sourceContents = new ArrayList<>(source.contents);
        clear();
        for (ProtectedContent content : sourceContents) {
            if (content.isNullMarker()) {
                add(ProtectedContent.nullMarker(encryptionScope));
            } else if (!content.isEmpty()) {
                add(newContent(content.get()));
            }
        }
        trim();
    }

    @Override
    public Iterator<ProtectedContent> iterator() {
        return contents.iterator();
    }

    private void trim() {
        int valueCount = 0;
        for (ProtectedContent content : contents) {
            if (!content.isNullMarker()) valueCount++;
        }

        for (int i = contents.size() - 1; valueCount > limit && i >= 0; i--) {
            ProtectedContent content = contents.get(i);
            if (content.isNullMarker()) continue;

            contents.remove(i);
            valueCount--;
        }
    }
}
