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
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.WrapperElementType;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class WrapperElementTypeLookupCache extends ElementTypeLookupCacheBase<WrapperElementType> {

    public WrapperElementTypeLookupCache(WrapperElementType elementType) {
        super(elementType);
    }

/*
    @Override
    boolean initAsFirstPossibleLeaf(LeafElementType leaf, ElementType source) {
        ElementTypeLookupCache startTokenLC = getElementType().getBeginTokenElement().getLookupCache();
        ElementTypeLookupCache wrappedTokenLC = getElementType().getWrappedElement().getLookupCache();
        return startTokenLC.couldStartWithLeaf(leaf) ||
               (*/
/*getElementType().isWrappingOptional() && *//*
wrappedTokenLC.couldStartWithLeaf(leaf));
    }

    @Override
    boolean initAsFirstRequiredLeaf(LeafElementType leaf, ElementType source) {
        ElementTypeLookupCache startTokenLC = getElementType().getBeginTokenElement().getLookupCache();
        return startTokenLC.shouldStartWithLeaf(leaf);
    }
*/

    @Override
    public Set<LeafElementType> captureFirstPossibleLeafs(ElementLookupContext context, @Nullable Set<LeafElementType> bucket) {
        bucket = super.captureFirstPossibleLeafs(context, bucket);
        bucket = initBucket(bucket);
        bucket.add(element.wrappingDefinition.beginElement);
        return bucket;
    }

    @Override
    public Set<TokenType> captureFirstPossibleTokens(ElementLookupContext context, @Nullable Set<TokenType> bucket) {
        bucket = super.captureFirstPossibleTokens(context, bucket);
        bucket = initBucket(bucket);
        bucket.add(element.wrappingDefinition.beginElement.tokenType);
        return bucket;
    }

    @Override
    public boolean containsToken(TokenType tokenType) {
        return element.wrappingDefinition.beginElement.tokenType == tokenType ||
                element.wrappingDefinition.endElement.tokenType == tokenType ||
                element.wrappedElement.cache.containsToken(tokenType);
    }

    @Override
    public Set<TokenType> getAllPossibleTokens() {
        Set<TokenType> tokenTypes = new HashSet<>();
        tokenTypes.add(element.wrappingDefinition.beginElement.tokenType);
        tokenTypes.add(element.wrappingDefinition.endElement.tokenType);
        tokenTypes.addAll(element.wrappedElement.cache.getAllPossibleTokens());
        return tokenTypes;
    }

    @Override
    public Set<TokenType> getFirstPossibleTokens() {
        return getFirstRequiredTokens();
    }

    @Override
    public Set<TokenType> getFirstRequiredTokens() {
        return element.wrappingDefinition.beginElement.cache.getFirstRequiredTokens();
    }

    @Override
    public boolean couldStartWithLeaf(LeafElementType elementType) {
        if (this.element.wrappingDefinition.beginElement == elementType) return true;
        if (this.element.wrappedElement.cache.couldStartWithLeaf(elementType)) return true;
        return false;
    }

    @Override
    public boolean shouldStartWithLeaf(LeafElementType leafElementType) {
        return element.wrappingDefinition.beginElement == leafElementType;
    }

    @Override
    public boolean couldStartWithToken(TokenType tokenType) {
        if (element.wrappingDefinition.beginElement.cache.couldStartWithToken(tokenType)) return true;
        if (element.wrappedElement.cache.couldStartWithToken(tokenType)) return true;
        return false;
    }

    @Override
    public Set<LeafElementType> getFirstPossibleLeafs() {
        return getFirstRequiredLeafs();
    }

    @Override
    public Set<LeafElementType> getFirstRequiredLeafs() {
        return Set.of(element.wrappingDefinition.beginElement);
    }

    @Override
    public boolean startsWith(TokenTypeCategory typeCategory) {
        return element.wrappingDefinition.beginElement.tokenType.getCategory() == typeCategory;
    }

    @Override
    public boolean isFirstPossibleToken(TokenType tokenType) {
        return couldStartWithToken(tokenType);
    }

    @Override
    public boolean isFirstRequiredToken(TokenType tokenType) {
        return element.wrappingDefinition.beginElement.tokenType == tokenType;
    }
}