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
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.SequenceElementType;
import com.dbn.language.common.element.parser.ElementTypeParser;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParseResultType;
import com.dbn.language.common.element.parser.ParserBuilder;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.LanguageNode;
import com.dbn.language.common.element.path.ParserNode;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.element.util.ParseBuilderErrorHandler;
import com.intellij.lang.PsiBuilder;

import java.util.Set;

import static com.dbn.language.common.element.parser.ParseResult.NO_MATCH_RESULT;
import static com.dbn.language.common.element.parser.ParseResultType.FULL_MATCH;
import static com.dbn.language.common.element.parser.ParseResultType.NO_MATCH;
import static com.dbn.language.common.element.parser.ParseResultType.PARTIAL_MATCH;
import static com.dbn.nls.NlsResources.txt;

public class SequenceElementTypeParser<E extends SequenceElementType> extends ElementTypeParser<E> {
    public SequenceElementTypeParser(E elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) throws ParseException {
        ParserBuilder builder = context.builder;
        ParserNode node = null;

        if (shouldParseElement(elementType, parentNode, context)) {
            node = stepIn(parentNode, context);

            ElementTypeRef[] elements = elementType.children;
            while (node.elementIndex < elements.length) {
                int index = node.elementIndex;
                ElementTypeRef element = elements[index];

                // end of document / language switch
                TokenType token = builder.getToken();
                if (token == null) {

                    if (elementType.isAtomic() || element.isFirst() || elementType.isExitIndex(index)) {
                        return stepOut(node, context, NO_MATCH);
                    }

                    if (element.optional && element.isOptionalFromHere()) {
                        return stepOut(node, context, FULL_MATCH);
                    }

                    return stepOut(node, context, PARTIAL_MATCH);
                }

                if (context.check(element)) {
                    ParseResult result = NO_MATCH_RESULT;
                    if (shouldParseElement(element.elementType, node, context)) {

                        //node = node.createVariant(builder.getCurrentOffset(), i);
                        result = element.elementType.parser.parse(node, context);

                        if (result.type != NO_MATCH) {
                            if (element.branch != null) {
                                context.addBranchMarker(node, element.branch);
                            }
                            node.matchedTokens += result.matchedTokens;
                            node.matchedElements++;
                        }
                    }

                    // not matched and not optional
                    if (result.type == NO_MATCH && !element.optional) {
                        if (elementType.isAtomic() ||
                                element.isFirst() ||
                                node.matchedElements == 0 ||
                                node.matchedTokens == 0 ||
                                elementType.isExitIndex(index) ||
                                isWeakMatch(node)) {
                            return stepOut(node, context, NO_MATCH);
                        }

                        index = advanceLexerToNextLandmark(node, context);

                        if (index <= 0) {
                            // no landmarks found or landmark in parent found
                            return stepOut(node, context, PARTIAL_MATCH);
                        } else {
                            // local landmarks found
                            node.elementIndex = index;
                            continue;
                        }
                    }
                }


                // if is last element
                if (element.isLast()) {
                    //matches == 0 reaches this stage only if all sequence elements are optional
                    ParseResultType resultType = node.matchedElements == 0 ? NO_MATCH : FULL_MATCH;
                    return stepOut(node, context, resultType);
                }
                node.elementIndex++;
                node.currentOffset = builder.getOffset();
            }
        }

        return stepOut(node, context, NO_MATCH);
    }

    private boolean isWeakMatch(ParserNode node) {
        return node.matchedElements < 2 && node.matchedTokens < 3 && node.elementIndex > 1;
    }

    private int advanceLexerToNextLandmark(ParserNode node, ParserContext context) {
        int siblingPosition = node.elementIndex;
        ParserBuilder builder = context.builder;
        PsiBuilder.Marker marker = builder.mark();
        Set<TokenType> possibleTokens = elementType.getFirstPossibleTokensFromIndex(context, siblingPosition);
        ParseBuilderErrorHandler.updateBuilderError(possibleTokens, context);

        TokenType tokenType = builder.getToken();
        siblingPosition++;
        while (tokenType != null) {
            int newIndex = getLandmarkIndex(tokenType, siblingPosition, node);

            // no landmark hit -> spool the builder
            if (newIndex == 0) {
                builder.advance();
                tokenType = builder.getToken();
            } else {
                builder.markerDone(marker, elementType.bundle.getUnknownElementType());
                //marker.error("Invalid or incomplete statement");
                return newIndex;
            }
        }
        //builder.markerDone(marker, getElementBundle().getUnknownElementType());
        marker.error(txt("msg.languageParser.error.InvalidOrIncompleteStatement"));
        return 0;
    }

    private int getLandmarkIndex(TokenType tokenType, int index, ParserNode node) {
        if (tokenType.isParserLandmark()) {
            LanguageNode statementPathNode = node.getParent(ElementTypeAttribute.STATEMENT);
            if (statementPathNode != null && statementPathNode.getElement().cache.couldStartWithToken(tokenType)) {
                return -1;
            }
            ElementTypeRef[] children = elementType.children;
            for (int i=index; i< children.length; i++) {
                // check children landmarks
                if (children[i].elementType.cache.couldStartWithToken(tokenType)) {
                    return i;
                }
            }

            ParserNode parseNode = node;
            while (parseNode != null) {
                ElementType elementType = parseNode.element;
                if (elementType instanceof SequenceElementType sequenceElementType) {
                    if ( sequenceElementType.containsLandmarkTokenFromIndex(tokenType, parseNode.elementIndex + 1)) {
                        return -1;
                    }
                } else  if (elementType instanceof IterationElementType iterationElementType) {
                    if (iterationElementType.isSeparator(tokenType)) {
                        return -1;
                    }
                }
                parseNode = (ParserNode) parseNode.parent;
            }
        }
        return 0;
    }
}
