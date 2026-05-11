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

import com.intellij.util.containers.ContainerUtil;

import java.util.Set;

public class QuotePair {
    private static final Set<String> POSSIBLE_BEGIN_QUOTES = ContainerUtil.newConcurrentSet();
    private static final Set<String> POSSIBLE_END_QUOTES = ContainerUtil.newConcurrentSet();

    public static final QuotePair DEFAULT_IDENTIFIER_QUOTE_PAIR = new QuotePair('"', '"');
    private final String beginQuote;
    private final String endQuote;
    private final int quoteSize;

    public QuotePair(char beginQuote, char endChar) {
        this(String.valueOf(beginQuote), String.valueOf(endChar));
    }
    public QuotePair(String beginQuote, String endQuote) {
        this.beginQuote = beginQuote;
        this.endQuote = endQuote;
        this.quoteSize = beginQuote.length() + endQuote.length();
        POSSIBLE_BEGIN_QUOTES.add(beginQuote.intern());
        POSSIBLE_END_QUOTES.add(endQuote.intern());
    }

    public static boolean isPossibleBeginQuote(char chr) {
        return POSSIBLE_BEGIN_QUOTES.contains(String.valueOf(chr));
    }

    public static boolean isPossibleEndQuote(char chr) {
        return POSSIBLE_END_QUOTES.contains(String.valueOf(chr));
    }

    public String beginQuote() {
        return beginQuote;
    }

    public String endQuote() {
        return endQuote;
    }

    public boolean isQuoted(CharSequence charSequence) {
        if (charSequence.length() < quoteSize) return false;

        String string = charSequence.toString();
        return string.startsWith(beginQuote) && string.endsWith(endQuote);
    }

    public String quote(String identifier) {
        if (isQuoted(identifier)) return identifier;

        return beginQuote + identifier + endQuote;
    }

    /**
     * Quotes identifier with Java-escaped quotes.  Does
     * not modify a string that is already escape-quoted
     * but does add escaping if the value is normally quoted.
     * @param identifier
     * @return the quoted version of identifier
     */
    public String quoteWithJavaEscape(String identifier) {
        if (!identifier.startsWith("\\"+beginQuote)) {
           if (identifier.startsWith(beginQuote)) {
                // already quoted without escape
                identifier = "\\" + identifier;
            }
           else {
               // not lead quoted at all
               identifier = "\\\"" + identifier;
           }
        }

        if (!identifier.endsWith("\\"+endQuote)) {
            if (identifier.endsWith(endQuote)) {
                // trailing unescaped quote so strip existing quote and add escaped.
                identifier = identifier.substring(0, identifier.length()-1);
                identifier += "\\\"";
            }
            else {
                identifier += "\\\"";
            }
        }
        return identifier;
    }
    public String unquote(String identifier) {
        if (!isQuoted(identifier)) return identifier;

        return identifier.substring(beginQuote.length(), identifier.length() - endQuote.length());
    }

    @Override
    public String toString() {
        return "quote pair (begin=" + beginQuote + ", end=" + endQuote +')';
    }
}
