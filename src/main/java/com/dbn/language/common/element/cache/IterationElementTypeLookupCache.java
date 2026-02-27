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

public class IterationElementTypeLookupCache extends ElementTypeLookupCacheBase<IterationElementType> {
    public IterationElementTypeLookupCache(IterationElementType elementType) {
        super(elementType);
    }

    @Override
    public boolean containsToken(TokenType tokenType) {
        return element.isSeparator(tokenType) ||
                element.isWrappingBegin(tokenType) ||
                element.isWrappingEnd(tokenType) ||
                element.iteratedElement.cache.containsToken(tokenType);
    }

    @Override
    public Set<TokenType> getFirstPossibleTokens() {
        return element.iteratedElement.cache.getFirstPossibleTokens();
    }

    @Override
    public Set<TokenType> getFirstRequiredTokens() {
        return element.iteratedElement.cache.getFirstRequiredTokens();
    }

    @Override
    public boolean couldStartWithLeaf(LeafElementType leafElementType) {
        if (element.isWrappingBegin(leafElementType)) return true;
        if (element.iteratedElement.cache.couldStartWithLeaf(leafElementType)) return true;
        return false;
    }

    @Override
    public boolean shouldStartWithLeaf(LeafElementType leafElementType) {
        return this.element.iteratedElement.cache.shouldStartWithLeaf(leafElementType);
    }


    @Override
    public boolean couldStartWithToken(TokenType tokenType) {
        return element.isWrappingBegin(tokenType) ||
                element.iteratedElement.cache.couldStartWithToken(tokenType);
    }

    @Override
    public Set<LeafElementType> getFirstPossibleLeafs() {
        Set<LeafElementType> firstPossibleLeafs = initBucket(null);
        firstPossibleLeafs.addAll(element.iteratedElement.cache.getFirstPossibleLeafs());
        WrappingDefinition wrapping = element.wrapping;
        if (wrapping != null) {
            firstPossibleLeafs.add(wrapping.beginElement);
        }
        return firstPossibleLeafs;
    }

    @Override
    public Set<TokenType> getAllPossibleTokens() {
        return element.iteratedElement.cache.getAllPossibleTokens();
    }

    @Override
    public Set<LeafElementType> getFirstRequiredLeafs() {
        return element.iteratedElement.cache.getFirstRequiredLeafs();
    }

    @Override
    public boolean startsWith(TokenTypeCategory typeCategory) {
        return element.iteratedElement.cache.startsWith(typeCategory);
    }

    @Override
    public boolean isFirstPossibleToken(TokenType tokenType) {
        return element.iteratedElement.cache.isFirstPossibleToken(tokenType) || element.isWrappingBegin(tokenType);
    }

    @Override
    public boolean isFirstRequiredToken(TokenType tokenType) {
        return element.iteratedElement.cache.isFirstRequiredToken(tokenType);
    }

    @Override
    public Set<LeafElementType> captureFirstPossibleLeafs(ElementLookupContext context, @Nullable Set<LeafElementType> bucket) {
        bucket = super.captureFirstPossibleLeafs(context, bucket);
        return element.iteratedElement.cache.captureFirstPossibleLeafs(context, bucket);
    }

    @Override
    public Set<TokenType> captureFirstPossibleTokens(ElementLookupContext context, @Nullable Set<TokenType> bucket) {
        bucket = super.captureFirstPossibleTokens(context, bucket);
        return element.iteratedElement.cache.captureFirstPossibleTokens(context, bucket);
    }

    @Override
    public Set<LeafElementType> captureSurrogateSuccessors(LeafElementType surrogateLead, Set<LeafElementType> bucket) {
        return element.iteratedElement.cache.captureSurrogateSuccessors(surrogateLead, bucket);
    }
}
