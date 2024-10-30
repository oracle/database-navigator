package com.dbn.language.common.element.parser.impl;

import com.dbn.language.common.ParseException;
import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.QualifiedIdentifierElementType;
import com.dbn.language.common.element.impl.QualifiedIdentifierVariant;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.parser.ElementTypeParser;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParseResultType;
import com.dbn.language.common.element.parser.ParserBuilder;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.ParserNode;
import com.dbn.language.common.element.util.ParseBuilderErrorHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class QualifiedIdentifierElementTypeParser extends ElementTypeParser<QualifiedIdentifierElementType> {
    public QualifiedIdentifierElementTypeParser(QualifiedIdentifierElementType elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) throws ParseException {
        ParserBuilder builder = context.builder;
        ParserNode node = stepIn(parentNode, context);

        TokenElementType separatorToken = elementType.getSeparatorToken();
        int matchedTokens = 0;

        QualifiedIdentifierVariant variant = getMostProbableParseVariant(builder);
        if (variant != null) {
            LeafElementType[] elementTypes = variant.getLeafs();

            for (LeafElementType elementType : elementTypes) {
                ParseResult result = elementType.parser.parse(node, context);
                if (result.isNoMatch()) break;  else matchedTokens = matchedTokens + result.getMatchedTokens();

                if (elementType != elementTypes[elementTypes.length -1])  {
                    result = separatorToken.parser.parse(node, context);
                    if (result.isNoMatch()) break; else matchedTokens = matchedTokens + result.getMatchedTokens();
                }
                node.incrementIndex(builder.getOffset());
            }

            if (matchedTokens > 0) {
                if (variant.isIncomplete()) {
                    Set<TokenType> expected = Collections.singleton(separatorToken.tokenType);
                    ParseBuilderErrorHandler.updateBuilderError(expected, context);
                    return stepOut(node, context, ParseResultType.PARTIAL_MATCH, matchedTokens);
                } else {
                    return stepOut(node, context, ParseResultType.FULL_MATCH, matchedTokens);
                }
            }
        }

        return stepOut(node, context, ParseResultType.NO_MATCH, matchedTokens);
    }

    private QualifiedIdentifierVariant getMostProbableParseVariant(ParserBuilder builder) {
        TokenType separatorToken = elementType.getSeparatorToken().tokenType;
        SharedTokenTypeBundle sharedTokenTypes = getSharedTokenTypes();
        TokenType identifier = sharedTokenTypes.getIdentifier();


        List<TokenType> chan = new ArrayList<>();
        int offset = 0;
        boolean wasSeparator = true;
        TokenType tokenType = builder.lookAhead(offset);
        while (tokenType != null) {
            if (tokenType == separatorToken) {
                if (wasSeparator) chan.add(identifier);
                wasSeparator = true;
            } else {
                if (wasSeparator) {
                    if (tokenType.isIdentifier() ||  elementType.cache.containsToken(tokenType))
                        chan.add(tokenType); else
                        chan.add(identifier);
                } else {
                   break;
                }
                wasSeparator = false;
            }
            offset++;
            tokenType = builder.lookAhead(offset);
            if (tokenType == null && wasSeparator) chan.add(identifier);
        }

        QualifiedIdentifierVariant mostProbableVariant = null;

        for (LeafElementType[] elementTypes : elementType.getVariants()) {
            if (elementTypes.length <= chan.size()) {
                int matchedTokens = 0;
                for (int i=0; i<elementTypes.length; i++) {
                    if (elementTypes[i].tokenType.matches(chan.get(i))) {
                        matchedTokens++;
                    }
                }
                if (mostProbableVariant == null || mostProbableVariant.getMatchedTokens() < matchedTokens) {
                    mostProbableVariant = mostProbableVariant == null ?
                            new QualifiedIdentifierVariant(elementTypes, matchedTokens) :
                            mostProbableVariant.replace(elementTypes, matchedTokens);
                }

            }
        }

        return mostProbableVariant;
    }
}