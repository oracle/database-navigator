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
import com.dbn.language.common.element.impl.WrapperElementType;
import com.intellij.lang.ASTNode;

import java.util.Set;

/**
 * Resolves transitions from a wrapper's opening leaf through its contents to its closing leaf.
 * Optional contents permit immediate closure; only the wrapper's own closing leaf allows ascent.
 */
final class WrapperNextLeafResolver implements ElementNextLeafResolver<WrapperElementType> {
    @Override
    public Step resolve(WrapperElementType element, ASTNode source, ElementLookupContext context, Set<LeafElementType> leafs) {
        if (source.getElementType() == element.getEndTokenElement()) return Step.ASCEND;

        if (source.getElementType() == element.getBeginTokenElement()) {
            element.wrappedElement.cache.captureFirstPossibleLeafs(context, leafs);
            if (element.wrappedElementOptional) leafs.add(element.getEndTokenElement());
            return Step.STOP;
        }
        if (source.getElementType() != element.wrappedElement) return Step.ABORT;

        leafs.add(element.getEndTokenElement());
        return Step.STOP;
    }
}
