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
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.parser.ElementTypeParser;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParserBuilder;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.ParserNode;
import com.intellij.lang.PsiBuilder.Marker;
import lombok.extern.slf4j.Slf4j;

import static com.dbn.language.common.element.parser.ParseResult.NO_MATCH_RESULT;
import static com.dbn.language.common.element.parser.ParseResultType.BORROWED_MATCH;
import static com.dbn.language.common.element.parser.ParseResultType.FULL_MATCH;
import static com.dbn.language.common.element.parser.ParseResultType.NO_MATCH;
import static com.dbn.language.common.element.util.ElementTypeAttribute.SURROGATE_LEAD;

@Slf4j
public class TokenElementTypeParser extends ElementTypeParser<TokenElementType> {
    public TokenElementTypeParser(TokenElementType elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) {
        ParserBuilder builder = context.builder;
        ElementTypeBase parentElement = parentNode.element;

        TokenType builderToken = builder.getToken();
        if (builderToken == null) return NO_MATCH_RESULT;


        ParseResult surrogateResult = parseSurrogate(context, parentElement);
        if (surrogateResult != null) return surrogateResult;


        if (isTokenMatch(builder) || builder.isDummyToken()) {
            String text = elementType.getText();
            if (Strings.isNotEmpty(text) && Strings.equalsIgnoreCase(builder.getTokenText(), text)) {
                Marker marker = builder.markAndAdvance();
                return stepOut(marker, context, FULL_MATCH, 1);
            }

            if (builderToken.isSuppressibleReservedWord()) {
                SharedTokenTypeBundle sharedTokenTypes = getSharedTokenTypes();
                SimpleTokenType leftParenthesis = sharedTokenTypes.getChrLeftParenthesis();
                SimpleTokenType dot = sharedTokenTypes.getChrDot();

                TokenType nextTokenType = builder.getNextToken();
                if (nextTokenType == dot && !elementType.isNextPossibleToken(dot, parentNode, context)) {
                    context.setWavedTokenType(builderToken);
                    return stepOut(null, context, NO_MATCH, 0);
                }
                if (builderToken.isFunction() && elementType.getFlavor() == null) {
                    if (nextTokenType != leftParenthesis && elementType.isNextRequiredToken(leftParenthesis, parentNode, context)) {
                        context.setWavedTokenType(builderToken);
                        return stepOut(null, context, NO_MATCH, 0);
                    }
                }
            }

            Marker marker = builder.markAndAdvance();
            return stepOut(marker, context, FULL_MATCH, 1);
        }

        if (isConsumedMatch(builder)) {
            return stepOut(null, context, BORROWED_MATCH, 1);
        }

        return NO_MATCH_RESULT;
    }

    private ParseResult parseSurrogate(ParserContext context, ElementTypeBase parentElement) {
        ParserBuilder builder = context.builder;

        if (context.isSurrogate()) {
            if (context.isSurrogateFor(elementType)) {
                if (elementType.is(SURROGATE_LEAD)) {
                    // chained surrogate lead match
                    return stepOut(null, context, FULL_MATCH, 0);
                }

                if (isConsumedMatch(builder)) {
                    // consumed surrogate target match
                    return stepOut(null, context, BORROWED_MATCH, 0);
                }

                if (isTokenMatch(builder)) {
                    // actual surrogate target match
                    Marker marker = builder.markAndAdvance();
                    return stepOut(marker, context, FULL_MATCH, 0);
                }

                return NO_MATCH_RESULT;
            }
        }

        if (elementType.is(SURROGATE_LEAD)) {
            if (isTokenMatch(builder)) {
                // surrogate lead match (soft)
                return stepOut(null, context, FULL_MATCH, 0);
            }
            if (isConsumedMatch(builder)) {
                // surrogate lead match (soft)
                return stepOut(null, context, BORROWED_MATCH, 0);
            }
        }

        return null;
    }

    private boolean isTokenMatch(ParserBuilder builder) {
        return elementType.tokenType == builder.getToken();
    }

    private boolean isConsumedMatch(ParserBuilder builder) {
        return builder.tokenPairMonitor.isConsumedMatch(elementType.tokenType);
    }
}
