/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.dev.language;

import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.OneOfElementType;
import com.dbn.language.common.element.impl.QualifiedIdentifierElementType;
import com.dbn.language.common.element.impl.SequenceElementType;
import com.dbn.language.common.element.impl.WrapperElementType;
import com.dbn.language.common.element.impl.WrappingDefinition;
import com.dbn.language.common.element.path.LanguageNodeBase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class LanguageSpecificationNextLeafResolver {
    private final Map<LanguageNodeBase, Set<LanguageNodeBase>> firstLeafCache = new HashMap<>();
    private final Map<LanguageNodeBase, Set<LanguageNodeBase>> nextLeafCache = new HashMap<>();

    public Set<LanguageNodeBase> getFirstPossibleLeafs(LanguageNodeBase source) {
        Set<LanguageNodeBase> leafs = firstLeafCache.get(source);
        if (leafs != null) return leafs;

        leafs = buildFirstPossibleLeafs(source, new HashSet<>());
        firstLeafCache.put(source, leafs);
        return leafs;
    }

    public Set<LanguageNodeBase> getNextPossibleLeafs(LanguageNodeBase source) {
        Set<LanguageNodeBase> leafs = nextLeafCache.get(source);
        if (leafs != null) return leafs;

        leafs = buildNextPossibleLeafs(source);
        nextLeafCache.put(source, leafs);
        return leafs;
    }

    private Set<LanguageNodeBase> buildFirstPossibleLeafs(
            LanguageNodeBase source,
            Set<ElementTypeBase> resolving) {
        ElementTypeBase element = source.element;
        if (element instanceof LeafElementType) return Set.of(source);
        if (!resolving.add(element)) return Set.of();

        try {
            if (element instanceof SequenceElementType sequence) {
                Set<LanguageNodeBase> leafs = new LinkedHashSet<>();
                addOptionalWrappingBegin(source, sequence.wrapping, leafs);
                addFirstSequenceLeafs(source, sequence, resolving, leafs);
                return leafs;
            }

            if (element instanceof OneOfElementType oneOf) {
                Set<LanguageNodeBase> leafs = new LinkedHashSet<>();
                addOptionalWrappingBegin(source, oneOf.wrapping, leafs);
                for (ElementTypeRef child : oneOf.children) {
                    LanguageNodeBase childNode = new LanguageNodeBase(child.elementType, source);
                    leafs.addAll(buildFirstPossibleLeafs(childNode, resolving));
                }
                return leafs;
            }

            if (element instanceof IterationElementType iteration) {
                Set<LanguageNodeBase> leafs = new LinkedHashSet<>();
                addOptionalWrappingBegin(source, iteration.wrapping, leafs);
                LanguageNodeBase iteratedNode = new LanguageNodeBase(iteration.iteratedElement, source);
                leafs.addAll(buildFirstPossibleLeafs(iteratedNode, resolving));
                return leafs;
            }

            if (element instanceof WrapperElementType wrapper) {
                return Set.of(new LanguageNodeBase(wrapper.getBeginTokenElement(), source));
            }

            if (element instanceof QualifiedIdentifierElementType identifier) {
                Set<LanguageNodeBase> leafs = new LinkedHashSet<>();
                for (LeafElementType[] variant : identifier.variants) {
                    if (variant.length > 0) {
                        leafs.add(new LanguageNodeBase(variant[0], source));
                    }
                }
                return leafs;
            }

            Set<LanguageNodeBase> leafs = new LinkedHashSet<>();
            for (LeafElementType leaf : element.cache.getFirstPossibleLeafs()) {
                leafs.add(new LanguageNodeBase(leaf, source));
            }
            return leafs;
        } finally {
            resolving.remove(element);
        }
    }

    private void addFirstSequenceLeafs(
            LanguageNodeBase sequenceNode,
            SequenceElementType sequence,
            Set<ElementTypeBase> resolving,
            Set<LanguageNodeBase> leafs) {
        for (ElementTypeRef child : sequence.children) {
            LanguageNodeBase childNode = new LanguageNodeBase(child.elementType, sequenceNode);
            leafs.addAll(buildFirstPossibleLeafs(childNode, resolving));
            if (!child.optional) return;
        }
    }

    private static void addOptionalWrappingBegin(
            LanguageNodeBase parent,
            WrappingDefinition wrapping,
            Set<LanguageNodeBase> leafs) {
        if (wrapping != null && wrapping.optional) {
            leafs.add(new LanguageNodeBase(wrapping.beginElement, parent));
        }
    }

    private Set<LanguageNodeBase> buildNextPossibleLeafs(LanguageNodeBase source) {
        LanguageNodeBase parentNode = source.getParent();
        if (parentNode == null) return Set.of();

        ElementTypeBase parent = parentNode.element;
        if (parent instanceof SequenceElementType sequence) {
            return getNextPossibleLeafs(source, parentNode, sequence);
        }
        if (parent instanceof IterationElementType iteration) {
            return getNextPossibleLeafs(source, parentNode, iteration);
        }
        if (parent instanceof OneOfElementType oneOf) {
            return getNextPossibleLeafs(source, parentNode, oneOf);
        }
        if (parent instanceof WrapperElementType wrapper) {
            return getNextPossibleLeafs(source, parentNode, wrapper);
        }
        if (parent instanceof QualifiedIdentifierElementType identifier) {
            return getNextPossibleLeafs(source, parentNode, identifier);
        }
        return Set.of();
    }

    private Set<LanguageNodeBase> getNextPossibleLeafs(
            LanguageNodeBase source,
            LanguageNodeBase sequenceNode,
            SequenceElementType sequence) {
        ElementTypeBase sourceElement = source.element;
        WrappingDefinition wrapping = sequence.wrapping;
        if (wrapping != null && wrapping.optional) {
            if (sourceElement == wrapping.beginElement) {
                Set<LanguageNodeBase> leafs = new LinkedHashSet<>();
                addFirstSequenceLeafs(sequenceNode, sequence, new HashSet<>(), leafs);
                if (allChildrenOptional(sequence)) {
                    leafs.add(new LanguageNodeBase(wrapping.endElement, sequenceNode));
                }
                return leafs;
            }
            if (sourceElement == wrapping.endElement) {
                return getNextPossibleLeafs(sequenceNode);
            }
        }

        int sourceIndex = sequence.indexOf(sourceElement, 0);
        if (sourceIndex == -1) return Set.of();

        Set<LanguageNodeBase> nextLeafs = new LinkedHashSet<>();
        for (int i = sourceIndex + 1; i < sequence.children.length; i++) {
            ElementTypeRef nextElement = sequence.children[i];
            LanguageNodeBase nextNode = new LanguageNodeBase(nextElement.elementType, sequenceNode);
            nextLeafs.addAll(getFirstPossibleLeafs(nextNode));
            if (!nextElement.optional) return nextLeafs;
        }

        nextLeafs.addAll(getNextPossibleLeafs(sequenceNode));
        if (wrapping != null && wrapping.optional) {
            nextLeafs.add(new LanguageNodeBase(wrapping.endElement, sequenceNode));
        }
        return nextLeafs;
    }

    private static boolean allChildrenOptional(SequenceElementType sequence) {
        for (ElementTypeRef child : sequence.children) {
            if (!child.optional) return false;
        }
        return true;
    }

    private Set<LanguageNodeBase> getNextPossibleLeafs(
            LanguageNodeBase source,
            LanguageNodeBase iterationNode,
            IterationElementType iteration) {
        ElementTypeBase sourceElement = source.element;
        WrappingDefinition wrapping = iteration.wrapping;
        if (wrapping != null) {
            if (sourceElement == wrapping.beginElement) {
                LanguageNodeBase iteratedNode = new LanguageNodeBase(iteration.iteratedElement, iterationNode);
                return getFirstPossibleLeafs(iteratedNode);
            }
            if (sourceElement == wrapping.endElement) {
                return getNextPossibleLeafs(iterationNode);
            }
        }

        if (iteration.separatorTokens != null) {
            for (LeafElementType separator : iteration.separatorTokens) {
                if (sourceElement == separator) {
                    LanguageNodeBase iteratedNode = new LanguageNodeBase(iteration.iteratedElement, iterationNode);
                    return getFirstPossibleLeafs(iteratedNode);
                }
            }
        }

        if (sourceElement != iteration.iteratedElement) return Set.of();
        Set<LanguageNodeBase> nextLeafs = new LinkedHashSet<>(getNextPossibleLeafs(iterationNode));
        if (wrapping != null) {
            nextLeafs.add(new LanguageNodeBase(wrapping.endElement, iterationNode));
        }
        if (iteration.separatorTokens == null) {
            LanguageNodeBase iteratedNode = new LanguageNodeBase(iteration.iteratedElement, iterationNode);
            nextLeafs.addAll(getFirstPossibleLeafs(iteratedNode));
        } else {
            for (LeafElementType separator : iteration.separatorTokens) {
                nextLeafs.add(new LanguageNodeBase(separator, iterationNode));
            }
        }
        return nextLeafs;
    }

    private Set<LanguageNodeBase> getNextPossibleLeafs(
            LanguageNodeBase source,
            LanguageNodeBase oneOfNode,
            OneOfElementType oneOf) {
        ElementTypeBase sourceElement = source.element;
        WrappingDefinition wrapping = oneOf.wrapping;
        if (wrapping != null && wrapping.optional) {
            if (sourceElement == wrapping.beginElement) {
                Set<LanguageNodeBase> leafs = new LinkedHashSet<>();
                for (ElementTypeRef child : oneOf.children) {
                    LanguageNodeBase childNode = new LanguageNodeBase(child.elementType, oneOfNode);
                    leafs.addAll(getFirstPossibleLeafs(childNode));
                }
                return leafs;
            }
            if (sourceElement == wrapping.endElement) {
                return getNextPossibleLeafs(oneOfNode);
            }
        }

        boolean alternativeComplete = false;
        for (ElementTypeRef child : oneOf.children) {
            if (sourceElement == child.elementType) {
                alternativeComplete = true;
                break;
            }
        }
        if (!alternativeComplete) return Set.of();

        Set<LanguageNodeBase> nextLeafs = new LinkedHashSet<>(getNextPossibleLeafs(oneOfNode));
        if (wrapping != null && wrapping.optional) {
            nextLeafs.add(new LanguageNodeBase(wrapping.endElement, oneOfNode));
        }
        return nextLeafs;
    }

    private Set<LanguageNodeBase> getNextPossibleLeafs(
            LanguageNodeBase source,
            LanguageNodeBase wrapperNode,
            WrapperElementType wrapper) {
        ElementTypeBase sourceElement = source.element;
        if (sourceElement == wrapper.getEndTokenElement()) {
            return getNextPossibleLeafs(wrapperNode);
        }
        if (sourceElement == wrapper.getBeginTokenElement()) {
            LanguageNodeBase wrappedNode = new LanguageNodeBase(wrapper.wrappedElement, wrapperNode);
            Set<LanguageNodeBase> nextLeafs = getFirstPossibleLeafs(wrappedNode);
            if (!wrapper.wrappedElementOptional) return nextLeafs;

            nextLeafs = new LinkedHashSet<>(nextLeafs);
            nextLeafs.add(new LanguageNodeBase(wrapper.getEndTokenElement(), wrapperNode));
            return nextLeafs;
        }
        if (sourceElement == wrapper.wrappedElement) {
            return Set.of(new LanguageNodeBase(wrapper.getEndTokenElement(), wrapperNode));
        }
        return Set.of();
    }

    private Set<LanguageNodeBase> getNextPossibleLeafs(
            LanguageNodeBase source,
            LanguageNodeBase identifierNode,
            QualifiedIdentifierElementType identifier) {
        ElementTypeBase sourceElement = source.element;
        Set<LanguageNodeBase> nextLeafs = new LinkedHashSet<>();
        boolean identifierComplete = false;
        for (LeafElementType[] variant : identifier.variants) {
            for (int i = 0; i < variant.length; i++) {
                if (sourceElement == identifier.separatorToken && i > 0) {
                    nextLeafs.add(new LanguageNodeBase(variant[i], identifierNode));
                } else if (sourceElement == variant[i]) {
                    if (i < variant.length - 1) {
                        nextLeafs.add(new LanguageNodeBase(identifier.separatorToken, identifierNode));
                    } else {
                        identifierComplete = true;
                    }
                }
            }
        }
        if (identifierComplete) {
            nextLeafs.addAll(getNextPossibleLeafs(identifierNode));
        }
        return nextLeafs;
    }
}
