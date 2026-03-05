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

import com.dbn.common.index.BackedIndexContainer;
import com.dbn.common.index.IndexContainer;
import com.dbn.common.index.IndexContainer.IndexResolver;
import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.TokenTypeBundle;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.IdentifierElementType;
import com.dbn.language.common.element.impl.LeafElementType;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public abstract class ElementTypeIndexedCache<T extends ElementTypeBase> extends ElementTypeCacheBase<T> {
    private final IndexResolver<TokenType> tokenTypeResolver = index -> getParserTokenTypes().getTokenType(index);
    private final IndexResolver<LeafElementType> elementTypeResolver = index -> getElementTypeBundle().getElement(index);

    private transient final IndexContainer<LeafElementType> allPossibleLeafs = new IndexContainer<>(); // only used during initialization
    public final BackedIndexContainer<LeafElementType> firstPossibleLeafs = new BackedIndexContainer<>(elementTypeResolver);
    public final BackedIndexContainer<LeafElementType> firstRequiredLeafs = new BackedIndexContainer<>(elementTypeResolver);

    public final BackedIndexContainer<TokenType> allPossibleTokens = new BackedIndexContainer<>(tokenTypeResolver);
    public final BackedIndexContainer<TokenType> firstPossibleTokens = new BackedIndexContainer<>(tokenTypeResolver);
    public final BackedIndexContainer<TokenType> firstRequiredTokens = new BackedIndexContainer<>(tokenTypeResolver);

    private final Map<TokenTypeCategory, Boolean> startsWithTokenCategory = new ConcurrentHashMap<>();


    ElementTypeIndexedCache(T elementType) {
        super(elementType);
        assert !elementType.isLeaf();
    }

    @Override
    public boolean isFirstPossibleToken(TokenType tokenType) {
        return firstPossibleTokens.contains(tokenType);
    }

    @Override
    public boolean isFirstRequiredToken(TokenType tokenType) {
        return firstRequiredTokens.contains(tokenType);
    }

    @Override
    public boolean containsToken(TokenType tokenType) {
        return allPossibleTokens.contains(tokenType);
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

    private TokenTypeBundle getParserTokenTypes() {
        return elementType.getLanguageDialect().getParserTokenTypes();
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
        return firstPossibleLeafs.contains(elementType);
    }

    @Override
    public boolean couldStartWithToken(TokenType tokenType) {
        return firstPossibleTokens.contains(tokenType);
    }

    @Override
    public boolean shouldStartWithLeaf(LeafElementType elementType) {
        return firstRequiredLeafs.contains(elementType);
    }

    @Override
    public void registerLeaf(LeafElementType leaf, ElementTypeBase source) {
        boolean initAllElements = initAllElements(leaf);
        boolean initAsFirstPossibleLeaf = initAsFirstPossibleLeaf(leaf, source);
        boolean initAsFirstRequiredLeaf = initAsFirstRequiredLeaf(leaf, source);

        // register first possible leafs
        if (initAsFirstPossibleLeaf) {
            firstPossibleLeafs.add(leaf);
            leaf.cache.captureFirstPossibleTokens(firstPossibleTokens);
        }

        // register first required leafs
        if (initAsFirstRequiredLeaf) {
            firstRequiredLeafs.add(leaf);
            leaf.cache.captureFirstPossibleTokens(firstRequiredTokens);
        }

        if (initAllElements) {
            // register all possible leafs
            allPossibleLeafs.add(leaf);

            // register all possible tokens
            if (leaf instanceof IdentifierElementType) {
                SharedTokenTypeBundle sharedTokenTypes = getSharedTokenTypes();
                allPossibleTokens.add(sharedTokenTypes.identifier);
                allPossibleTokens.add(sharedTokenTypes.quotedIdentifier);
            } else {
                allPossibleTokens.add(leaf.tokenType);
            }
        }

        if (initAsFirstPossibleLeaf || initAsFirstRequiredLeaf || initAllElements) {
            // walk the tree up
            registerLeafInParent(leaf);
        }
    }

    abstract boolean initAsFirstPossibleLeaf(LeafElementType leaf, ElementTypeBase source);
    abstract boolean initAsFirstRequiredLeaf(LeafElementType leaf, ElementTypeBase source);
    private boolean initAllElements(LeafElementType leafElementType) {
        return leafElementType != elementType && !allPossibleLeafs.contains(leafElementType);
    }

    protected void registerLeafInParent(LeafElementType leaf) {
        super.registerLeaf(leaf, null);
    }

    @Override
    public boolean startsWith(TokenTypeCategory typeCategory) {
        return startsWithTokenCategory.computeIfAbsent(typeCategory,
                c -> checkStartsWith(c) ? TRUE : FALSE);
    }

    protected abstract boolean checkStartsWith(TokenTypeCategory typeCategory);

    boolean isWrapperBeginLeaf(LeafElementType leaf) {
        return elementType.wrapping != null && elementType.wrapping.beginElement == leaf;
    }
}
