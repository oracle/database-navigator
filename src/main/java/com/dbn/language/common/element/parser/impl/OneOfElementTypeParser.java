package com.dbn.language.common.element.parser.impl;

import com.dbn.language.common.ParseException;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.OneOfElementType;
import com.dbn.language.common.element.parser.ElementTypeParser;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParseResultType;
import com.dbn.language.common.element.parser.ParserBuilder;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.ParserNode;

public class OneOfElementTypeParser extends ElementTypeParser<OneOfElementType> {

    public OneOfElementTypeParser(OneOfElementType elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) throws ParseException {
        ParserBuilder builder = context.builder;
        ParserNode node = stepIn(parentNode, context);

        elementType.sort();
        TokenType token = builder.getToken();

        if (token != null && !token.isChameleon()) {
            ElementTypeRef element = elementType.getFirstChild();
            while (element != null) {
                if (context.check(element) && shouldParseElement(element.elementType, node, context)) {
                    ParseResult result = element.elementType.parser.parse(node, context);

                    if (result.isMatch()) {
                        return stepOut(node, context, result.getType(), result.getMatchedTokens());
                    }
                }
                element = element.getNext();
            }
        }
        return stepOut(node, context, ParseResultType.NO_MATCH, 0);
    }
}