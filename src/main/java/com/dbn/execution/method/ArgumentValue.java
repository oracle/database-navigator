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

package com.dbn.execution.method;

import com.dbn.data.type.DBDataType;
import com.dbn.execution.common.input.ValueHolder;
import com.dbn.object.DBArgument;
import com.dbn.object.DBTypeAttribute;
import com.dbn.object.lookup.DBObjectRef;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;

@Getter
@Setter
public class ArgumentValue {
    private final DBObjectRef<DBArgument> argument;
    private final List<DBObjectRef<DBTypeAttribute>> attributePath;
    private ValueHolder valueHolder;

    public ArgumentValue(@NotNull DBArgument argument, @Nullable DBTypeAttribute attribute, ValueHolder valueHolder) {
        this(argument, attribute == null ? emptyList() : Collections.singletonList(attribute), valueHolder);
    }

    public ArgumentValue(@NotNull DBArgument argument, @NotNull List<DBTypeAttribute> attributePath, ValueHolder valueHolder) {
        this.argument = DBObjectRef.of(argument);
        List<DBObjectRef<DBTypeAttribute>> attributePathRefs = new ArrayList<>(attributePath.size());
        for (DBTypeAttribute attribute : attributePath) {
            attributePathRefs.add(DBObjectRef.of(attribute));
        }
        this.attributePath = attributePathRefs;
        this.valueHolder = valueHolder;
    }

    public ArgumentValue(@NotNull DBArgument argument, ValueHolder valueHolder) {
        this(argument, emptyList(), valueHolder);
    }

    @Nullable
    public DBArgument getArgument() {
        return argument.get();
    }

    public DBObjectRef<DBArgument> getArgumentRef() {
        return argument;
    }

    @Nullable
    public DBTypeAttribute getAttribute() {
        return attributePath.isEmpty() ? null : DBObjectRef.get(attributePath.get(attributePath.size() - 1));
    }

    @NotNull
    public List<DBTypeAttribute> getAttributePath() {
        if (attributePath.isEmpty()) return emptyList();

        List<DBTypeAttribute> resolvedAttributePath = new ArrayList<>(attributePath.size());
        for (DBObjectRef<DBTypeAttribute> attributeRef : attributePath) {
            DBTypeAttribute attribute = DBObjectRef.get(attributeRef);
            if (attribute == null) return emptyList();
            resolvedAttributePath.add(attribute);
        }
        return resolvedAttributePath;
    }

    public String getName() {
        StringBuilder name = new StringBuilder(argument.getObjectName());
        for (DBObjectRef<DBTypeAttribute> attribute : attributePath) {
            name.append('.').append(attribute.getObjectName());
        }
        return name.toString();
    }

    public Object getValue() {
        return valueHolder.getValue();
    }

    public boolean isLargeObject() {
        DBArgument argument = getArgument();
        if (argument == null) return false;

        DBDataType dataType = argument.getDataType();
        return dataType.isNative() && dataType.getNativeType().isLargeObject();
    }

    public  boolean isLargeValue() {
        Object value = valueHolder.getValue();
        if (value == null) return false;

        if (value instanceof String stringValue) {
            return stringValue.length() > 200 || stringValue.contains("\n");
        }

        return false;
    }

    public boolean matches(DBArgument argument) {
        return Objects.equals(argument.ref(), this.argument);
    }

    public boolean matches(DBTypeAttribute attribute) {
        return matches(Collections.singletonList(attribute));
    }

    public boolean matches(List<DBTypeAttribute> attributePath) {
        if (attributePath.size() != this.attributePath.size()) return false;

        for (int i = 0; i < attributePath.size(); i++) {
            if (!Objects.equals(attributePath.get(i).ref(), this.attributePath.get(i))) return false;
        }
        return true;
    }

    public boolean isCursor() {
        return getValue() instanceof ResultSet;
    }

    public void setValue(Object value) {
        valueHolder.setValue(value);
    }

    public String toString() {
        return getName() + " = " + getValue();
    }
}
