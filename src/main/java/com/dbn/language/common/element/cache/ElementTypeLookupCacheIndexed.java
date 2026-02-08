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

import com.dbn.common.index.IndexContainer;
import com.dbn.common.index.IndexContainer.IndexResolver;
import com.dbn.common.latent.Latent;
import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.TokenTypeBundle;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.IdentifierElementType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.WrappingDefinition;

import java.util.Set;

public abstract class ElementTypeLookupCacheIndexed<T extends ElementTypeBase> extends ElementTypeLookupCacheBase<T> {

    private transient final IndexContainer<LeafElementType> allPossibleLeafs = new IndexContainer<>(); // only used during initialization
    public final IndexContainer<LeafElementType> firstPossibleLeafs = new IndexContainer<>();
    public final IndexContainer<LeafElementType> firstRequiredLeafs = new IndexContainer<>();

    public final IndexContainer<TokenType> allPossibleTokens = new IndexContainer<>();
    public final IndexContainer<TokenType> firstPossibleTokens = new IndexContainer<>();
    public final IndexContainer<TokenType> firstRequiredTokens = new IndexContainer<>();
    private final Latent<Boolean> startsWithIdentifier = Latent.recursive(() -> loadStartsWithIdentifier());

    private final IndexResolver<TokenType> tokenTypeResolver = index -> getParserTokenTypes().getTokenType(index);
    private final IndexResolver<LeafElementType> elementTypeResolver = index -> getElementTypeBundle().getElement(index);

    ElementTypeLookupCacheIndexed(T elementType) {
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
        return allPossibleTokens.elements(tokenTypeResolver);
    }

    @Override
    public Set<TokenType> getFirstPossibleTokens() {
        return firstPossibleTokens.elements(tokenTypeResolver);
    }

    @Override
    public Set<TokenType> getFirstRequiredTokens() {
        return firstRequiredTokens.elements(tokenTypeResolver);
    }

    private TokenTypeBundle getParserTokenTypes() {
        return elementType.getLanguageDialect().getParserTokenTypes();
    }

    @Override
    public Set<LeafElementType> getFirstPossibleLeafs() {
        return firstPossibleLeafs.elements(elementTypeResolver);
    }

    @Override
    public Set<LeafElementType> getFirstRequiredLeafs() {
        return firstRequiredLeafs.elements(elementTypeResolver);
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
                allPossibleTokens.add(sharedTokenTypes.getIdentifier());
                allPossibleTokens.add(sharedTokenTypes.getQuotedIdentifier());
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
    public boolean startsWithIdentifier() {
        return startsWithIdentifier.get() == Boolean.TRUE;
    }

    private Boolean loadStartsWithIdentifier() {
        return checkStartsWithIdentifier() ? Boolean.TRUE : Boolean.FALSE;
    }

    protected abstract boolean checkStartsWithIdentifier();

    boolean isWrapperBeginLeaf(LeafElementType leaf) {
        WrappingDefinition wrapping = elementType.wrapping;
        return wrapping != null && wrapping.beginElementType == leaf;
    }
}
