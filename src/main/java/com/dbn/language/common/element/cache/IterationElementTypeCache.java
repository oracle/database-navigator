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
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.WrappingDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class IterationElementTypeCache extends ElementTypeCacheBase<IterationElementType> {
    public IterationElementTypeCache(IterationElementType elementType) {
        super(elementType);
    }

    @Override
    public boolean containsToken(TokenType tokenType) {
        return elementType.isSeparator(tokenType) ||
                elementType.isWrappingBegin(tokenType) ||
                elementType.isWrappingEnd(tokenType) ||
                elementType.iteratedElement.cache.containsToken(tokenType);
    }

    @Override
    public Set<TokenType> getFirstPossibleTokens() {
        return elementType.iteratedElement.cache.getFirstPossibleTokens();
    }

    @Override
    public Set<TokenType> getFirstRequiredTokens() {
        return elementType.iteratedElement.cache.getFirstRequiredTokens();
    }

    @Override
    public boolean couldStartWithLeaf(LeafElementType leafElementType) {
        if (elementType.isWrappingBegin(leafElementType)) return true;
        if (elementType.iteratedElement.cache.couldStartWithLeaf(leafElementType)) return true;
        return false;
    }

    @Override
    public boolean shouldStartWithLeaf(LeafElementType leafElementType) {
        return this.elementType.iteratedElement.cache.shouldStartWithLeaf(leafElementType);
    }


    @Override
    public boolean couldStartWithToken(TokenType tokenType) {
        return elementType.isWrappingBegin(tokenType) ||
                elementType.iteratedElement.cache.couldStartWithToken(tokenType);
    }

    @Override
    public Set<LeafElementType> getFirstPossibleLeafs() {
        Set<LeafElementType> firstPossibleLeafs = initBucket(null);
        firstPossibleLeafs.addAll(elementType.iteratedElement.cache.getFirstPossibleLeafs());
        WrappingDefinition wrapping = elementType.wrapping;
        if (wrapping != null) {
            firstPossibleLeafs.add(wrapping.beginElement);
        }
        return firstPossibleLeafs;
    }

    @Override
    public Set<TokenType> getAllPossibleTokens() {
        return elementType.iteratedElement.cache.getAllPossibleTokens();
    }

    @Override
    public Set<LeafElementType> getFirstRequiredLeafs() {
        return elementType.iteratedElement.cache.getFirstRequiredLeafs();
    }

    @Override
    public boolean startsWith(TokenTypeCategory typeCategory) {
        return elementType.iteratedElement.cache.startsWith(typeCategory);
    }

    @Override
    public boolean isFirstPossibleToken(TokenType tokenType) {
        return elementType.iteratedElement.cache.isFirstPossibleToken(tokenType) || elementType.isWrappingBegin(tokenType);
    }

    @Override
    public boolean isFirstRequiredToken(TokenType tokenType) {
        return elementType.iteratedElement.cache.isFirstRequiredToken(tokenType);
    }

    @Override
    public Set<LeafElementType> captureFirstPossibleLeafs(ElementLookupContext context, @Nullable Set<LeafElementType> bucket) {
        bucket = super.captureFirstPossibleLeafs(context, bucket);
        return elementType.iteratedElement.cache.captureFirstPossibleLeafs(context, bucket);
    }

    @Override
    public Set<TokenType> captureFirstPossibleTokens(ElementLookupContext context, @Nullable Set<TokenType> bucket) {
        bucket = super.captureFirstPossibleTokens(context, bucket);
        return elementType.iteratedElement.cache.captureFirstPossibleTokens(context, bucket);
    }

    @Override
    public Set<LeafElementType> captureSurrogateSuccessors(LeafElementType surrogateLead, Set<LeafElementType> bucket) {
        return elementType.iteratedElement.cache.captureSurrogateSuccessors(surrogateLead, bucket);
    }
}
