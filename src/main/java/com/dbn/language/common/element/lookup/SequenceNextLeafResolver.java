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
import com.dbn.language.common.element.impl.SequenceElementType;
import com.intellij.lang.ASTNode;

import java.util.OptionalInt;
import java.util.Set;

/**
 * Collects starts of eligible sequence children after the source's grammar occurrence.
 * Optional children allow further candidates; the first eligible required child stops ascent.
 */
final class SequenceNextLeafResolver implements ElementNextLeafResolver<SequenceElementType> {
    @Override
    public Step resolve(SequenceElementType element, ASTNode source, ElementLookupContext context, Set<LeafElementType> leafs) {
        int nextIndex = 0;
        if (element.wrapping == null || source.getElementType() != element.wrapping.beginElement) {
            OptionalInt index = NextLeafAstUtil.sequenceIndex(source, element);
            if (index.isEmpty()) return Step.ABORT;
            nextIndex = index.getAsInt() + 1;
        }

        for (int i = nextIndex; i < element.children.length; i++) {
            ElementTypeRef child = element.children[i];
            if (!context.check(child)) continue;

            child.elementType.cache.captureFirstPossibleLeafs(context, leafs);
            if (!child.optional) return Step.STOP;
        }
        return Step.ASCEND;
    }
}
