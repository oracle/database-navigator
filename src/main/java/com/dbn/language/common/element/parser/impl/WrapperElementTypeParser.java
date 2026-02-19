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

package com.dbn.language.common.element.parser.impl;

import com.dbn.language.common.ParseException;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.impl.WrapperElementType;
import com.dbn.language.common.element.parser.ElementTypeParser;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParserBuilder;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.ParserNode;
import com.dbn.language.common.element.util.ParseBuilderErrorHandler;

import java.util.Set;

import static com.dbn.language.common.element.parser.ParseResultType.FULL_MATCH;
import static com.dbn.language.common.element.parser.ParseResultType.NO_MATCH;
import static com.dbn.language.common.element.parser.ParseResultType.PARTIAL_MATCH;

public class WrapperElementTypeParser extends ElementTypeParser<WrapperElementType> {
    public WrapperElementTypeParser(WrapperElementType elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) throws ParseException {
        ParserBuilder builder = context.builder;
        ParserNode node = stepIn(parentNode, context);

        ElementTypeBase wrappedElement = elementType.wrappedElement;
        TokenElementType beginTokenElement = elementType.getBeginTokenElement();
        TokenElementType endTokenElement = elementType.getEndTokenElement();

        // parse begin token
        ParseResult beginTokenResult = beginTokenElement.parser.parse(node, context);

        boolean isStrong = elementType.isStrong();
        if (beginTokenResult.type != NO_MATCH) {
            node.matchedTokens++;

            ParseResult wrappedResult = wrappedElement.parser.parse(node, context);
            node.matchedTokens += wrappedResult.matchedTokens;

            if (wrappedResult.type == NO_MATCH  && !elementType.wrappedElementOptional) {
                if (!isStrong && builder.getToken() != endTokenElement.tokenType) {
                    return stepOut(node, context, NO_MATCH);
                } else {
                    Set<TokenType> possibleTokens = wrappedElement.cache.getFirstPossibleTokens();
                    ParseBuilderErrorHandler.updateBuilderError(possibleTokens, context);

                }
            }

            // check the end element => exit with partial match if not available
            ParseResult endTokenResult = endTokenElement.parser.parse(node, context);
            if (endTokenResult.type != NO_MATCH) {
                node.matchedTokens++;
                return stepOut(node, context, FULL_MATCH);
            } else {
                return stepOut(node, context, PARTIAL_MATCH);
            }
        }

        return stepOut(node, context, NO_MATCH);
    }
}