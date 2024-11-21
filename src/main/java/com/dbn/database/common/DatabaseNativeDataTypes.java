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

package com.dbn.database.common;

import com.dbn.data.type.BasicDataTypeDefinition;
import com.dbn.data.type.DataTypeDefinition;
import com.dbn.data.type.DateTimeDataTypeDefinition;
import com.dbn.data.type.GenericDataType;
import com.dbn.data.type.LargeObjectDataTypeDefinition;
import com.dbn.data.type.LiteralDataTypeDefinition;
import com.dbn.data.type.NumericDataTypeDefinition;
import com.dbn.database.common.util.DataTypeParseAdapter;

import java.util.ArrayList;
import java.util.List;

public abstract class DatabaseNativeDataTypes {
    protected List<DataTypeDefinition> dataTypes = new ArrayList<>();
    public List<DataTypeDefinition> list() {return dataTypes;}

    protected void createBasicDefinition(String name, Class typeClass, int sqlType, GenericDataType genericDataType) {
        createBasicDefinition(name, typeClass, sqlType, genericDataType, false, null);
    }

    protected void createBasicDefinition(String name, Class typeClass, int sqlType, GenericDataType genericDataType, boolean pseudoNative, String contentTypeName) {
        BasicDataTypeDefinition dataTypeDefinition = new BasicDataTypeDefinition(name, typeClass, sqlType, genericDataType, pseudoNative, contentTypeName);
        dataTypes.add(dataTypeDefinition);
    }

    protected void createLargeValueDefinition(String name, Class typeClass, int sqlType, GenericDataType genericDataType, boolean pseudoNative, String contentTypeName) {
        LargeObjectDataTypeDefinition dataTypeDefinition = new LargeObjectDataTypeDefinition(name, typeClass, sqlType, genericDataType, pseudoNative, contentTypeName);
        dataTypes.add(dataTypeDefinition);
    }

    protected void createLargeValueDefinition(String name, Class typeClass, int sqlType, GenericDataType genericDataType) {
        LargeObjectDataTypeDefinition dataTypeDefinition = new LargeObjectDataTypeDefinition(name, typeClass, sqlType, genericDataType);
        dataTypes.add(dataTypeDefinition);
    }

    protected void createLiteralDefinition(String name, Class typeClass, int sqlType) {
        BasicDataTypeDefinition dataTypeDefinition = new LiteralDataTypeDefinition(name, typeClass, sqlType);
        dataTypes.add(dataTypeDefinition);
    }

    protected void createNumericDefinition(String name, Class typeClass, int sqlType) {
        BasicDataTypeDefinition dataTypeDefinition = new NumericDataTypeDefinition(name, typeClass, sqlType);
        dataTypes.add(dataTypeDefinition);
    }

    protected <T> void createDateTimeDefinition(String name, Class<T> typeClass, int sqlType) {
        createDateTimeDefinition(name, typeClass, sqlType, null);
    }

    protected <T> void createDateTimeDefinition(String name, Class<T> typeClass, int sqlType, DataTypeParseAdapter<T> parseAdapter) {
        BasicDataTypeDefinition dataTypeDefinition = new DateTimeDataTypeDefinition(name, typeClass, sqlType);
        dataTypeDefinition.setParseAdapter(parseAdapter);
        dataTypes.add(dataTypeDefinition);

    }


}
