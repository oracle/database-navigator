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

import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.IdentifierElementType;
import com.dbn.language.common.element.parser.ElementTypeParser;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParserBuilder;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.ParserNode;
import com.intellij.lang.PsiBuilder.Marker;

import static com.dbn.language.common.element.parser.ParseResult.NO_MATCH_RESULT;
import static com.dbn.language.common.element.parser.ParseResultType.BORROWED_MATCH;
import static com.dbn.language.common.element.parser.ParseResultType.FULL_MATCH;

public class IdentifierElementTypeParser extends ElementTypeParser<IdentifierElementType> {
    public IdentifierElementTypeParser(IdentifierElementType elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) {
        ParserBuilder builder = context.builder;
        if (context.isSurrogateFor(elementType) && parentNode.matchedTokens == 0) {
            if (elementType.isSurrogate()) {
                return stepOut(null, context, BORROWED_MATCH, 0);
            } else {
                Marker marker = builder.markAndAdvance();
                return stepOut(marker, context, FULL_MATCH, 1);
            }
        }

        TokenType token = builder.getToken();
        if (token == null) return NO_MATCH_RESULT;

        // if reserved word, verify if suppressible in this context (i.e. converted to identifier)
        if (token.isIdentifier() || isSuppressibleReservedWord(parentNode, context, token)) {
            if (elementType.isSurrogate()) {
                return stepOut(null, context, FULL_MATCH, 0);
            }

            Marker marker = builder.markAndAdvance();
            return stepOut(marker, context, FULL_MATCH, 1);
        }

        return NO_MATCH_RESULT;
    }

    private boolean isSuppressibleReservedWord(ParserNode parentNode, ParserContext context, TokenType tokenType) {
        if (!tokenType.isSuppressibleReservedWord()) return false;
        if (context.isWavedTokenType(tokenType)) return true;
        if (elementType.isDefinition() && !elementType.isAlias()) return true;

        return isSuppressibleReservedWord(tokenType, parentNode, context);
    }
}