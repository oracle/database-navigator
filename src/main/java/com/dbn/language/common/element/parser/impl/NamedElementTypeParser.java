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