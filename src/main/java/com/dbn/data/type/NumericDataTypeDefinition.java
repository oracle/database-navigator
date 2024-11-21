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

package com.dbn.data.type;

import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.Objects;

public class NumericDataTypeDefinition extends BasicDataTypeDefinition {
    private final Constructor constructor;

    @SneakyThrows
    public NumericDataTypeDefinition(String name, Class typeClass, int sqlType) {
        super(name, typeClass, sqlType, GenericDataType.NUMERIC);
        constructor = typeClass.getConstructor(String.class);
    }

    @Override
    @SneakyThrows
    public Object convert(@Nullable Object object) {
        if (object == null) return null;

        Number number = (Number) object;
        if (Objects.equals(object.getClass(), getTypeClass())) return object;

        return constructor.newInstance(number.toString());
    }
}
