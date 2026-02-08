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
            if (variant.unambiguous) {
                children.addAll(variant.elements);
                continue;
            }

            if (variant.leafs.equals(variant.elements)) {
                children.addAll(variant.elements);
                continue;
            }

            // build "sequence (token, one-of)"
            SequenceElementType sequenceElementType = new SequenceElementType(bundle, subject, nextId());
            sequenceElementType.surrogate = true;
            children.add(sequenceElementType);

            ElementTypeBase firstSequenceElement;
            ElementTypeBase secondSequenceElement;
            if (variant.tokens.size() > 1) {
                List<LeafElementType> tokenElementTypes = new ArrayList<>(variant.tokens.size());
                for (TokenType tokenType : variant.tokens) {
                    TokenElementType tokenELementType = new TokenElementType(bundle, sequenceElementType, tokenType, nextId(), "");
                    tokenELementType.surrogate = true;
                    tokenELementType.surrogateFor = variant.getLeafs(tokenType);
                    tokenELementType.startSurrogateFor = variant.elements;
                    tokenElementTypes.add(tokenELementType);
                }
                OneOfElementType oneOfElementType = new OneOfElementType(bundle, sequenceElementType, nextId(), "");
                oneOfElementType.setElements(tokenElementTypes);
                oneOfElementType.surrogate = true;
                oneOfElementType.basic = true;
                firstSequenceElement = oneOfElementType;

            } else {
                LeafElementType leafElementType = variant.getFirstLeaf();
                TokenElementType tokenELementType = new TokenElementType(bundle, sequenceElementType, leafElementType.tokenType, nextId(), "");
                tokenELementType.surrogate = true;
                tokenELementType.surrogateFor = variant.leafs;
                tokenELementType.startSurrogateFor = variant.elements;
                firstSequenceElement = tokenELementType;
            }

            if (variant.elements.size() > 1) {
                OneOfElementType oneOfElementType = new OneOfElementType(bundle, sequenceElementType, nextId(), "");
                oneOfElementType.setElements(variant.elements);
                oneOfElementType.surrogate = true;
                secondSequenceElement = oneOfElementType;
            } else {
                secondSequenceElement = variant.getFirstElement();
            }

            sequenceElementType.setElements(List.of(firstSequenceElement, secondSequenceElement));
        }
        subject.setElements(children);
    }

    private String nextId() {
        return subject.id + "." + subject.idSuffix.incrementAndGet();
    }

    @Nullable
    private List<PathVariant> findAmbiguousPaths() {
        Map<TokenType, PathVariantMap> paths = new LinkedHashMap<>();
        Map<TokenType, PathVariantMap>  ambiguousPaths = new LinkedHashMap<>();

        for (ElementTypeRef child : subject.children) {
            Set<LeafElementType> possibleLeafs = child.elementType.cache.getFirstPossibleLeafs();
            possibleLeafs = unwrapSurrogates(possibleLeafs);
            for (LeafElementType leaf : possibleLeafs) {
                TokenType tokenType = leaf.tokenType;

                PathVariantMap leafMappings = paths.computeIfAbsent(tokenType, t -> new PathVariantMap());
                leafMappings.put(leaf, child.elementType);

                if (leafMappings.size() > 1) {
                    ambiguousPaths.put(tokenType, leafMappings);
                }
            }
        }
        if (ambiguousPaths.isEmpty()) return null;
        Set<ElementTypeBase> ambiguousElements = new HashSet<>();

        // ambiguous paths (one token to many elements)
        List<PathVariant> pathVariants = new ArrayList<>();
        for (TokenType tokenType : ambiguousPaths.keySet()) {
            PathVariantMap mappings = ambiguousPaths.get(tokenType);
            ambiguousElements.addAll(mappings.values());

            PathVariant pathVariant = new PathVariant(mappings.keySet(), mappings.values());
            pathVariants.add(pathVariant);
            paths.remove(tokenType);
        }

        Map<ElementTypeBase, Set<LeafElementType>> groupedPaths = new LinkedHashMap<>();
        for (TokenType tokenType : paths.keySet()) {
            PathVariantMap pathVariantMap = paths.get(tokenType);
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

    private static Set<LeafElementType> unwrapSurrogates(Set<LeafElementType> leafs) {
        Set<LeafElementType> collector = new  HashSet<>();
        for (LeafElementType leaf : leafs) {
            unwrapSurrogate(leaf, collector);
        }
        return collector;
    }

    private static void unwrapSurrogate(LeafElementType leafElementType, Set<LeafElementType> collector) {
        Set<LeafElementType> surrogateFor = leafElementType.surrogateFor;
        if (surrogateFor == null) {
            collector.add(leafElementType);
            return;
        }

        for (LeafElementType elementType : surrogateFor) {
            unwrapSurrogate(elementType, collector);
        }
    }

    private static class PathVariantMap extends LinkedHashMap<LeafElementType, ElementTypeBase> {
        public LeafElementType firstKey() {
            return keySet().iterator().next();
        }

        public ElementTypeBase firstValue() {
            return get(firstKey());
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
