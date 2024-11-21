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

package com.dbn.database.postgres;

import com.dbn.data.type.GenericDataType;
import com.dbn.database.common.DatabaseNativeDataTypes;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

public class PostgresNativeDataTypes extends DatabaseNativeDataTypes {
    {
        createNumericDefinition("OID", BigDecimal.class, Types.NUMERIC);
        createNumericDefinition("XID", BigDecimal.class, Types.NUMERIC);
        createNumericDefinition("CID", BigDecimal.class, Types.NUMERIC);
        createNumericDefinition("TID", BigDecimal.class, Types.NUMERIC);

        createNumericDefinition("SMALLINT", Integer.class, Types.SMALLINT);
        createNumericDefinition("TINYINT", Integer.class, Types.TINYINT);
        createNumericDefinition("INTEGER", Long.class, Types.BIGINT);
        createNumericDefinition("INT", Integer.class, Types.INTEGER);
        createNumericDefinition("INT2", Integer.class, Types.INTEGER);
        createNumericDefinition("INT4", Integer.class, Types.INTEGER);
        createNumericDefinition("INT8", Long.class, Types.NUMERIC);
        createNumericDefinition("BIGINT", Long.class, Types.BIGINT);
        createNumericDefinition("SMALLSERIAL", Integer.class, Types.SMALLINT);
        createNumericDefinition("SERIAL", Long.class, Types.BIGINT);
        createNumericDefinition("BIGSERIAL", Long.class, Types.BIGINT);
        createNumericDefinition("INT8", Long.class, Types.BIGINT);
        createNumericDefinition("SERIAL2", Long.class, Types.BIGINT);
        createNumericDefinition("SERIAL3", Long.class, Types.BIGINT);
        createNumericDefinition("SERIAL8", Long.class, Types.BIGINT);
        createNumericDefinition("DECIMAL", BigDecimal.class, Types.DECIMAL);
        createNumericDefinition("NUMERIC", BigDecimal.class, Types.NUMERIC);
        createNumericDefinition("REAL", Float.class, Types.NUMERIC);
        createNumericDefinition("DOUBLE", Float.class, Types.DOUBLE);
        createNumericDefinition("DOUBLE_PRECISION", BigDecimal.class, Types.DOUBLE);
        createNumericDefinition("FLOAT4", Float.class, Types.FLOAT);
        createNumericDefinition("FLOAT8", Float.class, Types.FLOAT);
        createNumericDefinition("MONEY", BigDecimal.class, Types.DOUBLE);

        createLiteralDefinition("CHARACTER VARYING", String.class, Types.VARCHAR);
        createLiteralDefinition("LONGVARCHAR", String.class, Types.LONGNVARCHAR);
        createLiteralDefinition("VARCHAR", String.class, Types.VARCHAR);
        createLiteralDefinition("CHARACTER", String.class, Types.CHAR);
        createLiteralDefinition("CHAR", String.class, Types.CHAR);
        createLiteralDefinition("BPCHAR", String.class, Types.CHAR);
        createLiteralDefinition("TEXT", String.class, Types.VARCHAR);
        createLiteralDefinition("NAME", String.class, Types.VARCHAR);

        createLiteralDefinition("BINARY", String.class, Types.BINARY);
        createLiteralDefinition("VARBINARY", String.class, Types.BINARY);
        createLiteralDefinition("LONGVARBINARY", String.class, Types.BINARY);
        createLiteralDefinition("BYTEA", String.class, Types.BINARY);
        createLiteralDefinition("BIT", Boolean.class, Types.BOOLEAN);
        createLiteralDefinition("BIT VARYING", String.class, Types.VARCHAR);
        createLiteralDefinition("VARBIT", String.class, Types.VARCHAR);


        createDateTimeDefinition("TIMESTAMP", Timestamp.class, Types.TIMESTAMP);
        createDateTimeDefinition("TIMESTAMPTZ", Timestamp.class, Types.TIMESTAMP_WITH_TIMEZONE);
        createDateTimeDefinition("TIMESTAMP WITH TIME ZONE", Timestamp.class, Types.TIMESTAMP_WITH_TIMEZONE);
        createDateTimeDefinition("TIMESTAMP WITHOUT TIME ZONE", Timestamp.class, Types.TIMESTAMP);
        createDateTimeDefinition("DATE", Date.class, Types.DATE);
        createDateTimeDefinition("TIME", Time.class, Types.TIME);
        createDateTimeDefinition("TIMETZ", Timestamp.class, Types.TIME_WITH_TIMEZONE);
        createDateTimeDefinition("TIME WITH TIME ZONE", Time.class, Types.TIME);
        createDateTimeDefinition("TIME WITHOUT TIME ZONE", Time.class, Types.TIME);
        createLiteralDefinition("INTERVAL", String.class, Types.VARCHAR);

        createBasicDefinition("BOOLEAN", Boolean.class, Types.BOOLEAN, GenericDataType.BOOLEAN);
        createBasicDefinition("BOOL", Boolean.class, Types.BOOLEAN, GenericDataType.BOOLEAN);
        createBasicDefinition("POINT", Object.class, Types.OTHER, GenericDataType.OBJECT);
        createBasicDefinition("LINE", Object.class, Types.OTHER, GenericDataType.OBJECT);
        createBasicDefinition("LSEG", Object.class, Types.OTHER, GenericDataType.OBJECT);
        createBasicDefinition("BOX", Object.class, Types.OTHER, GenericDataType.OBJECT);
        createBasicDefinition("PATH", Object.class, Types.OTHER, GenericDataType.OBJECT);
        createBasicDefinition("POLYGON", Object.class, Types.OTHER, GenericDataType.OBJECT);
        createBasicDefinition("CIRCLE", Object.class, Types.OTHER, GenericDataType.OBJECT);
        createBasicDefinition("JSON", Object.class, Types.OTHER, GenericDataType.OBJECT);
        createBasicDefinition("JSONB", Object.class, Types.OTHER, GenericDataType.OBJECT);
        createBasicDefinition("ARRAY", Array.class, Types.ARRAY, GenericDataType.ARRAY);

        createBasicDefinition("INT2VECTOR", Array.class, Types.ARRAY, GenericDataType.ARRAY);

        createLiteralDefinition("CIDR", String.class, Types.VARCHAR);
        createLiteralDefinition("INET", String.class, Types.VARCHAR);
        createLiteralDefinition("MACADDR", String.class, Types.VARCHAR);

        createLiteralDefinition("TSVECTOR", String.class, Types.VARCHAR);
        createLiteralDefinition("TSQUERY", String.class, Types.VARCHAR);
        createLiteralDefinition("UUID", String.class, Types.VARCHAR);
        createLiteralDefinition("XML", String.class, Types.VARCHAR);
        createLiteralDefinition("UNKNOWN", String.class, Types.VARCHAR);

        createBasicDefinition("REFCURSOR", Object.class, Types.OTHER, GenericDataType.CURSOR);
    }
}