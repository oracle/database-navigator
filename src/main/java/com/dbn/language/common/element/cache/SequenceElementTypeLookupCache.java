package com.dbn.language.common.element.cache;

import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.SequenceElementType;

import java.util.Set;

public class SequenceElementTypeLookupCache<T extends SequenceElementType> extends ElementTypeLookupCacheIndexed<T> {

    public SequenceElementTypeLookupCache(T elementType) {
        super(elementType);
    }

    @Override
    boolean initAsFirstPossibleLeaf(LeafElementType leaf, ElementTypeBase source) {
        boolean notInitialized = !firstPossibleLeafs.contains(leaf);
        return notInitialized && (
                isWrapperBeginLeaf(leaf) ||
                    (couldStartWithElement(source) &&
                     source.cache.couldStartWithLeaf(leaf)));
    }

    @Override
    boolean initAsFirstRequiredLeaf(LeafElementType leaf, ElementTypeBase source) {
        boolean notInitialized = !firstRequiredLeafs.contains(leaf);
        return notInitialized &&
                shouldStartWithElement(source) &&
                source.cache.shouldStartWithLeaf(leaf);
    }

    private boolean couldStartWithElement(ElementType elementType) {
        ElementTypeRef child = this.elementType.getFirstChild();
        while (child != null) {
            if (child.optional) {
                if (elementType == child.elementType) return true;
            } else {
                return child.elementType == elementType;
            }
            child = child.getNext();
        }
        return false;
    }

    private boolean shouldStartWithElement(ElementType elementType) {
        ElementTypeRef child = this.elementType.getFirstChild();
        while (child != null) {
            if (!child.optional) {
                return child.elementType == elementType;
            }
            child = child.getNext();
        }
        return false;
    }

    @Override
    public boolean checkStartsWithIdentifier() {
        ElementTypeRef child = this.elementType.getFirstChild();
        while (child != null) {
            if (child.elementType.cache.startsWithIdentifier()) {
                return true;
            }

            if (!child.optional) {
                return false;
            }
            child = child.getNext();
        }
        return false;
    }

    @Override
    public Set<LeafElementType> captureFirstPossibleLeafs(ElementLookupContext context, Set<LeafElementType> bucket) {
        bucket = super.captureFirstPossibleLeafs(context, bucket);
        bucket = initBucket(bucket);

        ElementTypeRef child = this.elementType.getFirstChild();
        while (child != null) {
            if (context.check(child)) {
                child.elementType.cache.captureFirstPossibleLeafs(context, bucket);
            }
            if (!child.optional) break;
            child = child.getNext();
        }
        return bucket;
    }

    @Override
    public Set<TokenType> captureFirstPossibleTokens(ElementLookupContext context, Set<TokenType> bucket) {
        bucket = super.captureFirstPossibleTokens(context, bucket);
        bucket = initBucket(bucket);

        ElementTypeRef child = this.elementType.getFirstChild();
        while (child != null) {
            if (context.check(child)) {
                child.elementType.cache.captureFirstPossibleTokens(context, bucket);
            }
            if (!child.optional) break;
            child = child.getNext();
        }
        return bucket;
    }
}

