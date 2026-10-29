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

import com.dbn.language.common.element.cache.ElementLookupContext;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.QualifiedIdentifierElementType;
import com.intellij.lang.ASTNode;

import java.util.List;
import java.util.Set;

/**
 * Matches the parsed identifier prefix against grammar variants to offer a separator or next component.
 * A complete matching variant permits ascent. Identifier components are matched structurally here;
 * resolving their database-object meaning remains the completion provider's responsibility.
 */
final class QualifiedIdentifierNextLeafResolver implements ElementNextLeafResolver<QualifiedIdentifierElementType> {
    @Override
    public Step resolve(QualifiedIdentifierElementType element, ASTNode source, ElementLookupContext context, Set<LeafElementType> leafs) {
        if (element.wrapping != null && source.getElementType() == element.wrapping.beginElement) {
            element.cache.captureFirstPossibleLeafs(context, leafs);
            return Step.STOP;
        }

        List<LeafElementType> prefix = NextLeafAstUtil.leafPrefix(source);
        if (element.wrapping != null && !prefix.isEmpty() && prefix.get(0) == element.wrapping.beginElement) {
            prefix = prefix.subList(1, prefix.size());
        }
        if (prefix.isEmpty()) return Step.ABORT;

        boolean matched = false;
        boolean complete = false;
        int index = prefix.size() / 2;
        for (LeafElementType[] variant : element.variants) {
            if (!matchesPrefix(element, variant, prefix)) continue;
            matched = true;
            if (prefix.get(prefix.size() - 1) == element.separatorToken) {
                leafs.add(variant[index]);
            } else if (index < variant.length - 1) {
                leafs.add(element.separatorToken);
            } else {
                complete = true;
            }
        }
        if (!matched) return Step.ABORT;
        return complete ? Step.ASCEND : Step.STOP;
    }

    private boolean matchesPrefix(QualifiedIdentifierElementType element, LeafElementType[] variant, List<LeafElementType> prefix) {
        if (prefix.size() > variant.length * 2 - 1) return false;

        for (int i = 0; i < prefix.size(); i++) {
            LeafElementType actual = prefix.get(i);
            if (i % 2 == 1) {
                if (actual != element.separatorToken) return false;
            } else {
                LeafElementType expected = variant[i / 2];
                // Semantic object resolution belongs to the completion provider.
                if (!(actual.isIdentifier() && expected.isIdentifier()) && !actual.isSameAs(expected)) return false;
            }
        }
        return true;
    }
}
