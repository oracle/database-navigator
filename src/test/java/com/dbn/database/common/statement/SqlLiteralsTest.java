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

import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SqlLiteralsTest {

    @Test
    public void rendersNullAsSqlNull() {
        assertEquals("NULL", SqlLiterals.renderLiteral(null));
    }

    @Test
    public void escapesQuotesInStringLiterals() {
        assertEquals("'O''Brien'", SqlLiterals.renderLiteral("O'Brien"));
        assertEquals("'x''; DROP TABLE users; --'", SqlLiterals.renderLiteral("x'; DROP TABLE users; --"));
        assertEquals("'x'' OR 1=1 --'''", SqlLiterals.renderLiteral("x' OR 1=1 --'"));
        assertEquals("'plain'", SqlLiterals.renderLiteral("plain"));
    }

    @Test
    public void rendersNumbersAsPlainLiterals() {
        assertEquals("42", SqlLiterals.renderLiteral(42));
        assertEquals("-7", SqlLiterals.renderLiteral((byte) -7));
        assertEquals("9223372036854775807", SqlLiterals.renderLiteral(Long.MAX_VALUE));
        assertEquals("0.75", SqlLiterals.renderLiteral(0.75));
        assertEquals("12345.6789", SqlLiterals.renderLiteral(new BigDecimal("12345.6789")));
    }

    @Test
    public void rendersBooleansAsKeywords() {
        assertEquals("TRUE", SqlLiterals.renderLiteral(true));
        assertEquals("FALSE", SqlLiterals.renderLiteral(false));
    }

    @Test
    public void rendersTemporalValuesAsAnsiLiterals() {
        assertEquals("DATE '2026-07-09'", SqlLiterals.renderLiteral(LocalDate.of(2026, 7, 9)));
        assertEquals("TIMESTAMP '2026-07-09 13:45:30'", SqlLiterals.renderLiteral(LocalDateTime.of(2026, 7, 9, 13, 45, 30)));
        assertEquals("TIMESTAMP '2026-07-09 13:45:30.5'", SqlLiterals.renderLiteral(LocalDateTime.of(2026, 7, 9, 13, 45, 30, 500_000_000)));
        assertEquals("DATE '2026-07-09'", SqlLiterals.renderLiteral(java.sql.Date.valueOf("2026-07-09")));
        assertEquals("TIMESTAMP '2026-07-09 13:45:30'", SqlLiterals.renderLiteral(java.sql.Timestamp.valueOf("2026-07-09 13:45:30")));
    }

    @Test
    public void rejectsNonFiniteNumbers() {
        assertThrows(IllegalArgumentException.class, () -> SqlLiterals.renderLiteral(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> SqlLiterals.renderLiteral(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> SqlLiterals.renderLiteral(Float.NEGATIVE_INFINITY));
    }

    @Test
    public void rejectsNulCharactersInStrings() {
        assertThrows(IllegalArgumentException.class, () -> SqlLiterals.renderLiteral("bad\0value"));
    }

    @Test
    public void rejectsSecretMaterial() {
        assertThrows(IllegalArgumentException.class, () -> SqlLiterals.renderLiteral("secret".toCharArray()));
    }

    @Test
    public void rejectsUnsupportedTypes() {
        assertThrows(IllegalArgumentException.class, () -> SqlLiterals.renderLiteral(new Object()));
        assertThrows(IllegalArgumentException.class, () -> SqlLiterals.renderLiteral(new String[]{"a"}));
    }
}
