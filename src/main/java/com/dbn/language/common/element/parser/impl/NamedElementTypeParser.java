package com.dbn.language.common.element.parser.impl;

import com.dbn.language.common.ParseException;
import com.dbn.language.common.element.impl.NamedElementType;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParserBuilder;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.ParserNode;

public class NamedElementTypeParser extends SequenceElementTypeParser<NamedElementType>{
    public NamedElementTypeParser(NamedElementType elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) throws ParseException {
        ParserBuilder builder = context.builder;
        if (isRecursive(parentNode, builder.getOffset())) {
            return ParseResult.noMatch();
        }
        return super.parse(parentNode, context);
    }

    protected boolean isRecursive(ParserNode parseNode, int builderOffset){
        // allow 2 levels of recursivity
        boolean recursive = false;
        while (parseNode != null) {
            if (parseNode.element == elementType &&
                    parseNode.startOffset == builderOffset) {
                if (recursive) {
                    return true;
                } else {
                    recursive = true;
                }
            }
            parseNode = (ParserNode) parseNode.parent;
        }
        return false;
    }
}