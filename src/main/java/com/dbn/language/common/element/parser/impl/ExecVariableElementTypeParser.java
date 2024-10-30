package com.dbn.language.common.element.parser.impl;

import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.ExecVariableElementType;
import com.dbn.language.common.element.parser.ElementTypeParser;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParseResultType;
import com.dbn.language.common.element.parser.ParserBuilder;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.ParserNode;
import com.intellij.lang.PsiBuilder.Marker;

public class ExecVariableElementTypeParser extends ElementTypeParser<ExecVariableElementType> {
    public ExecVariableElementTypeParser(ExecVariableElementType elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) {
        ParserBuilder builder = context.builder;
        TokenType token = builder.getToken();
        Marker marker = null;

        if (token != null && !token.isChameleon()){
            if (token.isVariable()) {
                marker = builder.markAndAdvance();
                return stepOut(marker, context, ParseResultType.FULL_MATCH, 1);
            }
        }
        return stepOut(marker, context, ParseResultType.NO_MATCH, 0);
    }

}
