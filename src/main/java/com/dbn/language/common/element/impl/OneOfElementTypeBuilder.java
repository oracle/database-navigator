/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.language.common.element.impl;

import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ElementTypeBundle;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dbn.language.common.element.parser.TokenMonitor.unwrapSurrogates;
import static com.dbn.language.common.element.util.ElementTypeAttribute.WRAPPING_TOKEN;
import static java.util.stream.Collectors.toCollection;

public class OneOfElementTypeBuilder {
    private final OneOfElementType subject;

    private OneOfElementTypeBuilder(OneOfElementType subject) {
        this.subject = subject;
    }

    public static void rebuildAmbiguousPaths(OneOfElementType subject) {
        OneOfElementTypeBuilder builder = new OneOfElementTypeBuilder(subject);
        builder.rebuildAmbiguousPaths();
    }

    private void rebuildAmbiguousPaths() {
        if (subject.basic) return; // all elements are distinct tokens

        List<PathVariant> paths = findAmbiguousPaths();
        if (paths == null) return;
        if (paths.isEmpty()) return;

        rebuildAmbiguousPaths(paths);
    }

    private void rebuildAmbiguousPaths(List<PathVariant> pathVariants) {
        ElementTypeBundle bundle = subject.bundle;
        List<ElementTypeBase> children = new ArrayList<>(subject.children.length);
        for (PathVariant variant : pathVariants) {
            Set<TokenType> tokens = variant.tokens;
            Set<ElementTypeBase> elements = variant.elements;

            if (variant.unambiguous) {
                children.addAll(elements);
                continue;
            }

            if (variant.leafs.equals(elements)) {
                // this only happens on ambiguous "identifier one-of" elements
                // TODO fix all occurrences of this use-case in the parser definitions and delete this block
                children.addAll(elements);
                continue;
            }

            // build "sequence (token, one-of)"
            SequenceElementType sequenceElementType = new SurrogateSequenceElementType(bundle, subject, nextId());
            children.add(sequenceElementType);

            ElementTypeBase firstSequenceElement;
            ElementTypeBase secondSequenceElement;

            if (tokens.size() > 1) {
                List<LeafElementType> tokenElementTypes = new ArrayList<>(tokens.size());
                for (TokenType tokenType : tokens) {
                    LeafElementType surrogateLeafElementType = tokenType.isIdentifier() ?
                            new IdentifierElementType(sequenceElementType, nextId()) :
                            new TokenElementType(sequenceElementType, tokenType, nextId());
                    surrogateLeafElementType.surrogate = true;
                    surrogateLeafElementType.surrogateFor = unwrapSurrogates(variant.getLeafs(tokenType));
                    tokenElementTypes.add(surrogateLeafElementType);
                }
                OneOfElementType oneOfElementType = new OneOfElementType(sequenceElementType, nextId());
                oneOfElementType.setElements(tokenElementTypes);
                oneOfElementType.surrogate = true;
                oneOfElementType.sortable = subject.sortable;
                oneOfElementType.basic = true;
                firstSequenceElement = oneOfElementType;

            } else {
                LeafElementType leafElementType = variant.getFirstLeaf();
                TokenType tokenType = leafElementType.tokenType;

                LeafElementType surrogateLeafElementType = tokenType.isIdentifier() ?
                        new IdentifierElementType(sequenceElementType, nextId()) :
                        new TokenElementType(sequenceElementType, tokenType, nextId());

                surrogateLeafElementType.surrogate = true;
                surrogateLeafElementType.surrogateFor = unwrapSurrogates(variant.leafs);
                firstSequenceElement = surrogateLeafElementType;
            }

            if (elements.size() > 1) {
                OneOfElementType oneOfElementType = new OneOfElementType(sequenceElementType, nextId());
                oneOfElementType.setElements(elements);
                oneOfElementType.surrogate = true;
                oneOfElementType.sortable = subject.sortable;
                oneOfElementType.sortChildren();
                secondSequenceElement = oneOfElementType;
                for (ElementTypeBase element : elements) {
                    if (element.wrapping != null) {
                        secondSequenceElement.wrapping = element.wrapping;
                    }
                }
/*
                // TODO verify if changing the parents is really needed
                // Complication: unnamed element types will potentially appear in multiple nodes,
                //   which would require multiple "parent" associations)

                for (ElementTypeBase element : elements) {
                    element.changeParent(subject, oneOfElementType);
                }
*/
            } else {
                secondSequenceElement = variant.getFirstElement();
                sequenceElementType.wrapping = secondSequenceElement.wrapping;
                //secondSequenceElement.changeParent(subject, sequenceElementType);
            }

            sequenceElementType.setElements(List.of(firstSequenceElement, secondSequenceElement));
        }
        subject.setElements(children);
        subject.sortChildren();
    }

    private String nextId() {
        return subject.nextChildId();
    }

    @Nullable
    private List<PathVariant> findAmbiguousPaths() {
        Map<TokenType, PathVariantMappings> paths = new LinkedHashMap<>();
        Map<TokenType, PathVariantMappings> ambiguousPaths = new LinkedHashMap<>();

        for (ElementTypeRef child : subject.children) {
            Set<LeafElementType> possibleLeafs = child.elementType.cache.getFirstPossibleLeafs();
            for (LeafElementType leaf : possibleLeafs) {
                if (leaf.is(WRAPPING_TOKEN)) continue;
                TokenType tokenType = leaf.tokenType;

                PathVariantMappings leafMappings = paths.computeIfAbsent(tokenType, t -> new PathVariantMappings());
                leafMappings.put(leaf, child.elementType);

                if (leafMappings.ambiguous) {
                    ambiguousPaths.put(tokenType, leafMappings);
                }
            }
        }
        if (ambiguousPaths.isEmpty()) return null;
        Set<ElementTypeBase> ambiguousElements = new HashSet<>();

        // ambiguous paths (one token to many elements)
        List<PathVariant> pathVariants = new ArrayList<>();
        for (TokenType tokenType : ambiguousPaths.keySet()) {
            PathVariantMappings mappings = ambiguousPaths.get(tokenType);
            Set<LeafElementType> leafs = mappings.leafs();
            Set<ElementTypeBase> elements = mappings.elements();

            PathVariant pathVariant = new PathVariant(leafs, elements);
            pathVariants.add(pathVariant);
            paths.remove(tokenType);

            ambiguousElements.addAll(elements);
        }

        Map<ElementTypeBase, Set<LeafElementType>> groupedPaths = new LinkedHashMap<>();
        for (TokenType tokenType : paths.keySet()) {
            PathVariantMappings pathVariantMap = paths.get(tokenType);
            ElementTypeBase elementType = pathVariantMap.firstValue();
            Set<LeafElementType> leafElementTypes = groupedPaths.computeIfAbsent(elementType, t -> new LinkedHashSet<>());
            leafElementTypes.add(pathVariantMap.firstKey());
        }

        // unambiguous paths (one-to-many keys to one element)
        for (ElementTypeBase elementType : groupedPaths.keySet()) {
            Set<LeafElementType> leafElementTypes = groupedPaths.get(elementType);
            PathVariant pathVariant = new PathVariant(leafElementTypes, Set.of(elementType));

            // paths where leading token does not appear more than once, nor does the one-of child element
            pathVariant.unambiguous = !ambiguousElements.contains(elementType);
            pathVariants.add(pathVariant);
        }

        return pathVariants;
    }

    private static class PathVariantMappings {
        private boolean ambiguous;
        Map<LeafElementType, Set<ElementTypeBase>> elements = new LinkedHashMap<>();

        public void put(LeafElementType leaf, ElementTypeBase elementType) {
            Set<ElementTypeBase> elements = this.elements.computeIfAbsent(leaf, t -> new LinkedHashSet<>());
            elements.add(elementType);
            ambiguous = elements.size() > 1 || this.elements.size() > 1;
        }

        public Set<LeafElementType> leafs() {
            return elements.keySet();
        }

        public Set<ElementTypeBase> elements() {
            return elements
                    .values()
                    .stream()
                    .flatMap(e -> e.stream())
                    .collect(toCollection(() -> new LinkedHashSet<>()));
        }

        public ElementTypeBase firstValue() {
            return elements.values().iterator().next().iterator().next();
        }

        public LeafElementType firstKey() {
            return elements.keySet().iterator().next();
        }
    }

    private static class PathVariant {
        private boolean unambiguous;
        private final Set<TokenType> tokens = new LinkedHashSet<>();
        private final Set<LeafElementType> leafs = new LinkedHashSet<>();
        private final Set<ElementTypeBase> elements = new LinkedHashSet<>();

        public PathVariant(Collection<LeafElementType> leafs, Collection<ElementTypeBase> elements) {
            this.leafs.addAll(leafs);
            this.leafs.forEach(leaf -> tokens.add(leaf.tokenType));
            this.elements.addAll(elements);
        }

        public LeafElementType getFirstLeaf() {
            return leafs.iterator().next();
        }

        public ElementTypeBase getFirstElement() {
            return elements.iterator().next();
        }

        public Set<LeafElementType> getLeafs(TokenType tokenType) {
            return leafs.stream().filter(l -> l.tokenType == tokenType).collect(Collectors.toSet());
        }

        @Override
        public String toString() {
            return tokens.toString() + " " + elements.toString();
        }
    }
}
