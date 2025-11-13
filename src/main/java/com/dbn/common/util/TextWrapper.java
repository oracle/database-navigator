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
import lombok.experimental.UtilityClass;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class TextWrapper {
    /**
     * Takes an input text and splits any non-spaced block of characters longer than
     * MAX_BLOCK_LENGTH. The split occurs on non-letter characters if possible.
     *
     * @param input The original text.
     * @return The transformed text with long unbroken blocks wrapped appropriately.
     */
    public static String wrapText(String input, int maxBlockLength) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();

        // Split text by whitespace but keep the delimiters
        Matcher matcher = Pattern.compile("\\S+|\\s+").matcher(input);

        while (matcher.find()) {
            String token = matcher.group();

            // Only process non-whitespace tokens
            if (!token.trim().isEmpty() && !token.matches("\\s+")) {
                if (token.length() > maxBlockLength) {
                    result.append(splitLongToken(token, maxBlockLength));
                } else {
                    result.append(token);
                }
            } else {
                // Preserve whitespace
                result.append(token);
            }
        }

        return result.toString();
    }

    /**
     * Splits a long non-spaced token into smaller chunks.
     * The preferred split points are non-letter characters (like '/', '.', '-', '_').
     */
    private static String splitLongToken(String token, int maxBlockLength) {
        StringBuilder builder = new StringBuilder();
        int start = 0;

        while (start < token.length()) {
            int end = Math.min(start + maxBlockLength, token.length());
            String chunk = token.substring(start, end);

            // If the chunk ends mid-word, try to backtrack to the last non-letter character
            if (end < token.length()) {
                int backtrack = findSplitPoint(chunk);
                if (backtrack != -1) {
                    end = start + backtrack + 1;
                    chunk = token.substring(start, end);
                }
            }

            builder.append(chunk);

            // Add a carriage return if not at the end
            if (end < token.length()) {
                builder.append("\n");
            }

            start = end;
        }

        return builder.toString();
    }

    /**
     * Finds the last non-letter character in a given substring to use as a split point.
     * Returns -1 if none found.
     */
    private static int findSplitPoint(String text) {
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (!Character.isLetter(c)) {
                return i;
            }
        }
        return -1;
    }
}


