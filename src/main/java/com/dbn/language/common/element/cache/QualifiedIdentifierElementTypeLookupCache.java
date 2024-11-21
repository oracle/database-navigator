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
import com.dbn.language.common.element.impl.QualifiedIdentifierElementType;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class QualifiedIdentifierElementTypeLookupCache extends ElementTypeLookupCacheIndexed<QualifiedIdentifierElementType> {
    public QualifiedIdentifierElementTypeLookupCache(QualifiedIdentifierElementType elementType) {
        super(elementType);
    }

    @Override
    boolean initAsFirstPossibleLeaf(LeafElementType leaf, ElementTypeBase source) {
        for (LeafElementType[] variant : elementType.getVariants()) {
            if (variant[0] == source) return true;
        }
        return false;
    }

    @Override
    boolean initAsFirstRequiredLeaf(LeafElementType leaf, ElementTypeBase source) {
        for (LeafElementType[] variant : elementType.getVariants()) {
            if (variant[0] == source && !variant[0].optional) return true;
        }
        return false;
    }

    @Override
    public boolean checkStartsWithIdentifier() {
        for (LeafElementType[] elementTypes : elementType.getVariants()) {
            if (elementTypes[0].cache.startsWithIdentifier()) return true;
        }
        return false;
    }

    @Override
    public Set<LeafElementType> captureFirstPossibleLeafs(ElementLookupContext context, @Nullable Set<LeafElementType> bucket) {
        bucket = initBucket(bucket);
        for (LeafElementType[] elementTypes : elementType.getVariants()) {
            // variants already consider optional leafs
            bucket.add(elementTypes[0]);
        }

        return bucket;
    }

    @Override
    public Set<TokenType> captureFirstPossibleTokens(ElementLookupContext context, @Nullable Set<TokenType> bucket) {
        bucket = initBucket(bucket);
        for (LeafElementType[] elementTypes : elementType.getVariants()) {
            // variants already consider optional leafs
            bucket.add(elementTypes[0].tokenType);
        }

        return bucket;
    }
}