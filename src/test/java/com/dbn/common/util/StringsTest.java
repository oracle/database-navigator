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

package com.dbn.common.util;

import junit.framework.TestCase;

import java.util.List;

public class StringsTest extends TestCase {

    public void testTrim1() {
        StringBuilder builder = new StringBuilder();

        Strings.trim(builder);
        assertEquals("", builder.toString());
    }

    public void testTrim2() {
        StringBuilder builder = new StringBuilder("\n\t\n  \n   \n\t  ");

        Strings.trim(builder);
        assertEquals("", builder.toString());
    }

    public void testTrim3() {
        StringBuilder builder = new StringBuilder("test \n   \n\t");

        Strings.trim(builder);
        assertEquals("test", builder.toString());
    }

    public void testTrim4() {
        StringBuilder builder = new StringBuilder("\n\t\n test");

        Strings.trim(builder);
        assertEquals("test", builder.toString());
    }

    public void testTrim5() {
        StringBuilder builder = new StringBuilder("\n\t\n test \n   \n\t");

        Strings.trim(builder);
        assertEquals("test", builder.toString());
    }

    public void testSlice1() {
        String input = "This is a test string. Sentence 1. Sentence 2. Sentence 3.";
        List<String> slices = Strings.slice(input, new int[]{23, 35, 47, 58});
        List<String> expected = List.of("This is a test string. ", "Sentence 1. ", "Sentence 2. ", "Sentence 3.");
        assertEquals(expected, slices);
    }

    public void testSlice2() {
        String input = "This is a test string. Sentence 1. Sentence 2. Sentence 3.";
        List<String> slices = Strings.slice(input, new int[]{0, 23, 35, 47, 58, 66});
        List<String> expected = List.of("This is a test string. ", "Sentence 1. ", "Sentence 2. ", "Sentence 3.");
        assertEquals(expected, slices);
    }

    public void testSlice3() {
        String input = "This is a test string. Sentence 1. Sentence 2. Sentence 3.";
        List<String> slices = Strings.slice(input, new int[]{23, 35, 47});
        List<String> expected = List.of("This is a test string. ", "Sentence 1. ", "Sentence 2. ", "Sentence 3.");
        assertEquals(expected, slices);
    }

    public void testSlice4() {
        String input = "This is a test string. Sentence 1. Sentence 2. Sentence 3.";
        List<String> slices = Strings.slice(input, new int[0]);
        List<String> expected = List.of(input);
        assertEquals(expected, slices);
    }

    public void testRemoveHtmlTags1() {
        String content = Strings.removeHtmlTags("""
                <!DOCTYPE html>
                <html>
                <head>
                  <title>Sample Page</title>
                  <style>
                    body { background: #f3f3f3; }
                    h1 { color: blue; }
                  </style>
                </head>
                <body>
                  <!-- This is a comment -->
                  <h1>Welcome to <span>The Test</span> Page</h1>
                  <script type="text/javascript">
                    alert("Hello, world!");
                  </script>
                  <p>This is a <b>sample</b> paragraph.<br>Here is a line break.</p>
                  <noscript>Javascript is disabled.</noscript>
                  <iframe src="https://example.com"></iframe>
                  <div>
                    <ul>
                      <li>One</li>
                      <li>Two <em>(second)</em></li>
                    </ul>
                  </div>
                  Text outside tags.
                </body>
                </html>
                """);
        assertEquals("Sample Page Welcome to The Test Page This is a sample paragraph.Here is a line break. Javascript is disabled. One Two (second) Text outside tags.", content);
    }

    public void testIndentText() {
        String indented = Strings.indentText("""
                Line1
                Line2
                Line3""", 4);
        assertEquals("    Line1\n    Line2\n    Line3", indented);
    }
}