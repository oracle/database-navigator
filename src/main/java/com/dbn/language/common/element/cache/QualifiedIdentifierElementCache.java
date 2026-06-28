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

import com.dbn.language.common.TokenChain;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.QualifiedIdentifierElementType;
import com.dbn.language.common.element.impl.QualifiedIdentifierVariant;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class QualifiedIdentifierElementCache extends ElementTypeIndexedCache<QualifiedIdentifierElementType> {
    private final Map<TokenChain, QualifiedIdentifierVariant> probableParseVariants = new ConcurrentHashMap<>();

    public QualifiedIdentifierElementCache(QualifiedIdentifierElementType elementType) {
        super(elementType);
    }

    @Override
    boolean initAsFirstPossibleLeaf(LeafElementType leaf, ElementTypeBase source) {
        for (LeafElementType[] variant : elementType.variants) {
            if (variant[0] == source) return true;
        }
        return false;
    }

    @Override
    boolean initAsFirstRequiredLeaf(LeafElementType leaf, ElementTypeBase source) {
        for (LeafElementType[] variant : elementType.variants) {
            if (variant[0] == source && !variant[0].optional) return true;
        }
        return false;
    }

    @Override
    protected boolean checkStartsWith(TokenTypeCategory typeCategory) {
        for (LeafElementType[] variant : elementType.variants) {
            if (variant[0].cache.startsWith(typeCategory)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<LeafElementType> captureFirstPossibleLeafs(ElementLookupContext context, @Nullable Set<LeafElementType> bucket) {
        bucket = initBucket(bucket);
        for (LeafElementType[] elementTypes : elementType.variants) {
            // variants already consider optional leafs
            bucket.add(elementTypes[0]);
        }

        return bucket;
    }

    @Override
    public Set<TokenType> captureFirstPossibleTokens(ElementLookupContext context, @Nullable Set<TokenType> bucket) {
        bucket = initBucket(bucket);
        for (LeafElementType[] elementTypes : elementType.variants) {
            // variants already consider optional leafs
            bucket.add(elementTypes[0].tokenType);
        }

        return bucket;
    }

    @Override
    public Set<LeafElementType> captureSurrogateSuccessors(LeafElementType surrogateLead, Set<LeafElementType> bucket) {
        for (LeafElementType[] elementTypes : elementType.variants) {
            if (elementTypes.length <= 1) continue;
            if (!surrogateLead.isSurrogateFor(elementTypes[0])) continue;

            bucket = initBucket(bucket);
            bucket.add(elementTypes[1]);
        }

        return bucket;
    }

    public QualifiedIdentifierVariant getMostProbableParseVariant(TokenChain tokenChain) {
        QualifiedIdentifierVariant variant = probableParseVariants.get(tokenChain);
        if (variant == null) {
            variant = evaluateMostProbableParseVariant(tokenChain);
            if (variant != null) {
                probableParseVariants.put(tokenChain, variant);
            }
        }
        return variant;
    }

    private QualifiedIdentifierVariant evaluateMostProbableParseVariant(TokenChain tokenChain) {
        QualifiedIdentifierVariant mostProbableVariant = null;

        for (LeafElementType[] elementTypes : elementType.variants) {
            if (elementTypes.length <= tokenChain.size()) {
                int matchedTokens = 0;
                for (int i=0; i<elementTypes.length; i++) {
                    if (elementTypes[i].tokenType.matches(tokenChain.get(i))) {
                        matchedTokens++;
                    }
                }
                if (mostProbableVariant == null || mostProbableVariant.matchedTokens < matchedTokens) {
                    mostProbableVariant = mostProbableVariant == null ?
                            new QualifiedIdentifierVariant(elementTypes, matchedTokens) :
                            mostProbableVariant.replace(elementTypes, matchedTokens);
                }

            }
        }

        return mostProbableVariant;
    }
}
