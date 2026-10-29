/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.language.common.element.lookup;

import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.cache.ElementTypeCache;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.OneOfElementType;
import com.dbn.language.common.element.impl.QualifiedIdentifierElementType;
import com.dbn.language.common.element.impl.SequenceElementType;
import com.dbn.language.common.element.impl.WrapperElementType;
import com.dbn.language.common.element.path.ParserNode;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves token continuations used to distinguish reserved words from identifiers during parsing.
 * The active {@link ParserNode} chain supplies grammar positions; no completed AST is needed.
 *
 * <p>Both queries use precomputed token indexes and return as soon as the path determines the answer.
 * They do not collect leaves, advance the parser, or change branch markers. The indexes describe
 * the grammar without filtering by active branches or database version.
 */
@UtilityClass
public final class NextTokenResolver {

    /**
     * Checks possible continuations after the construct currently being attempted.
     * Each sequence's {@link ParserNode#elementIndex} identifies its active child, so lookahead
     * starts at the following child and includes optional successors up to the first required one.
     * Enclosing constructs are examined when the local path permits continuation outside them.
     *
     * @param source the grammar leaf used to recognize a qualified-identifier separator boundary
     * @param tokenType the token being checked, matched through grammar token indexes
     * @param pathNode the active parser frame for the attempted construct, or null when no path exists
     * @return true if the parser lookahead rules admit the token; false for an exhausted or absent path
     */
    public static boolean isNextPossibleToken(
            @NotNull LeafElementType source,
            @NotNull TokenType tokenType,
            @Nullable ParserNode pathNode) {
        return isNextToken(source, tokenType, pathNode, false);
    }

    /**
     * Uses the same parser-path walk with required-token indexes. Optional sequence children
     * and alternative starts of an active choice do not establish a required continuation.
     * A required sequence child blocks further lookup if its required-token index does not match.
     *
     * <p>This preserves the parser's required-token lookahead semantics: optional sequence children
     * are skipped even though they could consume tokens before the required continuation.
     *
     * @param source the grammar leaf used to recognize a qualified-identifier separator boundary
     * @param tokenType the token being checked against required-token indexes
     * @param pathNode the active parser frame for the attempted construct, or null when no path exists
     * @return true if the parser's required-token rules match; false for an exhausted or absent path
     */
    public static boolean isNextRequiredToken(
            @NotNull LeafElementType source,
            @NotNull TokenType tokenType,
            @Nullable ParserNode pathNode) {
        return isNextToken(source, tokenType, pathNode, true);
    }

    private static boolean isNextToken(LeafElementType source, TokenType tokenType, ParserNode pathNode, boolean required) {
        for (ParserNode node = pathNode; node != null; node = node.getParent()) {
            ElementTypeBase element = node.element;
            if (element instanceof SequenceElementType sequence) {
                // The active parent cursor distinguishes repeated references to the same grammar element.
                for (int index = node.elementIndex + 1; index < sequence.children.length; index++) {
                    ElementTypeRef child = sequence.children[index];
                    ElementTypeCache<?> cache = child.elementType.cache;
                    if (required ?
                            !child.optional && cache.isFirstRequiredToken(tokenType) :
                            cache.isFirstPossibleToken(tokenType)) return true;
                    if (!child.optional) return false;
                }
            } else if (element instanceof IterationElementType iteration) {
                // A separated iteration cannot start another item directly at this position.
                if (iteration.separatorTokens == null) {
                    ElementTypeCache<?> cache = iteration.iteratedElement.cache;
                    if (required ? cache.isFirstRequiredToken(tokenType) : cache.isFirstPossibleToken(tokenType)) return true;
                }
            } else if (element instanceof QualifiedIdentifierElementType identifier) {
                // A separator commits the parser to the next identifier component.
                if (source == identifier.separatorToken) return false;
            } else if (element instanceof WrapperElementType wrapper) {
                return wrapper.getEndTokenElement().tokenType == tokenType;
            } else if (element instanceof OneOfElementType oneOf && !required) {
                // Other alternatives remain relevant while the parser is attempting this choice.
                if (oneOf.cache.isFirstPossibleToken(tokenType)) return true;
            }
        }
        return false;
    }
}
