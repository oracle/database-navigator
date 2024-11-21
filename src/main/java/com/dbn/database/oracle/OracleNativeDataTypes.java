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

package com.dbn.database.oracle;

import com.dbn.data.type.GenericDataType;
import com.dbn.database.common.DatabaseNativeDataTypes;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;

public class OracleNativeDataTypes extends DatabaseNativeDataTypes {
    {
        createLiteralDefinition("CHAR", String.class, OracleTypes.CHAR);
        createLiteralDefinition("CHAR VARYING", String.class, OracleTypes.CHAR);
        createLiteralDefinition("CHARACTER", String.class, OracleTypes.CHAR);
        createLiteralDefinition("CHARACTER VARYING", String.class, OracleTypes.CHAR);
        createLiteralDefinition("LONG", String.class, OracleTypes.LONGVARCHAR);
        createLiteralDefinition("LONG RAW", String.class, OracleTypes.LONGVARCHAR);
        createLiteralDefinition("NATIONAL CHAR", String.class, OracleTypes.CHAR);
        createLiteralDefinition("NATIONAL CHAR VARYING", String.class, OracleTypes.CHAR);
        createLiteralDefinition("NATIONAL CHARACTER", String.class, OracleTypes.CHAR);
        createLiteralDefinition("NATIONAL CHARACTER VARYING", String.class, OracleTypes.CHAR);
        createLiteralDefinition("NCHAR VARYING", String.class, OracleTypes.CHAR);
        createLiteralDefinition("NCHAR", String.class, OracleTypes.CHAR);
        createLiteralDefinition("NVARCHAR2", String.class, OracleTypes.CHAR);
        createLiteralDefinition("RAW", String.class, OracleTypes.RAW);
        createLiteralDefinition("STRING", String.class, OracleTypes.VARCHAR);
        createLiteralDefinition("VARCHAR", String.class, OracleTypes.VARCHAR);
        createLiteralDefinition("VARCHAR2", String.class, OracleTypes.VARCHAR);


        createNumericDefinition("BINARY_INTEGER", Integer.class, OracleTypes.INTEGER);
        createNumericDefinition("BINARY_FLOAT", Float.class, OracleTypes.BINARY_FLOAT);
        createNumericDefinition("BINARY_DOUBLE", Double.class, OracleTypes.BINARY_DOUBLE);
        createNumericDefinition("DECIMAL", BigDecimal.class, OracleTypes.DECIMAL);
        createNumericDefinition("DEC", BigDecimal.class, OracleTypes.DECIMAL);
        createNumericDefinition("DOUBLE PRECISION", Double.class, OracleTypes.DOUBLE);
        createNumericDefinition("FLOAT", Double.class, OracleTypes.FLOAT);
        createNumericDefinition("INTEGER", Integer.class, OracleTypes.INTEGER);
        createNumericDefinition("INT", Integer.class, OracleTypes.INTEGER);
        createNumericDefinition("NATURAL", Integer.class, OracleTypes.INTEGER);
        createNumericDefinition("NATURALN", Integer.class, OracleTypes.INTEGER);
        createNumericDefinition("NUMBER", BigDecimal.class, OracleTypes.NUMBER);
        createNumericDefinition("NUMERIC", BigDecimal.class, OracleTypes.NUMERIC);
        createNumericDefinition("PLS_INTEGER", Integer.class, OracleTypes.INTEGER);
        createNumericDefinition("POSITIVE", Integer.class, OracleTypes.INTEGER);
        createNumericDefinition("POSITIVEN", Integer.class, OracleTypes.INTEGER);
        createNumericDefinition("REAL", Float.class, OracleTypes.FLOAT);
        createNumericDefinition("SMALLINT", Integer.class, OracleTypes.SMALLINT);
        createNumericDefinition("SIGNTYPE", Integer.class, OracleTypes.SMALLINT);

        createDateTimeDefinition("DATE", Timestamp.class, OracleTypes.DATE);
        createDateTimeDefinition("TIME", Timestamp.class, OracleTypes.TIME);
        createDateTimeDefinition("TIME WITH TIME ZONE", Timestamp.class, OracleTypes.TIMESTAMPTZ);
        createDateTimeDefinition("TIMESTAMP", Timestamp.class, OracleTypes.TIMESTAMP);
        createDateTimeDefinition("TIMESTAMP WITH LOCAL TIME ZONE", Timestamp.class, OracleTypes.TIMESTAMPLTZ);
        createDateTimeDefinition("TIMESTAMP WITH LOCAL TZ", Timestamp.class, OracleTypes.TIMESTAMPLTZ);
        createDateTimeDefinition("TIMESTAMP WITH TIME ZONE", Timestamp.class, OracleTypes.TIMESTAMPLTZ);
        createDateTimeDefinition("TIMESTAMP WITH TZ", Timestamp.class, OracleTypes.TIMESTAMPLTZ);
        createBasicDefinition("INTERVAL DAY TO SECOND", Object.class, OracleTypes.INTERVALDS, GenericDataType.PROPRIETARY);
        createBasicDefinition("INTERVAL YEAR TO MONTH", Object.class, OracleTypes.INTERVALYM, GenericDataType.PROPRIETARY);
        createBasicDefinition("INTERVALDS", Object.class, OracleTypes.INTERVALDS, GenericDataType.PROPRIETARY);
        createBasicDefinition("INTERVALYM", Object.class, OracleTypes.INTERVALYM, GenericDataType.PROPRIETARY);

        createLargeValueDefinition("BLOB", Blob.class, OracleTypes.BLOB, GenericDataType.BLOB);
        createLargeValueDefinition("CLOB", Clob.class, OracleTypes.CLOB, GenericDataType.CLOB);
        createLargeValueDefinition("NCLOB", NClob.class, OracleTypes.NCLOB, GenericDataType.NCLOB);
        createLargeValueDefinition("XMLTYPE", Clob.class, Types.SQLXML, GenericDataType.XMLTYPE, true, "XML");

        createBasicDefinition("BFILE", Object.class, OracleTypes.BFILE, GenericDataType.FILE);
        createBasicDefinition("ROWID", Object.class, OracleTypes.ROWID, GenericDataType.ROWID);
        createBasicDefinition("UROWID", Object.class, OracleTypes.ROWID, GenericDataType.ROWID);
        createBasicDefinition("REF", Object.class, OracleTypes.REF, GenericDataType.REF);
        createBasicDefinition("REF CURSOR", Object.class, OracleTypes.CURSOR, GenericDataType.CURSOR, true, "CURSOR");

        createBasicDefinition("ANYDATA", Object.class, OracleTypes.OTHER, GenericDataType.OBJECT);
        createBasicDefinition("ANYTYPE", Object.class, OracleTypes.OTHER, GenericDataType.OBJECT);
        createBasicDefinition("ANYDATASET", Object.class, OracleTypes.OTHER, GenericDataType.OBJECT);
        createBasicDefinition("OBJECT", Object.class, OracleTypes.OTHER, GenericDataType.OBJECT);
        createBasicDefinition("TABLE", Object.class, OracleTypes.OTHER, GenericDataType.TABLE);
        createBasicDefinition("COLLECTION", Collection.class, OracleTypes.ARRAY, GenericDataType.COLLECTION);

        createBasicDefinition("BOOLEAN", Boolean.class, OracleTypes.VARCHAR, GenericDataType.BOOLEAN);
        createBasicDefinition("PL/SQL BOOLEAN", String.class, OracleTypes.VARCHAR, GenericDataType.BOOLEAN);
        createBasicDefinition("PL/SQL TABLE", String.class, OracleTypes.OTHER, GenericDataType.TABLE);
        createBasicDefinition("PL/SQL RECORD", String.class, OracleTypes.OTHER, GenericDataType.OBJECT);
    }

}
