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

package com.dbn.execution.java.wrapper;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;


public final class TypeMappings {
    @Getter
    private static final Map<String, SqlType> DATA_TYPES =
            Map.ofEntries(
                Map.entry("java.lang.String", new SqlType("VARCHAR2", "String.valueOf(", ")", " (32000)")),
                // Java Primitive types
                Map.entry("boolean", new SqlType("NUMBER", "", ".equals(\"1\")")),
                Map.entry("byte", new SqlType("NUMBER", "Byte.parseByte(String.valueOf(", "))")),
                Map.entry("char", new SqlType("VARCHAR2", "String.valueOf(", ").charAt(0)")),
                Map.entry("short", new SqlType("NUMBER", "Short.parseShort(String.valueOf(", "))")),
                Map.entry("int", new SqlType("NUMBER", "Integer.parseInt(String.valueOf(", "))")),
                Map.entry("long", new SqlType("NUMBER", "Long.parseLong(String.valueOf(", "))")),
                Map.entry("float", new SqlType("NUMBER", "Float.parseFloat(String.valueOf(", "))")),
                Map.entry("double", new SqlType("BINARY_DOUBLE", "Double.parseDouble(String.valueOf(", "))")),
                Map.entry("byte[]", new SqlType("RAW")),

                // SQL Types
                Map.entry("java.sql.Date", new SqlType("DATE", "transformStringToJavaSqlDate(", ")")),
                Map.entry("java.sql.Time", new SqlType("DATE")),
                Map.entry("java.math.BigDecimal", new SqlType("NUMBER", "new java.math.BigDecimal(String.valueOf(", "))")),
                Map.entry("java.math.BigInteger", new SqlType("NUMBER", "new java.math.BigInteger(String.valueOf(", "))")),

                // Java Wrapper Types
                Map.entry("java.lang.Boolean", new SqlType("NUMBER", "", ".equals(\"1\")")),
                Map.entry("java.lang.Byte", new SqlType("NUMBER", "Byte.parseByte(String.valueOf(", "))")),
                Map.entry("java.lang.Character", new SqlType("CHAR", "String.valueOf(", ").charAt(0)")),
                Map.entry("java.lang.Short", new SqlType("NUMBER", "Short.parseShort(String.valueOf(", "))")),
                Map.entry("java.lang.Integer", new SqlType("NUMBER", "Integer.parseInt(String.valueOf(", "))")),
                Map.entry("java.lang.Long", new SqlType("NUMBER", "Long.parseLong(String.valueOf(", "))")),
                Map.entry("java.lang.Float", new SqlType("NUMBER", "Float.parseFloat(String.valueOf(", "))")),
                Map.entry("java.lang.Double", new SqlType("BINARY_DOUBLE", "Double.parseDouble(String.valueOf(", "))")),

                // Oracle SQL Types
                Map.entry("oracle.sql.CHAR", new SqlType("VARCHAR2")),
                Map.entry("oracle.sql.NUMBER", new SqlType("NUMBER")),
                Map.entry("oracle.sql.BINARY_FLOAT", new SqlType("BINARY_FLOAT")),
                Map.entry("oracle.sql.BINARY_DOUBLE", new SqlType("BINARY_DOUBLE")),
                Map.entry("oracle.sql.DATE", new SqlType("DATE")),
                Map.entry("oracle.sql.RAW", new SqlType("RAW"))
            );

    @Getter
    private static final Set<String> UNSUPPORTED_TYPES = Set.of(
            "java.util.List",
            "java.util.ArrayList",
            "java.util.Map",
            "java.util.HashMap",
            "java.util.Set",
            "java.util.HashSet",
            "java.util.Collection"
            //...
    );

    public static boolean isSupportedType(String className) {
        return DATA_TYPES.containsKey(className);
    }

    public static boolean isUnsupportedType(String type){
        return UNSUPPORTED_TYPES.contains(type);
    }

    @Nullable
    public static SqlType getSqlType(String className){
        return DATA_TYPES.get(className);
    }

    @Nullable
    public static String getSqlTypeName(String className){
        SqlType sqlType = getSqlType(className);
        return sqlType == null ? null : sqlType.getSqlTypeName();
    }
}