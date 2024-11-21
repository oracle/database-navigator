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

import com.dbn.database.common.util.DataTypeParseAdapter;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class BasicDataTypeDefinition implements DataTypeDefinition {
    private final GenericDataType genericDataType;
    private final String name;
    private final Class typeClass;
    private final int sqlType;
    private final boolean pseudoNative;
    private final String contentTypeName;
    private DataTypeParseAdapter parseAdapter;


    public BasicDataTypeDefinition(String name, Class typeClass, int sqlType, GenericDataType genericDataType) {
        this(name, typeClass, sqlType, genericDataType, false);
    }

    public BasicDataTypeDefinition(String name, Class typeClass, int sqlType, GenericDataType genericDataType, boolean pseudoNative) {
        this(name, typeClass, sqlType, genericDataType, pseudoNative, null);
    }

    public BasicDataTypeDefinition(String name, Class typeClass, int sqlType, GenericDataType genericDataType, boolean pseudoNative, String contentTypeName) {
        this.name = name;
        this.typeClass = typeClass;
        this.sqlType = sqlType;
        this.genericDataType = genericDataType;
        this.pseudoNative = pseudoNative;
        this.contentTypeName = contentTypeName;
    }

    @Override
    public String toString() {
        return "[NAME = " + name + ", " +
                "GENERIC_TYPE = " + genericDataType + ", " +
                "TYPE_CLASS = " + typeClass.getName() + ", " +
                "SQL_TYPE = " + sqlType + ']';
    }

    @Override
    public Object convert(@Nullable Object object) {
        return object;
    }
}