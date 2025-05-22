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

package com.dbn.database.oracle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OracleMessageParserInterfaceTest {
    private final OracleMessageParserInterface converter = new OracleMessageParserInterface();

    @Test
    public void testNullInput() {
        String result = converter.convertToPresentable(null);
        assertNull("Expected null when input is null", result);
    }

    @Test
    public void testEmptyMessage() {
        String result = converter.convertToPresentable("");
        assertEquals("Expected empty string when input is empty", "", result);
    }

    @Test
    public void testSingleLineMessage() {
        String message = "This is a single line message.";
        String result = converter.convertToPresentable(message);
        assertEquals("Expected single line message to remain unchanged", message, result);
    }

    @Test
    public void testMessageWithEmptyLines() {
        String message = "\nThis is a message with empty lines.\n\nLine two.";
        String expected = "This is a message with empty lines.\nLine two.";
        String result = converter.convertToPresentable(message);
        assertEquals("Expected empty lines to be removed", expected, result);
    }

    @Test
    public void testMessageWithORAErrorLines() {
        String message = "Error message 1\nORA-12345: at line 1\nError message 2\nORA-67890: at line 2";
        String expected = "Error message 1\nError message 2";
        String result = converter.convertToPresentable(message);
        assertEquals("Expected ORA error lines to be removed", expected, result);
    }

    @Test
    public void testMessageWithMixedContent() {
        String message = "Valid line\n\nORA-12345: at line 10\nLine with content.\nORA-67890: at line 20\n";
        String expected = "Valid line\nLine with content.";
        String result = converter.convertToPresentable(message);
        assertEquals("Expected only valid lines to remain", expected, result);
    }

    @Test
    public void testMessageWithoutORAErrorsOrEmptyLines() {
        String message = "Line one\nLine two\nLine three";
        String result = converter.convertToPresentable(message);
        assertEquals("Expected message to remain unchanged", message, result);
    }

    @Test
    public void testMessageWithORAErrorsOnly() {
        String message = "ORA-12345: at line 10\nORA-67890: at line 20";
        String result = converter.convertToPresentable(message);
        assertEquals("Expected result to be empty as all lines are ORA errors", "", result);
    }

    @Test
    public void testMessageWithWhitespaceOnlyLines() {
        String message = "   \nLine one\n  \nLine two\n ";
        String expected = "Line one\nLine two";
        String result = converter.convertToPresentable(message);
        assertEquals("Expected whitespace-only lines to be removed", expected, result);
    }


    @Test
    public void testMessageWithMultipleORAErrors() {
        String message = "ORA-01031: insufficient privileges\n" +
                "ORA-06512: at line 7\n" +
                "ORA-06512: at line 3\n\n" +
                "https://docs.oracle.com/error-help/db/ora-01031/";

        String expected = "ORA-01031: insufficient privileges\nhttps://docs.oracle.com/error-help/db/ora-01031/";

        String result = converter.convertToPresentable(message);

        assertEquals("Expected ORA error lines referencing line numbers to be removed, but keep the actual error message and relevant information", expected, result);
    }

}