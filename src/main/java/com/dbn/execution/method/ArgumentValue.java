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
import java.util.Objects;

@Getter
@Setter
public class ArgumentValue {
    private final DBObjectRef<DBArgument> argumentRef;
    private DBObjectRef<DBTypeAttribute> attributeRef;
    private ValueHolder valueHolder;

    public ArgumentValue(@NotNull DBArgument argument, @Nullable DBTypeAttribute attribute, ValueHolder valueHolder) {
        this.argumentRef = DBObjectRef.of(argument);
        this.attributeRef = DBObjectRef.of(attribute);
        this.valueHolder = valueHolder;
    }

    public ArgumentValue(@NotNull DBArgument argument, ValueHolder valueHolder) {
        this.argumentRef = DBObjectRef.of(argument);
        this.valueHolder = valueHolder;
    }

    @Nullable
    public DBArgument getArgument() {
        return argumentRef.get();
    }

    public DBTypeAttribute getAttribute() {
        return DBObjectRef.get(attributeRef);
    }

    public String getName() {
        return
            attributeRef == null ?
            argumentRef.getObjectName() :
            argumentRef.getObjectName() + '.' + attributeRef.getObjectName();
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

        if (value instanceof String) {
            String stringValue = (String) value;
            return stringValue.length() > 200 || stringValue.contains("\n");
        }

        return false;
    }

    public boolean matches(DBArgument argument) {
        return Objects.equals(argument.ref(), this.argumentRef);
    }

    public boolean matches(DBTypeAttribute attribute) {
        return Objects.equals(attribute.ref(), this.attributeRef);
    }

    public boolean isCursor() {
        return getValue() instanceof ResultSet;
    }

    public void setValue(Object value) {
        valueHolder.setValue(value);
    }

    public String toString() {
        return argumentRef.getObjectName() + " = " + getValue();
    }
}
