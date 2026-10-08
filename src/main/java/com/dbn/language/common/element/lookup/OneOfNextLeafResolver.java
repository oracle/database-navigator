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
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.OneOfElementType;
import com.intellij.lang.ASTNode;

import java.util.Set;

/**
 * Offers eligible alternatives after a choice's opening wrapper leaf.
 * After a parsed alternative completes, allows ascent without restarting the other alternatives.
 */
final class OneOfNextLeafResolver implements ElementNextLeafResolver<OneOfElementType> {
    @Override
    public Step resolve(OneOfElementType element, ASTNode source, ElementLookupContext context, Set<LeafElementType> leafs) {
        if (element.wrapping != null && source.getElementType() == element.wrapping.beginElement) {
            for (ElementTypeRef child : element.children) {
                if (context.check(child)) child.elementType.cache.captureFirstPossibleLeafs(context, leafs);
            }
            return Step.STOP;
        }

        for (ElementTypeRef child : element.children) {
            if (child.elementType == source.getElementType()) return Step.ASCEND;
        }
        return Step.ABORT;
    }
}
