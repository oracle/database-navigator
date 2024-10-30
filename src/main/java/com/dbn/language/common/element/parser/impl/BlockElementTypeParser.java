package com.dbn.language.common.element.parser.impl;

import com.dbn.language.common.ParseException;
import com.dbn.language.common.element.impl.BlockElementType;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParseResultType;
import com.dbn.language.common.element.parser.ParserBuilder;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.ParserNode;
import com.intellij.lang.PsiBuilder.Marker;

public class BlockElementTypeParser extends SequenceElementTypeParser<BlockElementType>{
    public BlockElementTypeParser(BlockElementType elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) throws ParseException {
        ParserBuilder builder = context.builder;
        Marker marker = builder.mark();
        ParseResult result = super.parse(parentNode, context);

        if (result.getType() == ParseResultType.NO_MATCH) {
            builder.markerDrop(marker);
        } else {
            builder.markerDone(marker, elementType);
        }
        return result;
    }
}