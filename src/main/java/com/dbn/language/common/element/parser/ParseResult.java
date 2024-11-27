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

package com.dbn.language.common.element.parser;

import lombok.Getter;

@Getter
public class ParseResult{
    private static final ParseResult NO_MATCH = new ParseResult(ParseResultType.NO_MATCH, 0);

    private final ParseResultType type;
    private final int matchedTokens;

    private ParseResult(ParseResultType type, int matchedTokens) {
        this.type = type;
        this.matchedTokens = matchedTokens;
    }

    public static ParseResult match(ParseResultType type, int matchedTokens) {
        return new ParseResult(type, matchedTokens);
    }

    public static ParseResult noMatch() {
        return NO_MATCH;
    }

    public boolean isNoMatch() {
        return type == ParseResultType.NO_MATCH;
    }

    public boolean isFullMatch() {
        return type == ParseResultType.FULL_MATCH;
    }

    public boolean isPartialMatch() {
        return type == ParseResultType.PARTIAL_MATCH;
    }


    public boolean isMatch() {
        return isFullMatch() || isPartialMatch();
    }

    @Override
    public String toString() {
        return type.toString();
    }

    public boolean isBetterThan(ParseResult result) {
        return type.getScore() >= result.type.getScore() && matchedTokens > result.matchedTokens;
    }
}
