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

import com.dbn.common.compatibility.Compatibility;
import com.dbn.common.compatibility.Workaround;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NamingTest {

    @Test
    public void testNextNumberedIdentifier_withLargeNumericSuffix() {
        String identifier = "35679075542";
        assertEquals("356790755422", Naming.nextNumberedIdentifier(identifier, false,
                () -> Set.of(identifier, "356790755421")));
        assertEquals("35679075542 2", Naming.nextNumberedIdentifier(identifier, true,
                () -> Set.of(identifier, "35679075542 1")));
    }

    @Test
    public void testNextNumberedIdentifier_withFreeIdentifier() {
        assertEquals("Connection", Naming.nextNumberedIdentifier("Connection", false, name -> false));
        assertEquals("12345", Naming.nextNumberedIdentifier("12345", false, name -> false));
    }

    @Test
    public void testNextNumberedIdentifier_withPrefixedIdentifier() {
        assertEquals("Connection1", Naming.nextNumberedIdentifier("Connection", false, Set.of("Connection")::contains));
        assertEquals("Connection 1", Naming.nextNumberedIdentifier("Connection", true, Set.of("Connection")::contains));
        assertEquals("Connection 3", Naming.nextNumberedIdentifier("Connection 1", true,
                Set.of("Connection 1", "Connection 2")::contains));
    }

    @Test
    public void testNextNumberedIdentifier_withExistingNumber() {
        assertEquals("Session 1", Naming.nextNumberedIdentifier("Session 1", true, name -> false));
        assertEquals("Session 2", Naming.nextNumberedIdentifier("Session 1", true,
                Set.of("Session 1")::contains));
        assertEquals("Session 4", Naming.nextNumberedIdentifier("Session 1", true,
                Set.of("Session 1", "Session 2", "Session 3")::contains));
    }

    @Test
    public void testNextNumberedIdentifier_withPureNumericIdentifier() {
        assertEquals("123451", Naming.nextNumberedIdentifier("12345", false,
                Set.of("12345")::contains));
        assertEquals("12345 1", Naming.nextNumberedIdentifier("12345", true,
                Set.of("12345")::contains));
        assertEquals("123453", Naming.nextNumberedIdentifier("12345", false,
                Set.of("12345", "123451", "123452")::contains));
    }

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
    @Workaround // no "assertThrows" in older junit versions (intellij 2020.x)
    @Compatibility
    public void testLowerCaseWords_withNullInput() {
        //assertThrows(NullPointerException.class, () -> Naming.lowerCaseWords(null));
        Exception exception = null;
        try {
            Naming.lowerCaseWords(null);
        } catch (Exception e) {
            exception = e;
        }

        assertTrue(exception instanceof NullPointerException);
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
