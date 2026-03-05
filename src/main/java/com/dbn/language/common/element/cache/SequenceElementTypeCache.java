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
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.SequenceElementType;
import com.dbn.language.common.element.impl.SurrogateSequenceElementType;

import java.util.Set;

import static com.dbn.language.common.element.util.ElementTypeAttribute.SURROGATE_LEAD;

public class SequenceElementTypeCache<T extends SequenceElementType> extends ElementTypeIndexedCache<T> {

    public SequenceElementTypeCache(T elementType) {
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
            child = child.next;
        }
        return false;
    }

    private boolean shouldStartWithElement(ElementType elementType) {
        ElementTypeRef child = this.elementType.getFirstChild();
        while (child != null) {
            if (!child.optional) {
                return child.elementType == elementType;
            }
            child = child.next;
        }
        return false;
    }

    @Override
    protected boolean checkStartsWith(TokenTypeCategory typeCategory) {
        ElementTypeRef child = this.elementType.getFirstChild();
        while (child != null) {
            if (child.elementType.cache.startsWith(typeCategory)) return true;
            if (!child.optional) return false;
            child = child.next;
        }
        return false;    }

    @Override
    public Set<LeafElementType> captureFirstPossibleLeafs(ElementLookupContext context, Set<LeafElementType> bucket) {
        if (elementType instanceof SurrogateSequenceElementType surrogateSequence) {
            return surrogateSequence.getMainElementType().cache.captureFirstPossibleLeafs(context, bucket);
        }

        bucket = super.captureFirstPossibleLeafs(context, bucket);
        bucket = initBucket(bucket);

        ElementTypeRef child = this.elementType.getFirstChild();
        while (child != null) {
            if (context.check(child)) {
                child.elementType.cache.captureFirstPossibleLeafs(context, bucket);
            }
            if (!child.optional) break;
            child = child.next;
        }
        return bucket;
    }

    @Override
    public Set<TokenType> captureFirstPossibleTokens(ElementLookupContext context, Set<TokenType> bucket) {
        if (elementType instanceof SurrogateSequenceElementType surrogateSequence) {
            return surrogateSequence.getMainElementType().cache.captureFirstPossibleTokens(context, bucket);
        }

        bucket = super.captureFirstPossibleTokens(context, bucket);
        bucket = initBucket(bucket);

        ElementTypeRef child = this.elementType.getFirstChild();
        while (child != null) {
            if (context.check(child)) {
                child.elementType.cache.captureFirstPossibleTokens(context, bucket);
            }
            if (!child.optional) break;
            child = child.next;
        }
        return bucket;
    }

    @Override
    public Set<LeafElementType> captureSurrogateSuccessors(LeafElementType surrogateLead, Set<LeafElementType> bucket) {
        ElementTypeRef leadCandidate = elementType.getFirstChild();

        while (true) {
            if (surrogateLead.isSurrogateFor(leadCandidate.elementType)) break;
            if (!leadCandidate.optional) return bucket;
            leadCandidate = leadCandidate.next;
        }

        if (leadCandidate.elementType instanceof LeafElementType) {
            ElementTypeRef successorCandidate = leadCandidate.next;
            if (leadCandidate.elementType.is(SURROGATE_LEAD)) {
                bucket = successorCandidate.elementType.cache.captureSurrogateSuccessors(surrogateLead, bucket);
            } else {
                while (successorCandidate != null) {
                    bucket = initBucket(bucket);
                    bucket.addAll(successorCandidate.elementType.cache.getFirstPossibleLeafs());
                    if (!successorCandidate.optional) break;
                    successorCandidate = successorCandidate.next;
                }
            }
            return bucket;
        }

        return leadCandidate.elementType.cache.captureSurrogateSuccessors(surrogateLead, bucket);
    }
}

