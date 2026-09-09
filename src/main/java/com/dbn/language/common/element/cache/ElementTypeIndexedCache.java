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

import com.dbn.common.index.IndexContainer.IndexResolver;
import com.dbn.common.index.StagingIndexContainer;
import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.IdentifierElementType;
import com.dbn.language.common.element.impl.LeafElementType;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ElementTypeIndexedCache<T extends ElementTypeBase> extends ElementTypeCacheBase<T> {
    private final IndexResolver<TokenType> tokenTypeResolver = index -> elementType.bundle.tokenTypeBundle.getTokenType(index);
    private final IndexResolver<LeafElementType> elementTypeResolver = index -> elementType.bundle.getElement(index);

    private transient StagingIndexContainer<LeafElementType> allPossibleLeafs = new StagingIndexContainer<>(elementTypeResolver); // only used during init

    public final StagingIndexContainer<LeafElementType> firstPossibleLeafs = new StagingIndexContainer<>(elementTypeResolver);
    public final StagingIndexContainer<LeafElementType> firstRequiredLeafs = new StagingIndexContainer<>(elementTypeResolver);

    public final StagingIndexContainer<TokenType> allPossibleTokens = new StagingIndexContainer<>(tokenTypeResolver);
    public final StagingIndexContainer<TokenType> firstPossibleTokens = new StagingIndexContainer<>(tokenTypeResolver);
    public final StagingIndexContainer<TokenType> firstRequiredTokens = new StagingIndexContainer<>(tokenTypeResolver);

    private final Map<TokenTypeCategory, Boolean> startsWithTokenCategory = new ConcurrentHashMap<>();


    ElementTypeIndexedCache(T elementType) {
        super(elementType);
        assert !elementType.isLeaf();
    }

    @Override
    public boolean isFirstPossibleToken(TokenType tokenType) {
        return firstPossibleTokens.contains(tokenType.index());
    }

    @Override
    public boolean isFirstRequiredToken(TokenType tokenType) {
        return firstRequiredTokens.contains(tokenType.index());
    }

    @Override
    public boolean containsToken(TokenType tokenType) {
        return allPossibleTokens.contains(tokenType.index());
    }

    @Override
    public Set<TokenType> getAllPossibleTokens() {
        return allPossibleTokens.elements();
    }

    @Override
    public Set<TokenType> getFirstPossibleTokens() {
        return firstPossibleTokens.elements();
    }

    @Override
    public Set<TokenType> getFirstRequiredTokens() {
        return firstRequiredTokens.elements();
    }

    @Override
    public Set<LeafElementType> getFirstPossibleLeafs() {
        return firstPossibleLeafs.elements();
    }

    @Override
    public Set<LeafElementType> getFirstRequiredLeafs() {
        return firstRequiredLeafs.elements();
    }

    @Override
    public boolean couldStartWithLeaf(LeafElementType elementType) {
        return firstPossibleLeafs.contains(elementType.index());
    }

    @Override
    public boolean couldStartWithToken(TokenType tokenType) {
        return firstPossibleTokens.contains(tokenType.index());
    }

    @Override
    public boolean shouldStartWithLeaf(LeafElementType elementType) {
        return firstRequiredLeafs.contains(elementType.index());
    }

    @Override
    public void registerLeaf(LeafElementType leaf, ElementTypeBase source) {
        int leafIndex = leaf.index();
        boolean initAllElements = initAllElements(leaf, leafIndex);
        boolean initAsFirstPossibleLeaf = initAsFirstPossibleLeaf(leaf, source);
        boolean initAsFirstRequiredLeaf = initAsFirstRequiredLeaf(leaf, source);

        // register first possible leafs
        boolean registeredFirstPossibleLeaf = initAsFirstPossibleLeaf &&
                firstPossibleLeafs.addIfAbsent(leafIndex);
        if (registeredFirstPossibleLeaf) {
            leaf.cache.captureFirstPossibleTokens(firstPossibleTokens);
        }

        // register first required leafs
        boolean registeredFirstRequiredLeaf = initAsFirstRequiredLeaf &&
                firstRequiredLeafs.addIfAbsent(leafIndex);
        if (registeredFirstRequiredLeaf) {
            leaf.cache.captureFirstPossibleTokens(firstRequiredTokens);
        }

        if (initAllElements) {
            // register all possible leafs
            // register all possible tokens
            if (leaf instanceof IdentifierElementType) {
                SharedTokenTypeBundle sharedTokenTypes = getSharedTokenTypes();
                allPossibleTokens.add(sharedTokenTypes.identifier);
            } else {
                allPossibleTokens.add(leaf.tokenType);
            }
        }

        if (registeredFirstPossibleLeaf || registeredFirstRequiredLeaf || initAllElements) {
            // walk the tree up
            registerLeafInParent(leaf);
        }
    }

    abstract boolean initAsFirstPossibleLeaf(LeafElementType leaf, ElementTypeBase source);
    abstract boolean initAsFirstRequiredLeaf(LeafElementType leaf, ElementTypeBase source);
    private boolean initAllElements(LeafElementType leafElementType, int leafIndex) {
        if (allPossibleLeafs == null) return false;
        if (leafElementType == elementType) return false;
        return allPossibleLeafs.addIfAbsent(leafIndex);
    }

    public void releaseInitState() {
        if (allPossibleLeafs == null) return;

        firstPossibleLeafs.freeze();
        firstRequiredLeafs.freeze();
        allPossibleTokens.freeze();
        firstPossibleTokens.freeze();
        firstRequiredTokens.freeze();
        allPossibleLeafs.discard();
        allPossibleLeafs = null;
    }

    protected void registerLeafInParent(LeafElementType leaf) {
        super.registerLeaf(leaf, null);
    }

    @Override
    public boolean startsWith(TokenTypeCategory typeCategory) {
        Boolean startsWith = startsWithTokenCategory.get(typeCategory);
        if (startsWith == null) {
            startsWith = checkStartsWith(typeCategory);
            startsWithTokenCategory.put(typeCategory, startsWith);
        }
        return startsWith;
    }

    protected abstract boolean checkStartsWith(TokenTypeCategory typeCategory);

    boolean isWrapperBeginLeaf(LeafElementType leaf) {
        return elementType.wrapping != null && elementType.wrapping.beginElement == leaf;
    }
}
