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

public class ParseResult{
    public static final ParseResult NO_MATCH_RESULT = new ParseResult(ParseResultType.NO_MATCH, 0);
    private static final ParseResult FULL_MATCH_0 = new ParseResult(ParseResultType.FULL_MATCH, 0);
    private static final ParseResult FULL_MATCH_1 = new ParseResult(ParseResultType.FULL_MATCH, 1);
    private static final ParseResult BORROWED_MATCH_0 = new ParseResult(ParseResultType.BORROWED_MATCH, 0);
    private static final ParseResult BORROWED_MATCH_1 = new ParseResult(ParseResultType.BORROWED_MATCH, 1);
    private static final ParseResult PARTIAL_MATCH_0 = new ParseResult(ParseResultType.PARTIAL_MATCH, 0);

    public final ParseResultType type;
    public final int matchedTokens;

    private ParseResult(ParseResultType type, int matchedTokens) {
        this.type = type;
        this.matchedTokens = matchedTokens;
    }

    public static ParseResult match(ParseResultType type, int matchedTokens) {
        if (matchedTokens == 0) {
            return switch (type) {
                case NO_MATCH -> NO_MATCH_RESULT;
                case FULL_MATCH -> FULL_MATCH_0;
                case BORROWED_MATCH -> BORROWED_MATCH_0;
                case PARTIAL_MATCH -> PARTIAL_MATCH_0;
            };
        }
        if (matchedTokens == 1) {
            return switch (type) {
                case NO_MATCH -> NO_MATCH_RESULT;
                case FULL_MATCH -> FULL_MATCH_1;
                case BORROWED_MATCH -> BORROWED_MATCH_1;
                default -> new ParseResult(type, matchedTokens);
            };
        }
        return new ParseResult(type, matchedTokens);
    }

    @Override
    public String toString() {
        return type.toString();
    }

    public boolean isBetterThan(ParseResult result) {
        return type.getScore() >= result.type.getScore() && matchedTokens > result.matchedTokens;
    }
}
