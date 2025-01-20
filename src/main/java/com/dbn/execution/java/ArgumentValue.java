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

package com.dbn.execution.java;

import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaParameter;
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
    private final DBObjectRef<DBJavaParameter> argumentRef;
    private final DBObjectRef<DBJavaField> fieldRef;
    private ArgumentValueHolder valueHolder;
    private boolean isComplexClass;

    public ArgumentValue(@NotNull DBJavaParameter argument, ArgumentValueHolder valueHolder) {
        this.argumentRef = DBObjectRef.of(argument);
        this.fieldRef = null;
        this.valueHolder = valueHolder;
        this.isComplexClass = false;
    }

    public ArgumentValue(@NotNull DBJavaField argument, ArgumentValueHolder valueHolder) {
        this.argumentRef = null;
        this.fieldRef = DBObjectRef.of(argument);
        this.valueHolder = valueHolder;
        this.isComplexClass = argument.getType().equals("class") && argument.getFieldClass() != null;
    }

    @Nullable
    public DBJavaParameter getArgument() {
        if(argumentRef == null)
            return null;
        return argumentRef.get();
    }

    @Nullable
    public DBJavaField getField() {
        if(fieldRef == null)
            return null;
        return fieldRef.get();
    }

    public String getName() {
        if(argumentRef == null){
            return fieldRef.getObjectName();
        }
        return argumentRef.getObjectName();
    }

    public Object getValue() {
        return valueHolder.getValue();
    }

    public boolean isLargeObject() {
        return false;
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

    public boolean matches(DBJavaParameter argument) {
        return Objects.equals(argument.ref(), this.argumentRef)
                || Objects.equals(argument.ref(), this.fieldRef);
    }

    public boolean matches(DBJavaField argument) {
        return Objects.equals(argument.ref(), this.argumentRef)
                || Objects.equals(argument.ref(), this.fieldRef);
    }

    public boolean isCursor() {
        return getValue() instanceof ResultSet;
    }

    public void setValue(Object value) {
        valueHolder.setValue(value);
    }

    public String toString() {
        if(argumentRef == null){
            return fieldRef.getObjectName() + " = " + getValue();
        }
        return argumentRef.getObjectName() + " = " + getValue();
    }

    public static <T> ArgumentValueHolder<T> createBasicValueHolder(T value) {
        ArgumentValueHolder<T> valueStore = new ArgumentValueHolder<>() {
            private T value;

            @Override
            public T getValue() {
                return value;
            }

            @Override
            public void setValue(T value) {
                this.value = value;
            }
        };

        valueStore.setValue(value);
        return valueStore;
    }
}
