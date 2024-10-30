package com.dbn.language.common.element.parser.impl;

import com.dbn.language.common.ParseException;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.BasicElementType;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.SequenceElementType;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.parser.ElementTypeParser;
import com.dbn.language.common.element.parser.ParseResult;
import com.dbn.language.common.element.parser.ParseResultType;
import com.dbn.language.common.element.parser.ParserBuilder;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.ParserNode;
import com.dbn.language.common.element.util.ParseBuilderErrorHandler;
import com.intellij.lang.PsiBuilder;

import java.util.Set;

public class IterationElementTypeParser extends ElementTypeParser<IterationElementType> {
    public IterationElementTypeParser(IterationElementType elementType) {
        super(elementType);
    }

    @Override
    public ParseResult parse(ParserNode parentNode, ParserContext context) throws ParseException {
        ParserBuilder builder = context.builder;
        ParserNode node = stepIn(parentNode, context);

        ElementTypeBase iteratedElementType = elementType.iteratedElementType;
        TokenElementType[] separatorTokens = elementType.separatorTokens;

        int iterations = 0;
        int matchedTokens = 0;

        //if (shouldParseElement(iteratedElementType, node, context)) {
            ParseResult result = iteratedElementType.parser.parse(node, context);

            // check first iteration element
            if (result.isMatch()) {
                if (node.isRecursive(node.startOffset)) {
                    ParseResultType resultType = matchesMinIterations(iterations) ? result.getType() : ParseResultType.NO_MATCH;
                    return stepOut(node, context, resultType, matchedTokens);
                }
                while (true) {
                    iterations++;
                    // check separator
                    // if not matched just step out
                    PsiBuilder.Marker partialMatchMarker = null;
                    if (separatorTokens != null) {
                        if (elementType.isFollowedBySeparator()) {
                            partialMatchMarker = builder.mark();
                        }

                        ParseResult sepResult = ParseResult.noMatch();
                        for (TokenElementType separatorToken : separatorTokens) {
                            sepResult = separatorToken.parser.parse(node, context);
                            matchedTokens = matchedTokens + sepResult.getMatchedTokens();
                            if (sepResult.isMatch()) break;
                        }

                        if (sepResult.isNoMatch()) {
                            // if NO_MATCH, no additional separator found, hence then iteration should exit with MATCH
                            ParseResultType resultType =
                                    matchesMinIterations(iterations) ?
                                            matchesIterations(iterations) ?
                                                    result.getType() :
                                                    ParseResultType.PARTIAL_MATCH :
                                            ParseResultType.NO_MATCH;

                            builder.markerDrop(partialMatchMarker);
                            return stepOut(node, context, resultType, matchedTokens);
                        } else {
                            node.currentOffset = builder.getOffset();
                        }
                    }

                    // check consecutive iterated element
                    // if not matched, step out with error

                    result = iteratedElementType.parser.parse(node, context);

                    if (result.isNoMatch()) {
                        // missing separators permit ending the iteration as valid at any time
                        if (separatorTokens == null) {
                            ParseResultType resultType =
                                    matchesMinIterations(iterations) ?
                                        matchesIterations(iterations) ?
                                            ParseResultType.FULL_MATCH :
                                            ParseResultType.PARTIAL_MATCH :
                                    ParseResultType.NO_MATCH;
                            return stepOut(node, context, resultType, matchedTokens);
                        } else {
                            if (matchesMinIterations(iterations)) {
                                if (elementType.isFollowedBySeparator()) {
                                    builder.markerRollbackTo(partialMatchMarker);
                                    return stepOut(node, context, ParseResultType.FULL_MATCH, matchedTokens);
                                } else {
                                    builder.markerDrop(partialMatchMarker);
                                }

                                boolean exit = advanceLexerToNextLandmark(node, false, context);
                                if (exit){
                                    return stepOut(node, context, ParseResultType.PARTIAL_MATCH, matchedTokens);
                                }
                            } else {
                                builder.markerDrop(partialMatchMarker);
                                return stepOut(node, context, ParseResultType.NO_MATCH, matchedTokens);
                            }
                        }
                    } else {
                        builder.markerDrop(partialMatchMarker);
                        matchedTokens = matchedTokens + result.getMatchedTokens();
                    }
                }
            }
        //}
        return stepOut(node, context, ParseResultType.NO_MATCH, matchedTokens);
    }

    private boolean advanceLexerToNextLandmark(ParserNode parentNode, boolean lenient, ParserContext context) {
        ParserBuilder builder = context.builder;

        PsiBuilder.Marker marker = builder.mark();
        ElementTypeBase iteratedElementType = elementType.iteratedElementType;
        TokenElementType[] separatorTokens = elementType.separatorTokens;

        if (!lenient) {
            Set<TokenType> expectedTokens = iteratedElementType.cache.captureFirstPossibleTokens(context.reset());
            ParseBuilderErrorHandler.updateBuilderError(expectedTokens, context);
        }
        boolean advanced = false;
        BasicElementType unknownElementType = getElementBundle().getUnknownElementType();
        while (!builder.eof()) {
            TokenType token = builder.getToken();
            if (token == null || token.isChameleon())  break;

            if (token.isParserLandmark()) {
                if (separatorTokens != null) {
                    for (TokenElementType separatorToken : separatorTokens) {
                        if (separatorToken.cache.containsToken(token)) {
                            builder.markerDone(marker, unknownElementType);
                            return false;
                        }
                    }
                }

                ParserNode parseNode = parentNode;
                while (parseNode != null) {
                    if (parseNode.element instanceof SequenceElementType) {
                        SequenceElementType sequenceElementType = (SequenceElementType) parseNode.element;
                        int index = parseNode.cursorPosition;
                        if (!iteratedElementType.cache.containsToken(token) && sequenceElementType.containsLandmarkTokenFromIndex(token, index + 1)) {
                            if (advanced || !lenient) {
                                builder.markerDone(marker, unknownElementType);
                            } else {
                                builder.markerRollbackTo(marker);
                            }
                            return true;
                        }

                    }
                    parseNode = (ParserNode) parseNode.parent;
                }
            }
            builder.advance();
            advanced = true;
        }
        if (advanced || !lenient)
            builder.markerDone(marker, unknownElementType); else
            builder.markerRollbackTo(marker);
        return true;
    }

    @Deprecated
    private boolean matchesMinIterations(int iterations) {
        return elementType.minIterations <= iterations;
    }

    @Deprecated
    private boolean matchesIterations(int iterations) {
        int[]elementsCountVariants = elementType.elementsCountVariants;
        if (elementsCountVariants != null) {
            for (int elementsCountVariant: elementsCountVariants) {
                if (elementsCountVariant == iterations) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private boolean matchesIterationConstraints(int iterations) {
        if (elementType.minIterations <= iterations) {
            int[]elementsCountVariants = elementType.elementsCountVariants;
            if (elementsCountVariants != null) {
                for (int elementsCountVariant: elementsCountVariants) {
                    if (elementsCountVariant == iterations) {
                        return true;
                    }
                }
                return false;
            }
        } else {
            return false;
        }

        return true;
    }
}
