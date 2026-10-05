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

import com.dbn.language.common.element.ChameleonElementType;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.cache.ElementLookupContext;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.NamedElementType;
import com.dbn.language.common.element.impl.OneOfElementType;
import com.dbn.language.common.element.impl.QualifiedIdentifierElementType;
import com.dbn.language.common.element.impl.SequenceElementType;
import com.dbn.language.common.element.impl.WrapperElementType;
import com.dbn.language.common.element.impl.WrappingDefinition;
import com.dbn.language.common.element.lookup.ElementNextLeafResolver.Step;
import com.intellij.lang.ASTNode;
import com.intellij.psi.impl.source.tree.FileElement;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.dbn.language.common.element.util.ElementTypeAttribute.STATEMENT;

/**
 * Resolves grammar successors for code completion from an already parsed leaf.
 * Follows the leaf's actual AST ancestry to distinguish occurrences of shared named grammar elements.
 * Construct-specific resolvers collect candidates through first-leaf caches and branch/version checks.
 *
 * <p>Parser-path token queries are handled by {@link NextTokenResolver}. Extension-based completion
 * alternatives and semantic database-object resolution are handled by the completion provider.
 */
@UtilityClass
public final class NextLeafResolver {
    private static final SequenceNextLeafResolver SEQUENCE = new SequenceNextLeafResolver();
    private static final IterationNextLeafResolver ITERATION = new IterationNextLeafResolver();
    private static final OneOfNextLeafResolver ONE_OF = new OneOfNextLeafResolver();
    private static final WrapperNextLeafResolver WRAPPER = new WrapperNextLeafResolver();
    private static final QualifiedIdentifierNextLeafResolver QUALIFIED_IDENTIFIER = new QualifiedIdentifierNextLeafResolver();

    /**
     * Collects grammar leaves that may follow the parsed source, including optional continuations.
     * Traversal may leave completed or skippable constructs, and respects the context's statement
     * boundary setting. Only the parsed prefix through the source determines the current position.
     *
     * <p>Supply a fresh lookup context for each call: ascending out of a named scope removes its
     * branch markers. An unsupported parent or unresolved source position aborts the lookup and
     * discards all candidates collected so far.
     *
     * @param source an AST node whose element type is a grammar leaf
     * @param context branch markers, database version, and traversal boundary settings for this lookup
     * @return distinct grammar leaves in discovery order, or an empty set when none are found or lookup aborts
     * @throws IllegalArgumentException if the source does not have a leaf element type
     */
    public static Set<LeafElementType> nextPossibleLeafs(@NotNull ASTNode source, @NotNull ElementLookupContext context) {
        if (!(source.getElementType() instanceof LeafElementType)) {
            throw new IllegalArgumentException("Expected a parsed grammar leaf");
        }

        Set<LeafElementType> leafs = new LinkedHashSet<>();
        while (true) {
            if (source.getElementType() instanceof ElementType element &&
                    element.is(STATEMENT) && context.isBreakOnAttribute(STATEMENT)) break;

            ASTNode parent = source.getTreeParent();
            if (parent == null || parent instanceof FileElement) break;

            if (parent.getElementType() instanceof ChameleonElementType chameleon) {
                NamedElementType root = chameleon.getParentLanguage().getParserDefinition().getParser().getElementTypes().getRootElementType();
                root.cache.captureFirstPossibleLeafs(context, leafs);
            } else if (parent.getElementType() instanceof ElementTypeBase element) {
                Step step = resolveParent(element, source, context, leafs);
                if (step == Step.ABORT) return Set.of();
                if (step == Step.STOP) break;

                if (element instanceof NamedElementType named) context.removeBranchMarkers(named);
            } else {
                return Set.of();
            }
            source = parent;
        }
        return leafs;
    }

    /**
     * Resolves continuation within one parent and applies its wrapping boundary before allowing ascent.
     * An active wrapper contributes its closing leaf and stops traversal inside the enclosing construct.
     */
    private static Step resolveParent(ElementTypeBase element, ASTNode source, ElementLookupContext context, Set<LeafElementType> leafs) {
        if (element instanceof WrapperElementType wrapper) return WRAPPER.resolve(wrapper, source, context, leafs);

        WrappingDefinition wrapping = element.wrapping;
        if (wrapping != null && source.getElementType() == wrapping.endElement) return Step.ASCEND;

        Step step;
        if (element instanceof SequenceElementType sequence) {
            step = SEQUENCE.resolve(sequence, source, context, leafs);
        } else if (element instanceof IterationElementType iteration) {
            step = ITERATION.resolve(iteration, source, context, leafs);
        } else if (element instanceof OneOfElementType oneOf) {
            step = ONE_OF.resolve(oneOf, source, context, leafs);
        } else if (element instanceof QualifiedIdentifierElementType identifier) {
            step = QUALIFIED_IDENTIFIER.resolve(identifier, source, context, leafs);
        } else {
            return Step.ABORT;
        }

        if (step == Step.ASCEND && wrapping != null &&
                (!wrapping.optional || NextLeafAstUtil.hasWrappingBegin(source, wrapping))) {
            leafs.add(wrapping.endElement);
            return Step.STOP;
        }
        return step;
    }
}
