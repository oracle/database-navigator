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

package com.dbn.database.mysql;

import com.dbn.data.type.GenericDataType;
import com.dbn.database.common.DatabaseNativeDataTypes;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Timestamp;
import java.sql.Types;

class MySqlNativeDataTypes extends DatabaseNativeDataTypes {
    {
        createBasicDefinition("CHAR", String.class, Types.CHAR, GenericDataType.LITERAL);
        createBasicDefinition("VARCHAR", String.class, Types.VARCHAR, GenericDataType.LITERAL);
        createBasicDefinition("BINARY", String.class, Types.BINARY, GenericDataType.LITERAL);
        createBasicDefinition("VARBINARY", String.class, Types.VARBINARY, GenericDataType.LITERAL);
        createBasicDefinition("NATIONAL CHAR", String.class, Types.CHAR, GenericDataType.LITERAL);
        createBasicDefinition("NATIONAL VARCHAR", String.class, Types.VARCHAR, GenericDataType.LITERAL);
        createBasicDefinition("ENUM", String.class, Types.CHAR, GenericDataType.LITERAL);
        createBasicDefinition("SET", String.class, Types.CHAR, GenericDataType.LITERAL);

        createBasicDefinition("TINYTEXT", String.class, Types.VARCHAR, GenericDataType.LITERAL);
        createBasicDefinition("TEXT", String.class, Types.VARCHAR, GenericDataType.LITERAL);
        createBasicDefinition("MEDIUMTEXT", String.class, Types.VARCHAR, GenericDataType.LITERAL);
        createBasicDefinition("LONGTEXT", String.class, Types.VARCHAR, GenericDataType.LITERAL);


        createNumericDefinition("BIT", Short.class, Types.BIT);
        createNumericDefinition("TINYINT", Short.class, Types.TINYINT);
        createNumericDefinition("BOOL", Boolean.class, Types.BOOLEAN);
        createNumericDefinition("BOOLEAN", Boolean.class, Types.BOOLEAN);
        createNumericDefinition("SMALLINT", Integer.class, Types.SMALLINT);
        createNumericDefinition("MEDIUMINT", Integer.class, Types.INTEGER);
        createNumericDefinition("INT", Long.class, Types.INTEGER);
        createNumericDefinition("INT UNSIGNED", Long.class, Types.INTEGER);
        createNumericDefinition("INTEGER", Long.class, Types.INTEGER);
        createNumericDefinition("BIGINT", BigInteger.class, Types.BIGINT);
        createNumericDefinition("FLOAT", Float.class, Types.FLOAT);
        createNumericDefinition("DOUBLE", Double.class, Types.DOUBLE);
        createNumericDefinition("DOUBLE PRECISION", Double.class, Types.DOUBLE);
        createNumericDefinition("DECIMAL", BigDecimal.class, Types.DECIMAL);
        createNumericDefinition("DEC", BigDecimal.class, Types.DECIMAL);

        createDateTimeDefinition("DATE", Timestamp.class, Types.DATE);
        createDateTimeDefinition("DATETIME", Timestamp.class, Types.TIMESTAMP);
        createDateTimeDefinition("TIMESTAMP", Timestamp.class, Types.TIMESTAMP);
        createDateTimeDefinition("TIME", Timestamp.class, Types.TIME);
        createDateTimeDefinition("YEAR", Timestamp.class, Types.DATE);

        createLargeValueDefinition("TINYBLOB", Blob.class, Types.BLOB, GenericDataType.BLOB);
        createLargeValueDefinition("BLOB", Blob.class, Types.BLOB, GenericDataType.BLOB);
        createLargeValueDefinition("MEDIUMBLOB", Blob.class, Types.BLOB, GenericDataType.BLOB);
        createLargeValueDefinition("LONGBLOB", Blob.class, Types.BLOB, GenericDataType.BLOB);
    }
}