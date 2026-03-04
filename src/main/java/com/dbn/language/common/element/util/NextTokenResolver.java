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

package com.dbn.language.common.element.util;

import com.dbn.common.index.IndexContainer;
import com.dbn.common.util.Commons;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.cache.ElementTypeLookupCache;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.NamedElementType;
import com.dbn.language.common.element.impl.SequenceElementType;
import com.dbn.language.common.element.impl.TokenElementType;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class NextTokenResolver {
    private final ElementTypeBase source;
    private final Set<NamedElementType> visited = new HashSet<>();
    private IndexContainer<TokenType> bucket;

    private NextTokenResolver(ElementTypeBase source) {
        this.source = source;
    }

    public static NextTokenResolver from(ElementTypeBase source) {
        return new NextTokenResolver(source);
    }

    public IndexContainer<TokenType> resolve() {
        if (source instanceof NamedElementType namedElementType) {
            visit(namedElementType);
        } else {
            visitElement(source.parent, source);
        }
        return bucket;
    }

    private void visit(NamedElementType element) {
        if (visited.contains(element)) return;

        visited.add(element);
        for (ElementTypeBase parent : element.parents) {
            visitElement(parent, element);
        }
    }

    private void visitElement(ElementTypeBase parent, ElementTypeBase child) {
        while (parent != null) {
            if (parent instanceof NamedElementType) {
                if (visited.contains(parent)) return;
            }

            if (parent instanceof SequenceElementType sequenceElementType) {
                parent = visitSequence(sequenceElementType, child);

            } else if (parent instanceof IterationElementType iterationElementType) {
                visitIteration(iterationElementType);
            }

            if (parent != null) {
                child = parent;
                parent = child.parent;
                if (child instanceof NamedElementType namedElementType) {
                    visit(namedElementType);
                }
            }
        }
    }

    private void visitIteration(IterationElementType parent) {
        TokenElementType[] separatorTokens = parent.separatorTokens;
        if (separatorTokens == null) return;

        ensureBucket();
        for (TokenElementType separatorToken : separatorTokens) {
            bucket.add(separatorToken.tokenType);
        }
    }

    @Nullable
    private ElementTypeBase visitSequence(SequenceElementType parent, ElementType element) {
        int elementsCount = parent.children.length;
        int index = parent.indexOf(element, 0) + 1;

        if (index >= elementsCount) return parent;

        ElementTypeRef child = parent.children[index];
        while (child != null) {
            ensureBucket();
            ElementTypeLookupCache<?> lookupCache = child.elementType.cache;
            lookupCache.captureFirstPossibleTokens(bucket);
            if (!child.optional) {
                parent = null;
                break;
            }
            child = child.next;
        }
        return parent;
    }

    private void ensureBucket() {
        bucket = Commons.nvl(bucket, () -> new IndexContainer<>());
    }
}
