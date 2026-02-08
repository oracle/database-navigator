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

import com.dbn.common.util.Strings;
import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.SimpleTokenType;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.parser.ElementTypeParser;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParserBuilder;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.ParserNode;
import com.intellij.lang.PsiBuilder.Marker;
import lombok.extern.slf4j.Slf4j;

import static com.dbn.language.common.element.parser.ParseResultType.FULL_MATCH;
import static com.dbn.language.common.element.parser.ParseResultType.NO_MATCH;

@Slf4j
public class TokenElementTypeParser extends ElementTypeParser<TokenElementType> {
    public TokenElementTypeParser(TokenElementType elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) {
        ParserBuilder builder = context.builder;
        Marker marker = null;

        TokenType token = builder.getToken();
        if (token == elementType.tokenType) {
            if (elementType.isSurrogate()) {
                return stepOut(marker, context, FULL_MATCH, 0);
            }

            if (context.isSurrogate()) {
                if (context.isSurrogateFor(elementType)) {
                    marker = builder.markAndAdvance();
                    return stepOut(marker, context, FULL_MATCH, 1);
                } else /*if (!builder.getTokenText().equals("("))*/ {
                    return stepOut(marker, context, NO_MATCH, 0);
                }
            }
        }

        if (token == elementType.tokenType || builder.isDummyToken()) {
            String text = elementType.getText();
            if (Strings.isNotEmpty(text) && Strings.equalsIgnoreCase(builder.getTokenText(), text)) {
                marker = builder.markAndAdvance();
                return stepOut(marker, context, FULL_MATCH, 1);
            }

            if (token.isSuppressibleReservedWord()) {
                SharedTokenTypeBundle sharedTokenTypes = elementType.bundle.tokenTypeBundle.getSharedTokenTypes();
                SimpleTokenType leftParenthesis = sharedTokenTypes.getChrLeftParenthesis();
                SimpleTokenType dot = sharedTokenTypes.getChrDot();

                TokenType nextTokenType = builder.getNextToken();
                if (nextTokenType == dot && !elementType.isNextPossibleToken(dot, parentNode, context)) {
                    context.setWavedTokenType(token);
                    return stepOut(marker, context, NO_MATCH, 0);
                }
                if (token.isFunction() && elementType.getFlavor() == null) {
                    if (nextTokenType != leftParenthesis && elementType.isNextRequiredToken(leftParenthesis, parentNode, context)) {
                        context.setWavedTokenType(token);
                        return stepOut(marker, context, NO_MATCH, 0);
                    }
                }
            }

            marker = builder.markAndAdvance();
            return stepOut(marker, context, FULL_MATCH, 1);
        }
        return stepOut(marker, context, NO_MATCH, 0);
    }
}
