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

import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.SequenceElementType;
import com.dbn.language.common.element.impl.WrappingDefinition;
import com.intellij.lang.ASTNode;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Interprets the parsed prefix used by leaf completion without changing the AST.
 * Sibling scans stop at the supplied source so later parsed text does not affect its position.
 */
@UtilityClass
final class NextLeafAstUtil {
    /**
     * Checks whether the parent's parsed prefix contains the opening leaf of the given wrapping.
     * The source itself is included; a detached source has no opening leaf in its parent scope.
     */
    static boolean hasWrappingBegin(ASTNode source, WrappingDefinition wrapping) {
        ASTNode parent = source.getTreeParent();
        if (parent == null) return false;

        for (ASTNode child = parent.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            if (child.getElementType() == wrapping.beginElement) return true;
            if (child == source) break;
        }
        return false;
    }

    /**
     * Maps a direct AST child to its occurrence in the parent sequence's grammar.
     * Matching advances through grammar positions rather than physical sibling counts, allowing
     * trivia and omitted optional elements between repeated references to the same element type.
     *
     * @param source the direct child whose grammar position is needed
     * @param sequence the grammar sequence represented by the source's parent
     * @return the zero-based grammar index, or empty if the source has no parent or cannot be mapped
     */
    static OptionalInt sequenceIndex(ASTNode source, SequenceElementType sequence) {
        ASTNode parent = source.getTreeParent();
        if (parent == null) return OptionalInt.empty();

        int fromIndex = 0;
        for (ASTNode child = parent.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            int match = -1;
            if (child.getElementType() instanceof ElementTypeBase) {
                for (int i = fromIndex; i < sequence.children.length; i++) {
                    ElementTypeRef reference = sequence.children[i];
                    if (reference.elementType == child.getElementType()) {
                        match = i;
                        break;
                    }
                }
            }
            if (child == source) return match < 0 ? OptionalInt.empty() : OptionalInt.of(match);
            if (match >= 0) fromIndex = match + 1;
        }
        return OptionalInt.empty();
    }

    /**
     * Collects direct sibling grammar leaves through the source, in parsed order.
     * Trivia and composite siblings are ignored; composite children are not traversed.
     * A source without a parent produces an empty list.
     */
    static List<LeafElementType> leafPrefix(ASTNode source) {
        List<LeafElementType> prefix = new ArrayList<>();
        ASTNode parent = source.getTreeParent();
        if (parent == null) return prefix;

        for (ASTNode child = parent.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            if (child.getElementType() instanceof LeafElementType leaf) prefix.add(leaf);
            if (child == source) break;
        }
        return prefix;
    }

}
