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

import org.jetbrains.annotations.NonNls;

import java.util.Arrays;

public class QuoteDefinition {
    public static final QuoteDefinition DEFAULT_IDENTIFIER_QUOTE_DEFINITION = new QuoteDefinition(QuotePair.DEFAULT_IDENTIFIER_QUOTE_PAIR);
    private final QuotePair[] quotePairs;

    public QuoteDefinition(QuotePair... quotePairs) {
        this.quotePairs = quotePairs;
    }

    public QuotePair getDefaultQuotes() {
        return quotePairs[0];
    }

    public QuotePair getQuote(char character) {
        for (QuotePair quotePair : quotePairs) {
            if (character == quotePair.beginChar() || character == quotePair.endChar()) {
                return quotePair;
            }
        }
        return null;
    }

    public boolean isQuoted(CharSequence charSequence) {
        for (QuotePair quotePair : quotePairs) {
            if (quotePair.isQuoted(charSequence)) {
                return true;
            }
        }
        return false;
    }

    public boolean isQuoteBegin(char character) {
        for (QuotePair quotePair : quotePairs) {
            if (quotePair.beginChar() == character) {
                return true;
            }
        }
        return false;
    }

    public boolean isQuoteEnd(char beginQuote, char character) {
        for (QuotePair quotePair : quotePairs) {
            if (quotePair.beginChar() == beginQuote && quotePair.endChar() == character) {
                return true;
            }
        }
        return false;
    }

    @NonNls
    @Override
    public String toString() {
        return "quote definition (pairs=" + Arrays.toString(quotePairs) +')';
    }
}
