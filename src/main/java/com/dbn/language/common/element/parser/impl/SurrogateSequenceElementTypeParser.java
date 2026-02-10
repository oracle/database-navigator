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
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.SurrogateSequenceElementType;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.ParserNode;

import static com.dbn.language.common.element.parser.ParseResultType.FULL_MATCH;
import static com.dbn.language.common.element.parser.ParseResultType.NO_MATCH;

public class SurrogateSequenceElementTypeParser extends SequenceElementTypeParser<SurrogateSequenceElementType>{
    public SurrogateSequenceElementTypeParser(SurrogateSequenceElementType elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) throws ParseException {
        // TODO JDBC-5173
        if (true) return super.parse(parentNode, context);

        ParserNode node = stepIn(parentNode, context);
        if (shouldParseElement(elementType, node, context)) {

            // leading element
            ElementTypeBase leadingElement = elementType.getLeadingElementType();
            if (shouldParseElement(leadingElement, node, context)) {
                ParseResult result = leadingElement.parser.parse(node, context);
                if (result.isMatch()) {
                    node.matchedTokens += result.matchedTokens;
                    node.matchedElements++;
                } else {
                    return stepOut(node, context, NO_MATCH);
                }
            } else {
                return stepOut(node, context, NO_MATCH);
            }

            ElementTypeBase mainElement = elementType.getMainElementType();
            if (shouldParseElement(mainElement, node, context)) {
                ParseResult result = mainElement.parser.parse(node, context);
                if (result.isMatch()) {
                    node.matchedTokens += result.matchedTokens;
                    node.matchedElements++;
                } else {
                    return stepOut(node, context, NO_MATCH);
                }
            } else {
                return stepOut(node, context, NO_MATCH);
            }
            stepOut(node, context, FULL_MATCH);
        }

        return stepOut(node, context, NO_MATCH);
    }


}