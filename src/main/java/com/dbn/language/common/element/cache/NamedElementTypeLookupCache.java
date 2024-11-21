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

package com.dbn.language.common.element.cache;

import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.NamedElementType;

import java.util.Set;

public class NamedElementTypeLookupCache extends SequenceElementTypeLookupCache<NamedElementType>{

    public NamedElementTypeLookupCache(NamedElementType elementType) {
        super(elementType);
    }

    @Override
    protected void registerLeafInParent(LeafElementType leaf) {
        // walk the tree up for all potential parents
        Set<ElementTypeBase> parents = elementType.parents;
        if (parents == null) return;

        for (ElementTypeBase parentElementType: parents) {
            parentElementType.cache.registerLeaf(leaf, elementType);
        }
    }

    @Override
    public Set<LeafElementType> captureFirstPossibleLeafs(ElementLookupContext context, Set<LeafElementType> bucket) {
        if (!context.isScanned(elementType)) {
            context.markScanned(elementType);
            return super.captureFirstPossibleLeafs(context, bucket);
        }
        return bucket;
    }

    @Override
    public Set<TokenType> captureFirstPossibleTokens(ElementLookupContext context, Set<TokenType> bucket) {
        if (!context.isScanned(elementType)) {
            context.markScanned(elementType);
            return super.captureFirstPossibleTokens(context, bucket);
        }
        return bucket;
    }
}
