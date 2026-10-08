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

@SuppressWarnings("unused")
public class OracleTypes {

    public final static int BIT = -7;
    public final static int TINYINT = -6;
    public final static int SMALLINT = 5;
    public final static int INTEGER = 4;
    public final static int BIGINT = -5;

    public final static int FLOAT = 6;
    public final static int REAL = 7;
    public final static int DOUBLE = 8;

    public final static int NUMERIC = 2;
    public final static int DECIMAL = 3;

    public final static int CHAR = 1;
    public final static int VARCHAR = 12;
    public final static int LONGVARCHAR = -1;

    public final static int DATE = 91;
    public final static int TIME = 92;
    public final static int TIMESTAMP = 93;

    public final static int PLSQL_BOOLEAN = 252;

    public final static int TIMESTAMPNS = -100;
    public final static int TIMESTAMPTZ = -101;
    public final static int TIMESTAMPLTZ = -102;

    public final static int INTERVALYM = -103;
    public final static int INTERVALDS = -104;

    public final static int VECTOR = -105;

    public final static int VECTOR_INT8 = -106;
    public final static int VECTOR_FLOAT32 = -107;
    public final static int VECTOR_FLOAT64 = -108;
    public final static int VECTOR_BINARY = -109;

    public final static int BINARY = -2;
    public final static int VARBINARY = -3;
    public final static int LONGVARBINARY = -4;

    public final static int ROWID = -8;
    public final static int CURSOR = -10;
    public final static int BLOB = 2004;
    public final static int CLOB = 2005;
    public final static int BFILE = -13;

    public final static int STRUCT = 2002;
    public final static int ARRAY = 2003;
    public final static int REF = 2006;

    public final static int NCHAR = -15;
    public final static int NCLOB = 2011;
    public final static int NVARCHAR = -9;
    public final static int LONGNVARCHAR = -16;
    public final static int SQLXML = 2009;
    public final static int REF_CURSOR = 2012;
    public final static int JSON = 2016;
    public final static int OPAQUE = 2007;
    public final static int JAVA_STRUCT = 2008;
    public final static int JAVA_OBJECT = 2000;
    public final static int PLSQL_INDEX_TABLE = -14;
    public final static int BINARY_FLOAT = 100;
    public final static int BINARY_DOUBLE = 101;
    public final static int NULL = 0;
    public final static int NUMBER = NUMERIC;
    public final static int RAW = BINARY;
    public final static int OTHER = 1111;
    public final static int FIXED_CHAR = 999;
    public final static int DATALINK = 70;
    public final static int BOOLEAN = 16;
}
