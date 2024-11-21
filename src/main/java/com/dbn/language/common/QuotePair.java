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

package com.dbn.language.common;

public class QuotePair {
    private static String POSSIBLE_BEGIN_QUOTES = "";
    private static String POSSIBLE_END_QUOTES = "";

    public static final QuotePair DEFAULT_IDENTIFIER_QUOTE_PAIR = new QuotePair('"', '"');
    private final char beginChar;
    private final char endChar;

    public QuotePair(char beginChar, char endChar) {
        this.beginChar = beginChar;
        this.endChar = endChar;
        POSSIBLE_BEGIN_QUOTES = POSSIBLE_BEGIN_QUOTES.indexOf(beginChar) == -1 ? POSSIBLE_BEGIN_QUOTES + beginChar : POSSIBLE_BEGIN_QUOTES;
        POSSIBLE_END_QUOTES = POSSIBLE_END_QUOTES.indexOf(endChar) == -1 ? POSSIBLE_END_QUOTES + endChar : POSSIBLE_END_QUOTES;
    }

    public static boolean isPossibleBeginQuote(char chr) {
        return POSSIBLE_BEGIN_QUOTES.indexOf(chr) > -1;
    }

    public static boolean isPossibleEndQuote(char chr) {
        return POSSIBLE_END_QUOTES.indexOf(chr) > -1;
    }

    public char beginChar() {
        return beginChar;
    }

    public char endChar() {
        return endChar;
    }

    public boolean isQuoted(CharSequence charSequence) {
        char firstChar = charSequence.charAt(0);
        char lastChar = charSequence.charAt(charSequence.length() - 1);

        return firstChar == beginChar && lastChar == endChar;
    }

    public String quote(String identifier) {
        return beginChar + identifier + endChar;
    }

    public String unquote(String identifier) {
        int length = identifier.length();
        if (length < 2) return identifier;

        char firstChar = identifier.charAt(0);
        char lastChar = identifier.charAt(length - 1);

        if (firstChar == beginChar && lastChar == endChar) {
            return identifier.substring(1, length - 1);
        }
        return identifier;
    }

    @Override
    public String toString() {
        return "quote pair (begin=" + beginChar + ", end=" + endChar +')';
    }
}
