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
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.OneOfElementType;

import java.util.Set;

public class OneOfElementTypeLookupCache extends ElementTypeLookupCacheIndexed<OneOfElementType> {
    public OneOfElementTypeLookupCache(OneOfElementType elementType) {
        super(elementType);
    }

    @Override
    boolean initAsFirstPossibleLeaf(LeafElementType leaf, ElementTypeBase source) {
        boolean notInitialized = !firstPossibleLeafs.contains(leaf);
        return notInitialized && (isWrapperBeginLeaf(leaf) || source.cache.couldStartWithLeaf(leaf));
    }

    @Override
    boolean initAsFirstRequiredLeaf(LeafElementType leaf, ElementTypeBase source) {
        boolean notInitialized = !firstRequiredLeafs.contains(leaf);
        return notInitialized && source.cache.shouldStartWithLeaf(leaf);
    }

    @Override
    public boolean checkStartsWithIdentifier() {
        for(ElementTypeRef child : elementType.children){
            if (child.elementType.cache.startsWithIdentifier()) return true;
        }
        return false;
    }

    @Override
    public Set<LeafElementType> captureFirstPossibleLeafs(ElementLookupContext context, Set<LeafElementType> bucket) {
        bucket = super.captureFirstPossibleLeafs(context, bucket);
        ElementTypeRef[] elementTypeRefs = elementType.children;
        for (ElementTypeRef child : elementTypeRefs) {
            if (context.check(child)) {
                bucket = child.elementType.cache.captureFirstPossibleLeafs(context, bucket);
            }
        }
        return bucket;
    }

    @Override
    public Set<TokenType> captureFirstPossibleTokens(ElementLookupContext context, Set<TokenType> bucket) {
        bucket = super.captureFirstPossibleTokens(context, bucket);
        ElementTypeRef[] elementTypeRefs = elementType.children;
        for (ElementTypeRef child : elementTypeRefs) {
            if (context.check(child)) {
                bucket = child.elementType.cache.captureFirstPossibleTokens(context, bucket);
            }
        }
        return bucket;
    }
}