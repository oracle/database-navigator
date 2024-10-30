package com.dbn.language.common.element.cache;

import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.BasicElementType;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.LeafElementType;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class BasicElementTypeLookupCache extends ElementTypeLookupCacheIndexed<BasicElementType> {
    public BasicElementTypeLookupCache(BasicElementType elementType) {
        super(elementType);
    }

    @Override
    boolean initAsFirstPossibleLeaf(LeafElementType leaf, ElementTypeBase source) {
        return false;
    }

    @Override
    boolean initAsFirstRequiredLeaf(LeafElementType leaf, ElementTypeBase source) {
        return false;
    }

    @Override
    public boolean containsToken(TokenType tokenType) {
        return false;
    }

    @Override
    public boolean startsWithIdentifier() {
        return false;
    }

    @Override
    public boolean checkStartsWithIdentifier() {return false;}

    @Override
    public Set<LeafElementType> captureFirstPossibleLeafs(ElementLookupContext context, @Nullable Set<LeafElementType> bucket) {
        return bucket;
    }

    @Override
    public Set<TokenType> captureFirstPossibleTokens(ElementLookupContext context, @Nullable Set<TokenType> bucket) {
        return bucket;
    }
}