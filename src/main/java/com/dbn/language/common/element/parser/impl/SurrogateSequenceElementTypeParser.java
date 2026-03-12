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
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.SurrogateSequenceElementType;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.parser.TokenMonitor;
import com.dbn.language.common.element.path.ParserNode;
import com.intellij.lang.PsiBuilder;

import static com.dbn.language.common.element.parser.ParseResultType.FULL_MATCH;
import static com.dbn.language.common.element.parser.ParseResultType.NO_MATCH;
import static com.dbn.language.common.element.parser.ParseResultType.PARTIAL_MATCH;

public class SurrogateSequenceElementTypeParser extends SequenceElementTypeParser<SurrogateSequenceElementType>{
    public SurrogateSequenceElementTypeParser(SurrogateSequenceElementType elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) throws ParseException {
        ElementTypeBase leadingElement = elementType.getLeadingElementType();
        ParserNode node = null;
        if (shouldParseElement(leadingElement, parentNode, context)) {
            node = stepIn(parentNode, context);

            ParseResult result = leadingElement.parser.parse(node, context);

            if (result.type != NO_MATCH) {
                node.matchedTokens += result.matchedTokens;
                node.matchedElements++;

                TokenMonitor tokenMonitor = context.builder.tokenMonitor;
                LeafElementType surrogateLeaf = tokenMonitor.lastLeaf;

                tokenMonitor.enterSurrogateSection(surrogateLeaf);

                ElementTypeBase mainElement = elementType.getMainElementType();
                result = mainElement.parser.parse(node, context);

                tokenMonitor.exitSurrogateSection(surrogateLeaf);
                if (result.type != NO_MATCH) {
                    node.matchedTokens += result.matchedTokens;
                    node.matchedElements++;
                    return stepOut(node, context, FULL_MATCH);
                }

                // consume leading element as partial match if encountered as first in the chain
                if (isFirstLeadingElement(node)) {
                    PsiBuilder.Marker marker = context.builder.markAndAdvance();
                    marker.done(leadingElement);
                    return stepOut(node, context, PARTIAL_MATCH);
                }
            }
        }

        return stepOut(node, context, NO_MATCH);
    }

    private boolean isFirstLeadingElement(ParserNode node) {
        ParserNode parentNode = (ParserNode) node.parent;
        while (parentNode != null && parentNode.startOffset == node.startOffset) {
            if (parentNode.element instanceof SurrogateSequenceElementType) return false;
            parentNode = (ParserNode) parentNode.parent;
        }

        return true;
    }
}