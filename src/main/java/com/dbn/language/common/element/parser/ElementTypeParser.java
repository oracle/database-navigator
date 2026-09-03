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

import com.dbn.common.util.Commons;
import com.dbn.language.common.ParseException;
import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.SimpleTokenType;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.BlockElementType;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.path.ParserNode;
import com.dbn.language.common.element.util.ElementTypeUtil;
import com.dbn.language.common.element.util.ParseBuilderErrorHandler;
import com.intellij.lang.PsiBuilder.Marker;

import java.util.Set;

import static com.dbn.language.common.element.parser.ParseResult.NO_MATCH_RESULT;
import static com.dbn.language.common.element.parser.ParseResult.match;
import static com.dbn.language.common.element.parser.ParseResultType.BORROWED_MATCH;
import static com.dbn.language.common.element.parser.ParseResultType.NO_MATCH;
import static com.dbn.language.common.element.parser.ParseResultType.PARTIAL_MATCH;
import static com.dbn.language.common.element.util.ElementTypeAttribute.IDENTIFIER_LEAD;

public abstract class ElementTypeParser<T extends ElementTypeBase> {
    public final T elementType;
    public final SharedTokenTypeBundle sharedTokenTypes;

    public ElementTypeParser(T elementType) {
        this.elementType = elementType;
        this.sharedTokenTypes = elementType.bundle.tokenTypeBundle.getSharedTokenTypes();
    }

    public ParserNode stepIn(ParserNode parentNode, ParserContext context) {
        ParserBuilder builder = context.builder;
        ParserNode node = new ParserNode(elementType, parentNode, builder.getOffset(), 0);
        node.elementMarker = builder.mark(node);
        return node;
    }

    public ParseResult stepOut(ParserNode node, ParserContext context, ParseResultType resultType) {
        return stepOut(null, node, context, resultType, node == null ? 0 : node.matchedTokens);
    }

    public ParseResult stepOut(Marker marker, ParserContext context, ParseResultType resultType, int matchedTokens) {
        return stepOut(marker, null, context, resultType, matchedTokens);
    }

    private ParseResult stepOut(Marker marker, ParserNode node, ParserContext context, ParseResultType resultType, int matchedTokens) {
        try {
            ParserBuilder builder = context.builder;
            marker = marker == null ? node == null ? null : node.elementMarker : marker;
            if (resultType == PARTIAL_MATCH) {
                ElementTypeBase offsetPsiElement = Commons.nvl(builder.tokenMonitor.lastLeaf, elementType);
                Set<TokenType> nextPossibleTokens = offsetPsiElement.cache.getNextPossibleTokens();
                ParseBuilderErrorHandler.updateBuilderError(nextPossibleTokens, context);
            }

            if (resultType == NO_MATCH) {
                builder.markerRollbackTo(marker, elementType);
            } else {
                if (elementType instanceof BlockElementType)
                    builder.markerDrop(marker); else
                    builder.markerDone(marker, elementType);
            }


            if (resultType == NO_MATCH) {
                return NO_MATCH_RESULT;
            } else {
                Branch branch = this.elementType.branch;
                if (node != null && branch != null) {
                    // if node is matched add branches marker
                    context.addBranchMarker(node, branch);
                }
                if (elementType instanceof LeafElementType leafElementType) {
                    builder.tokenMonitor.markResolved(leafElementType);
                    builder.tokenPairMonitor.acknowledge(leafElementType, resultType == BORROWED_MATCH);
                }

                return match(resultType, matchedTokens);
            }
        } finally {
            if (node != null) {
                context.removeBranchMarkers(node);
                node.detach();

            }

        }
    }

    /**
     * Returns true if the token is a reserved word, but can act as an identifier in this context.
     */
    protected boolean isSuppressibleReservedWord(TokenType tokenType, ParserNode node, ParserContext context) {
        if (tokenType == null) return false;
        if (!tokenType.isSuppressibleReservedWord()) return false;


        SimpleTokenType dot = context.sharedTokenTypes.chrDot;
        SimpleTokenType leftParenthesis = sharedTokenTypes.chrLeftParenthesis;
        ParserBuilder builder = context.builder;

        TokenType previousToken = builder.getPreviousToken();
        if (previousToken == dot) return true;

        TokenType nextToken = builder.getNextToken();
        if (nextToken == dot) return true;

        LeafElementType lastResolvedLeaf = builder.tokenMonitor.lastLeaf;
        if (lastResolvedLeaf != null && lastResolvedLeaf.is(IDENTIFIER_LEAD)) return true;

        if (tokenType.isFunction()) {
            if (nextToken == leftParenthesis) return false;

            if (elementType instanceof LeafElementType leafElementType) {
                return !leafElementType.isNextRequiredToken(leftParenthesis, node);
            }
        }

        ElementTypeBase namedElementType = ElementTypeUtil.getEnclosingNamedElementType(node);
        if (namedElementType != null && namedElementType.cache.containsToken(tokenType)) {
            return lastResolvedLeaf != null && !lastResolvedLeaf.isNextPossibleToken(tokenType, node);
        }

        if (lastResolvedLeaf != null) {
            if (lastResolvedLeaf.isNextPossibleToken(tokenType, node)) {
                return false;
            }
        }

        return true;//!isFollowedByToken(tokenType, node);
    }

    protected boolean shouldParseElement(ElementTypeBase elementType, ParserNode node, ParserContext context) {
        ParserBuilder builder = context.builder;
        TokenType token = builder.getToken();
        if (token == null) return false;

        if (builder.isDummyToken()) return true;
        if (elementType.cache.couldStartWithToken(token)) return true;

        if (builder.tokenPairMonitor.hasConsumedMatch(elementType)) return true;
        if (isSuppressibleReservedWord(token, node, context)) return true;

        return false;
    }

    @Override
    public String toString() {
        return elementType.toString();
    }

    protected SharedTokenTypeBundle getSharedTokenTypes() {
        return elementType.getLanguage().getSharedTokenTypes();
    }

    public abstract ParseResult parse(ParserNode parentNode, ParserContext context) throws ParseException;
}
