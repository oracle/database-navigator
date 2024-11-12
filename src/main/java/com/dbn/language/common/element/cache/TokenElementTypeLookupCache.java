package com.dbn.language.common.element.cache;

import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.TokenElementType;

import java.util.Collections;
import java.util.Set;

public class TokenElementTypeLookupCache extends LeafElementTypeLookupCache<TokenElementType>{
    public TokenElementTypeLookupCache(TokenElementType elementType) {
        super(elementType);
    }

    @Override
    public boolean isFirstPossibleToken(TokenType tokenType) {
        return getTokenType() == tokenType;
    }

    protected TokenType getTokenType() {
        return elementType.tokenType;
    }

    @Override
    public boolean isFirstRequiredToken(TokenType tokenType) {
        return getTokenType() == tokenType;
    }

    @Override
    public Set<TokenType> getFirstPossibleTokens() {
        return Collections.singleton(getTokenType());
    }

    @Override
    public void captureFirstPossibleTokens(Set<TokenType> bucket) {
        bucket.add(getTokenType());
    }

    @Override
    public boolean containsToken(TokenType tokenType) {
        return getTokenType() == tokenType;
    }

    @Override
    public boolean startsWithIdentifier() {
        return false;
    }
}