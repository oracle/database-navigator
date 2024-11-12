package com.dbn.language.common.element.util;

import com.dbn.common.index.IndexContainer;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Compactables;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.cache.ElementTypeLookupCache;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.NamedElementType;
import com.dbn.language.common.element.impl.SequenceElementType;
import com.dbn.language.common.element.impl.TokenElementType;
import org.jetbrains.annotations.NotNull;
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
        if (source instanceof NamedElementType) {
            visit((NamedElementType) source);
        } else {
            visitElement(source.parent, source);
        }
        Compactables.compact(bucket);
        return bucket;
    }

    private void visit(@NotNull NamedElementType element) {
        if (!visited.contains(element)) {
            visited.add(element);
            for (ElementTypeBase parent : element.parents) {
                visitElement(parent, element);
            }
        }
    }

    private void visitElement(ElementTypeBase parent, ElementTypeBase child) {
        while (parent != null) {
            if (parent instanceof SequenceElementType) {
                parent = visitSequence((SequenceElementType) parent, child);

            } else if (parent instanceof IterationElementType) {
                visitIteration((IterationElementType) parent);
            }

            if (parent != null) {
                child = parent;
                parent = child.parent;
                if (child instanceof NamedElementType) {
                    visit((NamedElementType) child);
                }
            }
        }
    }

    private void visitIteration(IterationElementType parent) {
        TokenElementType[] separatorTokens = parent.separatorTokens;
        if (separatorTokens != null) {
            ensureBucket();
            for (TokenElementType separatorToken : separatorTokens) {
                bucket.add(separatorToken.tokenType);
            }
        }
    }

    @Nullable
    private ElementTypeBase visitSequence(SequenceElementType parent, ElementType element) {
        int elementsCount = parent.getChildCount();
        int index = parent.indexOf(element, 0) + 1;

        if (index < elementsCount) {
            ElementTypeRef child = parent.getChild(index);
            while (child != null) {
                ensureBucket();
                ElementTypeLookupCache lookupCache = child.elementType.cache;
                lookupCache.captureFirstPossibleTokens(bucket);
                if (!child.optional) {
                    parent = null;
                    break;
                }
                child = child.getNext();
            }
        }
        return parent;
    }

    private void ensureBucket() {
        bucket = Commons.nvl(bucket, () -> new IndexContainer<>());
    }
}
