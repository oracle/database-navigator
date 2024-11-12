package com.dbn.language.common.element.parser.impl;

import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.IdentifierElementType;
import com.dbn.language.common.element.parser.ElementTypeParser;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParseResultType;
import com.dbn.language.common.element.parser.ParserBuilder;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.ParserNode;
import com.intellij.lang.PsiBuilder.Marker;

public class IdentifierElementTypeParser extends ElementTypeParser<IdentifierElementType> {
    public IdentifierElementTypeParser(IdentifierElementType elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) {
        ParserBuilder builder = context.builder;
        TokenType token = builder.getToken();
        Marker marker = null;

        if (token != null && !token.isChameleon()){
            if (token.isIdentifier()) {
                marker = builder.markAndAdvance();
                return stepOut(marker, context, ParseResultType.FULL_MATCH, 1);
            }
            else if (isSuppressibleReservedWord(parentNode, context, token)) {
                marker = builder.markAndAdvance();
                return stepOut(marker, context, ParseResultType.FULL_MATCH, 1);
            }
        }
        return stepOut(marker, context, ParseResultType.NO_MATCH, 0);
    }

    private boolean isSuppressibleReservedWord(ParserNode parentNode, ParserContext context, TokenType tokenType) {
        if (tokenType.isSuppressibleReservedWord()) {
            if (context.isWavedTokenType(tokenType)) {
                return true;
            }

            return (elementType.isDefinition() && !elementType.isAlias()) || isSuppressibleReservedWord(tokenType, parentNode, context);
        }
        return false;
    }
}