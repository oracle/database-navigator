/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.ml.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * One input attribute from an Oracle machine learning model signature.
 *
 * @author ayoub allali
 */
@Getter
@AllArgsConstructor
public class MLPredictionAttribute {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");
    private static final DateTimeFormatter TIMESTAMP_TIME_ZONE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS XXX");

    private final @NonNls String name;
    private final @Nullable @NonNls String attributeType;
    private final @Nullable @NonNls String dataType;

    @NotNull
    public String getDisplayDataType() {
        if (dataType != null) return dataType;
        return attributeType == null ? "UNKNOWN" : attributeType;
    }

    public boolean isSupportedForPrediction() {
        String type = normalizedDataType();
        if (attributeTypeEquals("MIXED") || attributeTypeEquals("UNSTRUCTURED") || attributeTypeEquals("VECTOR")) {
            return false;
        }

        return isNumericType(type) || isCharacterType(type) || type.equals("DATE") ||
                type.startsWith("TIMESTAMP") || type.equals("BOOLEAN") || type.equals("CLOB");
    }

    public int getJdbcType() {
        String type = normalizedDataType();
        if (isNumericType(type)) return Types.NUMERIC;
        if (isCharacterType(type)) return Types.VARCHAR;
        if (type.equals("DATE")) return Types.DATE;
        if (type.startsWith("TIMESTAMP")) return Types.TIMESTAMP;
        if (type.equals("BOOLEAN")) return Types.BOOLEAN;
        if (type.equals("CLOB")) return Types.CLOB;
        return Types.OTHER;
    }

    public Object parseValue(String value) {
        String type = normalizedDataType();
        if (isNumericType(type)) return new BigDecimal(value);
        if (type.equals("DATE")) return Date.valueOf(value);
        if (type.startsWith("TIMESTAMP WITH")) return OffsetDateTime.parse(value);
        if (type.startsWith("TIMESTAMP")) return Timestamp.valueOf(value.replace('T', ' '));
        if (type.equals("BOOLEAN")) return parseBoolean(value);
        return value;
    }

    @NonNls
    public String toSqlLiteral(String value) {
        String type = normalizedDataType();
        if (value.isBlank()) return "NULL";

        Object parsedValue = parseValue(value);
        if (isNumericType(type)) return ((BigDecimal) parsedValue).toPlainString();
        if (isCharacterType(type) || type.equals("CLOB")) return quoteLiteral(value);
        if (type.equals("DATE")) return "DATE " + quoteLiteral(parsedValue.toString());
        if (type.startsWith("TIMESTAMP WITH")) {
            OffsetDateTime timestamp = (OffsetDateTime) parsedValue;
            return "TO_TIMESTAMP_TZ(" + quoteLiteral(TIMESTAMP_TIME_ZONE_FORMATTER.format(timestamp)) +
                    ", 'YYYY-MM-DD HH24:MI:SS.FF9 TZH:TZM')";
        }
        if (type.startsWith("TIMESTAMP")) {
            Timestamp timestamp = (Timestamp) parsedValue;
            LocalDateTime dateTime = timestamp.toLocalDateTime();
            return "TIMESTAMP " + quoteLiteral(TIMESTAMP_FORMATTER.format(dateTime));
        }
        if (type.equals("BOOLEAN")) return Boolean.TRUE.equals(parsedValue) ? "TRUE" : "FALSE";
        return quoteLiteral(value);
    }

    @NonNls
    private static String quoteLiteral(String value) {
        return '\'' + value.replace("'", "''") + '\'';
    }

    private boolean attributeTypeEquals(@NonNls String type) {
        return type.equalsIgnoreCase(attributeType);
    }

    @NonNls
    private String normalizedDataType() {
        return dataType == null ? "" : dataType.toUpperCase(Locale.ROOT);
    }

    private static boolean isNumericType(@NonNls String type) {
        return switch (type) {
            case "NUMBER", "NUMERIC", "DECIMAL", "INTEGER", "FLOAT", "BINARY_FLOAT", "BINARY_DOUBLE" -> true;
            default -> false;
        };
    }

    private static boolean isCharacterType(@NonNls String type) {
        return switch (type) {
            case "CHAR", "VARCHAR", "VARCHAR2", "NCHAR", "NVARCHAR2" -> true;
            default -> false;
        };
    }

    private static boolean parseBoolean(String value) {
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "TRUE", "1" -> true;
            case "FALSE", "0" -> false;
            default -> throw new IllegalArgumentException(value);
        };
    }
}
