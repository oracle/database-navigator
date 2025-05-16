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

package com.dbn.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class NamingTest {

    @Test
    public void testLowerCaseWords_withCamelCase() {
        String input = "camelCaseTest";
        String expected = "camel case test";
        assertEquals(expected, Naming.lowerCaseWords(input));
    }

    @Test
    public void testLowerCaseWords_withMixedCase() {
        String input = "MixedCASEExample";
        String expected = "mixed case example";
        assertEquals(expected, Naming.lowerCaseWords(input));
    }

    @Test
    public void testLowerCaseWords_withConsecutiveUppercase() {
        String input = "HTTPResponseCode";
        String expected = "http response code";
        assertEquals(expected, Naming.lowerCaseWords(input));
    }

    @Test
    public void testLowerCaseWords_withSingleWord() {
        String input = "Word";
        String expected = "word";
        assertEquals(expected, Naming.lowerCaseWords(input));
    }

    @Test
    public void testLowerCaseWords_withAlreadyLowerCase() {
        String input = "alreadylowercase";
        String expected = "alreadylowercase";
        assertEquals(expected, Naming.lowerCaseWords(input));
    }

    @Test
    public void testLowerCaseWords_withEmptyString() {
        String input = "";
        String expected = "";
        assertEquals(expected, Naming.lowerCaseWords(input));
    }

    @Test
    public void testLowerCaseWords_withNumbersAndCamelCase() {
        String input = "version2Release";
        String expected = "version 2 release";
        assertEquals(expected, Naming.lowerCaseWords(input));
    }

    @Test
    public void testLowerCaseWords_withNullInput() {
        assertThrows(NullPointerException.class, () -> Naming.lowerCaseWords(null));
    }

    @Test
    public void testLowerCaseWords_withUpperCaseOnly() {
        String input = "UPPERCASE";
        String expected = "uppercase";
        assertEquals(expected, Naming.lowerCaseWords(input));
    }

    @Test
    public void testLowerCaseWords_withLowercaseFollowedByUppercase() {
        String input = "lowerUPPERCase";
        String expected = "lower upper case";
        assertEquals(expected, Naming.lowerCaseWords(input));
    }


}