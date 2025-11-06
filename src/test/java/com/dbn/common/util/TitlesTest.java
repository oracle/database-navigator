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

public class TitlesTest {

    /**
     * Tests for the `titleCased` method in the `Titles` utility class.
     * This method converts a given string into title case format, with consideration for acronyms which should remain uppercase.
     */

    @Test
    public void testEmptyString() {
        // Test for an empty string
        String input = "";
        String expected = "";
        String actual = Titles.titleCased(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testNullString() {
        // Test for a null string (should return null)
        String input = null;
        String expected = null;
        String actual = Titles.titleCased(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testSingleWord() {
        // Test for a single word
        String input = "hello";
        String expected = "Hello";
        String actual = Titles.titleCased(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testSingleAcronym() {
        // Test for a single acronym
        String input = "api";
        String expected = "API"; // Should remain uppercase
        String actual = Titles.titleCased(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testSentenceWithSpaces() {
        // Test for a sentence with mixed words and spaces
        String input = "hello world";
        String expected = "Hello World";
        String actual = Titles.titleCased(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testSentenceWithPunctuation() {
        // Test for a sentence with punctuation
        String input = "hello, world!";
        String expected = "Hello, World!";
        String actual = Titles.titleCased(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testSentenceWithAcronyms() {
        // Test for a sentence with acronyms and regular words
        String input = "this is an api test";
        String expected = "This Is An API Test"; // "API" should remain uppercase
        String actual = Titles.titleCased(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testSentenceWithMixedDelimiters() {
        // Test for a sentence with mixed delimiters (spaces, hyphens, underscores)
        String input = "hello-world_this is a_test";
        String expected = "Hello-World_This Is A_Test";
        String actual = Titles.titleCased(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testObjectIdentifier() {
        // Test for a sentence with mixed delimiters (spaces, hyphens, underscores)
        String input = "SOME_DATABASE_OBJECT_IDENTIFIER";
        String expected = "Some_Database_Object_Identifier";
        String actual = Titles.titleCased(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testObjectIdentifierWithNumbers() {
        // Test for a sentence with mixed delimiters (spaces, hyphens, underscores)
        String input = "SOME_DATABASE_45OBJECT_32IDENTIFIER44";
        String expected = "Some_Database_45Object_32Identifier44";
        String actual = Titles.titleCased(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testAllUppercaseInput() {
        // Test for input where all words are uppercase
        String input = "HELLO WORLD";
        String expected = "Hello World";
        String actual = Titles.titleCased(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testAcronymsAlreadyUppercase() {
        // Test for a sentence where acronyms are already uppercase
        String input = "this is an API test";
        String expected = "This Is An API Test"; // "API" should remain uppercase
        String actual = Titles.titleCased(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testNumbersAndWords() {
        // Test for a sentence with numbers and words
        String input = "hello 123 world";
        String expected = "Hello 123 World";
        String actual = Titles.titleCased(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testOnlyDelimiters() {
        // Test for input that contains only delimiters
        String input = "---___   !!!";
        String expected = "---___   !!!"; // Should remain unchanged
        String actual = Titles.titleCased(input);
        assertEquals(expected, actual);
    }

    @Test
    public void testMixedCasingInput() {
        // Test for mixed casing input
        String input = "HeLLo WoRLd";
        String expected = "Hello World";
        String actual = Titles.titleCased(input);
        assertEquals(expected, actual);
    }
}