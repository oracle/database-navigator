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
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

public abstract class LeafElementTypeLookupCache<T extends LeafElementType> extends ElementTypeLookupCache<T> {
    public LeafElementTypeLookupCache(T elementType) {
        super(elementType);
    }

    @Override
    @Deprecated
    public boolean couldStartWithLeaf(LeafElementType elementType) {
        return this.elementType == elementType;
    }

    @Override
    public boolean shouldStartWithLeaf(LeafElementType elementType) {
        return this.elementType == elementType;
    }

    @Override
    public Set<LeafElementType> getFirstPossibleLeafs() {
        return Collections.singleton(elementType);
    }

    @Override
    public Set<LeafElementType> getFirstRequiredLeafs() {
        return getFirstPossibleLeafs();
    }

    @Override
    public boolean isFirstPossibleLeaf(LeafElementType elementType) {
        return this.elementType == elementType;
    }

    @Override
    public boolean isFirstRequiredLeaf(LeafElementType elementType) {
        return isFirstPossibleLeaf(elementType);
    }

    @Override
    public Set<TokenType> getFirstRequiredTokens() {
        return getFirstPossibleTokens();
    }

    @Override
    public boolean couldStartWithToken(TokenType tokenType) {
        return elementType.tokenType == tokenType;
    }

    @Override
    public Set<LeafElementType> captureFirstPossibleLeafs(ElementLookupContext context, @Nullable Set<LeafElementType> bucket) {
        bucket = initBucket(bucket);
        bucket.add(elementType);
        return bucket;
    }

    @Override
    public Set<TokenType> captureFirstPossibleTokens(ElementLookupContext context, @Nullable Set<TokenType> bucket) {
        bucket = initBucket(bucket);
        captureFirstPossibleTokens(bucket);
        return bucket;
    }

    @Override
    public boolean containsLeaf(LeafElementType elementType) {
        return this.elementType == elementType;
    }

    @Override
    public void registerLeaf(LeafElementType leaf, ElementTypeBase source) {}
}