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
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.extension.OneOfElementTypeExtension;
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.OneOfElementType;
import com.dbn.language.common.element.parser.ElementTypeParser;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.ParserNode;

import static com.dbn.language.common.element.parser.ParseResult.NO_MATCH_RESULT;
import static com.dbn.language.common.element.parser.ParseResultType.NO_MATCH;

public class OneOfElementTypeParser<E extends OneOfElementType> extends ElementTypeParser<E> {

    public OneOfElementTypeParser(E elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) throws ParseException {
        TokenType token = context.builder.getToken();
        if (token == null) return NO_MATCH_RESULT;

        ParserNode node = stepIn(parentNode, context);

        ElementTypeRef[] candidates = parseCandidates(context);
        for (ElementTypeRef candidate : candidates) {
            if (!context.check(candidate)) continue;
            if (!shouldParseElement(candidate.elementType, node, context)) continue;

            ParseResult result = candidate.elementType.parser.parse(node, context);
            if (result.type == NO_MATCH) continue;

            node.matchedTokens = result.matchedTokens;
            return stepOut(node, context, result.type);
        }
        return stepOut(node, context, NO_MATCH);
    }

    private ElementTypeRef[] parseCandidates(ParserContext context) {
        OneOfElementTypeExtension extension = elementType.extension;
        return extension == null ?
                elementType.children :
                extension.parseCandidates(context);
    }
}
