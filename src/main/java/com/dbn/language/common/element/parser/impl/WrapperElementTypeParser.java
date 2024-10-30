package com.dbn.language.common.element.parser.impl;

import com.dbn.language.common.ParseException;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.impl.WrapperElementType;
import com.dbn.language.common.element.impl.WrappingDefinition;
import com.dbn.language.common.element.parser.ElementTypeParser;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParseResultType;
import com.dbn.language.common.element.parser.ParserBuilder;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.parser.TokenPairMonitor;
import com.dbn.language.common.element.path.ParserNode;
import com.dbn.language.common.element.util.ParseBuilderErrorHandler;

import java.util.Set;

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

        int matchedTokens = 0;

        // parse begin token
        ParseResult beginTokenResult = beginTokenElement.parser.parse(node, context);

        TokenType beginTokenType = beginTokenElement.tokenType;
        TokenType endTokenType = endTokenElement.tokenType;
        boolean isStrong = elementType.isStrong();

        TokenPairMonitor tokenPairMonitor = builder.tokenPairMonitor;
        boolean beginMatched = beginTokenResult.isMatch() || (builder.getPreviousToken() == beginTokenType && !tokenPairMonitor.isExplicitRange(beginTokenType));
        if (beginMatched) {
            matchedTokens++;
            boolean initialExplicitRange = tokenPairMonitor.isExplicitRange(beginTokenType);
            tokenPairMonitor.setExplicitRange(beginTokenType, true);

            ParseResult wrappedResult = wrappedElement.parser.parse(node, context);
            matchedTokens = matchedTokens + wrappedResult.getMatchedTokens();

            ParseResultType wrappedResultType = wrappedResult.getType();
            if (wrappedResultType == ParseResultType.NO_MATCH  && !elementType.wrappedElementOptional) {
                if (!isStrong && builder.getToken() != endTokenType) {
                    tokenPairMonitor.setExplicitRange(beginTokenType, initialExplicitRange);
                    return stepOut(node, context, ParseResultType.NO_MATCH, matchedTokens);
                } else {
                    Set<TokenType> possibleTokens = wrappedElement.cache.getFirstPossibleTokens();
                    ParseBuilderErrorHandler.updateBuilderError(possibleTokens, context);

                }
            }

            // check the end element => exit with partial match if not available
            ParseResult endTokenResult = endTokenElement.parser.parse(node, context);
            if (endTokenResult.isMatch()) {
                matchedTokens++;
                return stepOut(node, context, ParseResultType.FULL_MATCH, matchedTokens);
            } else {
                tokenPairMonitor.setExplicitRange(beginTokenType, initialExplicitRange);
                return stepOut(node, context, ParseResultType.PARTIAL_MATCH, matchedTokens);
            }
        }

        return stepOut(node, context, ParseResultType.NO_MATCH, matchedTokens);
    }

    private static boolean isParentWrapping(ParserNode node, TokenType tokenType) {
        ParserNode parent = (ParserNode) node.parent;
        while (parent != null && parent.cursorPosition == 0) {
            WrappingDefinition parentWrapping = parent.element.wrapping;
            if (parentWrapping != null && parentWrapping.beginElementType.tokenType == tokenType) {
                return true;
            }
            parent = (ParserNode) parent.parent;
        }
        return false;
    }
}