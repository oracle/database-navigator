package com.dbn.language.common.element.parser.impl;

import com.dbn.common.util.Strings;
import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.SimpleTokenType;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.parser.ElementTypeParser;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParseResultType;
import com.dbn.language.common.element.parser.ParserBuilder;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.ParserNode;
import com.intellij.lang.PsiBuilder.Marker;

public class TokenElementTypeParser extends ElementTypeParser<TokenElementType> {
    public TokenElementTypeParser(TokenElementType elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) {
        ParserBuilder builder = context.builder;
        Marker marker = null;

        TokenType token = builder.getToken();
        if (token == elementType.tokenType || builder.isDummyToken()) {

            String text = elementType.getText();
            if (Strings.isNotEmpty(text) && Strings.equalsIgnoreCase(builder.getTokenText(), text)) {
                marker = builder.markAndAdvance();
                return stepOut(marker, context, ParseResultType.FULL_MATCH, 1);
            }

            SharedTokenTypeBundle sharedTokenTypes = getElementBundle().getTokenTypeBundle().getSharedTokenTypes();
            SimpleTokenType leftParenthesis = sharedTokenTypes.getChrLeftParenthesis();
            SimpleTokenType dot = sharedTokenTypes.getChrDot();

            if (token.isSuppressibleReservedWord()) {
                TokenType nextTokenType = builder.getNextToken();
                if (nextTokenType == dot && !elementType.isNextPossibleToken(dot, parentNode, context)) {
                    context.setWavedTokenType(token);
                    return stepOut(marker, context, ParseResultType.NO_MATCH, 0);
                }
                if (token.isFunction() && elementType.getFlavor() == null) {
                    if (nextTokenType != leftParenthesis && elementType.isNextRequiredToken(leftParenthesis, parentNode, context)) {
                        context.setWavedTokenType(token);
                        return stepOut(marker, context, ParseResultType.NO_MATCH, 0);
                    }
                }
            }

            marker = builder.markAndAdvance();
            return stepOut(marker, context, ParseResultType.FULL_MATCH, 1);
        }
        return stepOut(marker, context, ParseResultType.NO_MATCH, 0);
    }
}
