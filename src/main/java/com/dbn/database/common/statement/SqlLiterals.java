/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.database.common.statement;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Renders java values as SQL literals for statement templates resolved with {@code {$N}} markers.
 * Used in deferred-execution contexts (e.g. DBMS_SCHEDULER job actions) where values cannot travel
 * as JDBC binds and must be part of the final statement text.
 * <p>
 * Rendering is strictly whitelist-based and fails closed: only well-known scalar types are supported,
 * strings are quote-escaped, numbers are validated, and temporal values are rendered as ANSI date/time
 * literals. Secret material ({@code char[]} by DBN convention) is rejected, as rendered values become
 * visible in scheduler job metadata.
 */
@UtilityClass
public class SqlLiterals {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String renderLiteral(@Nullable Object value) {
        if (value == null) return "NULL";

        if (value instanceof String string) return renderString(string);
        if (value instanceof Character character) return renderString(character.toString());
        if (value instanceof Boolean bool) return bool ? "TRUE" : "FALSE";
        if (value instanceof Number number) return renderNumber(number);
        if (value instanceof LocalDate date) return renderDate(date);
        if (value instanceof LocalDateTime dateTime) return renderDateTime(dateTime);
        if (value instanceof java.sql.Date date) return renderDate(date.toLocalDate());
        if (value instanceof java.sql.Timestamp timestamp) return renderDateTime(timestamp.toLocalDateTime());
        if (value instanceof Date date) return renderDateTime(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
        if (value instanceof char[]) throw new IllegalArgumentException("Refusing to render char[] (secret material) as a SQL literal - rendered values are exposed in scheduler job metadata");

        throw new IllegalArgumentException("Unsupported SQL literal type: " + value.getClass().getName());
    }

    private static String renderString(String value) {
        if (value.indexOf(0) >= 0) throw new IllegalArgumentException("SQL string literal must not contain NUL characters");
        return "'" + value.replace("'", "''") + "'";
    }

    private static String renderNumber(Number value) {
        if (value instanceof Double dbl && !Double.isFinite(dbl)) throw new IllegalArgumentException("SQL number literal must be finite: " + value);
        if (value instanceof Float flt && !Float.isFinite(flt)) throw new IllegalArgumentException("SQL number literal must be finite: " + value);

        if (value instanceof BigDecimal decimal) return decimal.toPlainString();
        if (value instanceof BigInteger ||
                value instanceof Long ||
                value instanceof Integer ||
                value instanceof Short ||
                value instanceof Byte) return value.toString();
        if (value instanceof Double || value instanceof Float) return BigDecimal.valueOf(value.doubleValue()).toPlainString();

        throw new IllegalArgumentException("Unsupported SQL number literal type: " + value.getClass().getName());
    }

    private static String renderDate(LocalDate value) {
        return "DATE '" + DATE_FORMAT.format(value) + "'";
    }

    private static String renderDateTime(LocalDateTime value) {
        String text = DATE_TIME_FORMAT.format(value);
        int nano = value.getNano();
        if (nano > 0) text = text + "." + String.format("%09d", nano).replaceAll("0+$", "");
        return "TIMESTAMP '" + text + "'";
    }
}
