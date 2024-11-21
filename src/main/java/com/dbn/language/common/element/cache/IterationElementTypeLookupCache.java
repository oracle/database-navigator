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
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.impl.WrappingDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class IterationElementTypeLookupCache extends ElementTypeLookupCache<IterationElementType> {
    public IterationElementTypeLookupCache(IterationElementType elementType) {
        super(elementType);
    }

    @Override
    public boolean containsToken(TokenType tokenType) {
        return elementType.isSeparator(tokenType) ||
                elementType.isWrappingBegin(tokenType) ||
                elementType.isWrappingEnd(tokenType) ||
                getIteratedElementLookupCache().containsToken(tokenType);
    }

    @Override
    public boolean containsLeaf(LeafElementType leafElementType) {
        if (getIteratedElementLookupCache().containsLeaf(leafElementType)) {
            return true;
        }

        if (leafElementType instanceof TokenElementType) {
            TokenElementType tokenElementType = (TokenElementType) leafElementType;
            if (elementType.isSeparator(tokenElementType)) {
                return true;
            }
        }

        return elementType.isWrappingBegin(leafElementType) || elementType.isWrappingEnd(leafElementType);
    }

    @Override
    public Set<TokenType> getFirstPossibleTokens() {
        return getIteratedElementLookupCache().getFirstPossibleTokens();
    }

    @Override
    public Set<TokenType> getFirstRequiredTokens() {
        return getIteratedElementLookupCache().getFirstRequiredTokens();
    }

    @Override
    public boolean couldStartWithLeaf(LeafElementType elementType) {
        return this.elementType.isWrappingBegin(elementType) || getIteratedElementLookupCache().couldStartWithLeaf(elementType);
    }

    @Override
    public boolean shouldStartWithLeaf(LeafElementType elementType) {
        return getIteratedElementLookupCache().shouldStartWithLeaf(elementType);
    }


    @Override
    public boolean couldStartWithToken(TokenType tokenType) {
        return elementType.isWrappingBegin(tokenType) ||
                getIteratedElementLookupCache().couldStartWithToken(tokenType);
    }

    @Override
    public Set<LeafElementType> getFirstPossibleLeafs() {
        Set<LeafElementType> firstPossibleLeafs = initBucket(null);
        firstPossibleLeafs.addAll(getIteratedElementLookupCache().getFirstPossibleLeafs());
        WrappingDefinition wrapping = elementType.wrapping;
        if (wrapping != null) {
            firstPossibleLeafs.add(wrapping.beginElementType);
        }
        return firstPossibleLeafs;
    }

    @Override
    public Set<LeafElementType> getFirstRequiredLeafs() {
        return getIteratedElementLookupCache().getFirstRequiredLeafs();
    }

    @Override
    public boolean isFirstPossibleLeaf(LeafElementType elementType) {
        WrappingDefinition wrapping = this.elementType.wrapping;
        if (wrapping != null) {
            if (wrapping.beginElementType == elementType) {
                return true;
            }
        }
        return getIteratedElementLookupCache().isFirstPossibleLeaf(elementType);
    }

    @Override
    public boolean isFirstRequiredLeaf(LeafElementType elementType) {
        return false;
    }

    @Override
    public boolean startsWithIdentifier() {
        return getIteratedElementLookupCache().startsWithIdentifier();
    }

    @Override
    public boolean isFirstPossibleToken(TokenType tokenType) {
        return getIteratedElementLookupCache().isFirstPossibleToken(tokenType) || elementType.isWrappingBegin(tokenType);
    }

    @Override
    public boolean isFirstRequiredToken(TokenType tokenType) {
        return getIteratedElementLookupCache().isFirstRequiredToken(tokenType);
    }

    @Override
    public Set<LeafElementType> captureFirstPossibleLeafs(ElementLookupContext context, @Nullable Set<LeafElementType> bucket) {
        bucket = super.captureFirstPossibleLeafs(context, bucket);
        return getIteratedElementLookupCache().captureFirstPossibleLeafs(context, bucket);
    }

    @Override
    public Set<TokenType> captureFirstPossibleTokens(ElementLookupContext context, @Nullable Set<TokenType> bucket) {
        bucket = super.captureFirstPossibleTokens(context, bucket);
        return getIteratedElementLookupCache().captureFirstPossibleTokens(context, bucket);
    }

    private ElementTypeLookupCache<?> getIteratedElementLookupCache() {
        return elementType.iteratedElementType.cache;
    }
}
