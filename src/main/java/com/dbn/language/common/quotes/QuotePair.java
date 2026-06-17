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

package com.dbn.language.common.quotes;

import org.jetbrains.annotations.NonNls;

import java.util.Set;
import java.util.regex.Pattern;

import static java.util.concurrent.ConcurrentHashMap.newKeySet;

public class QuotePair {
    private static final Set<String> POSSIBLE_BEGIN_QUOTES = newKeySet();
    private static final Set<String> POSSIBLE_END_QUOTES = newKeySet();

    public static final QuotePair DEFAULT_IDENTIFIER_QUOTE_PAIR = new QuotePair('"', '"');
    private final String beginQuote;
    private final String endQuote;
    private final int quoteSize;

    public QuotePair(char beginQuote, char endChar) {
        this(String.valueOf(beginQuote), String.valueOf(endChar));
    }

    public QuotePair(String quote) {
        this(quote, quote);
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
        return quote(identifier, QuoteEscaping.NONE);
    }

    public String quote(String identifier, QuoteEscaping escaping) {
        // Database identifiers may arrive already quote-wrapped; still escape them as raw names.
        if (escaping == QuoteEscaping.NONE && isQuoted(identifier)) return identifier;

        return beginQuote + escaping.escape(identifier, this) + endQuote;
    }

    public String unquote(String identifier) {
        return unquote(identifier, QuoteEscaping.NONE);
    }

    public String unquote(String identifier, QuoteEscaping escaping) {
        if (!isQuoted(identifier)) return identifier;

        String unquoted = identifier.substring(beginQuote.length(), identifier.length() - endQuote.length());
        return escaping.unescape(unquoted, this);
    }

    public String unquoteComposite(String identifier) {
        return unquoteComposite(identifier, QuoteEscaping.NONE);
    }

    public String unquoteComposite(String identifier, QuoteEscaping escaping) {
        if (beginQuote.isEmpty() || endQuote.isEmpty()) return identifier;

        String quoteBoundary = "(?<=" + Pattern.quote(endQuote) + ")\\.(?=" + Pattern.quote(beginQuote) + ")";
        String[] nameParts = identifier.split(quoteBoundary, -1);
        if (nameParts.length == 1) {
            nameParts = identifier.split("\\.", -1);
        }

        StringBuilder unquoted = new StringBuilder(identifier.length());
        for (int i = 0; i < nameParts.length; i++) {
            if (i > 0) unquoted.append('.');
            unquoted.append(unquote(nameParts[i], escaping));
        }
        return unquoted.toString();
    }

    @NonNls
    @Override
    public String toString() {
        return "quote pair (begin=" + beginQuote + ", end=" + endQuote +')';
    }
}
