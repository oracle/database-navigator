package com.dbn.language.common.element.cache;

import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.LeafElementType;

import java.util.Set;

public class VoidElementTypeLookupCache<T extends ElementTypeBase> extends ElementTypeLookupCache<T>{
    public VoidElementTypeLookupCache(T elementType) {
        super(elementType);
    }

    @Override
    public boolean containsToken(TokenType tokenType) {
        return false;
    }

    @Override
    public boolean containsLeaf(LeafElementType elementType) {
        return false;
    }

    @Override
    public Set<TokenType> getFirstPossibleTokens() {
        return null;
    }

    @Override
    public Set<TokenType> getFirstRequiredTokens() {
        return null;
    }

    @Override
    public boolean couldStartWithLeaf(LeafElementType elementType) {
        return false;
    }

    @Override
    public boolean shouldStartWithLeaf(LeafElementType elementType) {
        return false;
    }

    @Override
    public boolean couldStartWithToken(TokenType tokenType) {
        return false;
    }

    @Override
    public Set<LeafElementType> getFirstPossibleLeafs() {
        return null;
    }

    @Override
    public Set<LeafElementType> getFirstRequiredLeafs() {
        return null;
    }

    @Override
    public boolean isFirstPossibleLeaf(LeafElementType elementType) {
        return false;
    }

    @Override
    public boolean isFirstRequiredLeaf(LeafElementType elementType) {
        return false;
    }

    @Override
    public boolean startsWithIdentifier() {
        return false;
    }

    @Override
    public boolean isFirstPossibleToken(TokenType tokenType) {
        return false;
    }

    @Override
    public boolean isFirstRequiredToken(TokenType tokenType) {
        return false;
    }
}
