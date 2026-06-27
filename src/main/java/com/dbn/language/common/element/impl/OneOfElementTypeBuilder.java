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
import com.dbn.language.common.element.ElementTypeBundle.Builder;
import org.jdom.Element;

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
import static com.dbn.language.common.element.util.ElementTypeAttribute.OPTIONAL_WRAPPING;
import static com.dbn.language.common.element.util.ElementTypeAttribute.SURROGATE_LEAD;
import static com.dbn.language.common.element.util.ElementTypeAttribute.SURROGATE_SEQUENCE;
import static java.util.stream.Collectors.toCollection;

/**
 * Legacy one-of tree rewrite support. Trie-based parser extensions supersede this path.
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("removal")
public class OneOfElementTypeBuilder {
    /**
     * Legacy switch used by parser tooling while trie extension generation still needs the raw tree.
     */
    @Deprecated(forRemoval = true)
    public static boolean legacyAmbiguousPathRebuildEnabled = true; // disabled by dev tooling that needs the raw tree

    private final OneOfElementType subject;

    private OneOfElementTypeBuilder(OneOfElementType subject) {
        this.subject = subject;
    }

    @Deprecated(forRemoval = true)
    public static void rebuildAmbiguousPaths(OneOfElementType subject) {
        if (!subject.bundle.legacyParser) return;

        OneOfElementTypeBuilder builder = new OneOfElementTypeBuilder(subject);
        builder.rebuildAmbiguousPaths();
    }

    private void rebuildAmbiguousPaths() {
        if (!legacyAmbiguousPathRebuildEnabled) return;
        if (subject.basic) return; // all elements are distinct tokens
        if (!subject.ambiguous && !Builder.rebuilding) return;

        List<PathVariant> paths = findAmbiguousPaths();
        if (paths == null || paths.isEmpty()) {
            updateDefinition(false);
            return;
        }

        updateDefinition(true);
        rebuildAmbiguousPaths(paths);
    }

    private void updateDefinition(boolean ambiguous) {
        if (subject.ambiguous == ambiguous) return;
        if (!Builder.rebuilding) return;

        subject.ambiguous = ambiguous;
        Builder builder = subject.bundle.getBuilder();
        builder.setDirty(true);
        Element definition = builder.getDefinition(subject);
        if (ambiguous) {
            definition.setAttribute("ambiguous", "true");
        } else {
            definition.removeAttribute("ambiguous");
        }
    }

    private void rebuildAmbiguousPaths(List<PathVariant> pathVariants) {
        ElementTypeBundle bundle = subject.bundle;
        List<ElementTypeBase> children = new ArrayList<>(subject.children.length);
        for (PathVariant variant : pathVariants) {
            Set<TokenType> tokens = variant.tokens;
            Set<ElementTypeBase> elements = variant.mainElements;

            if (!variant.ambiguous) {
                children.addAll(elements);
                continue;
            }

            if (variant.leadElements.equals(elements)) {
                // this only happens on ambiguous "identifier one-of" elements
                // TODO fix all occurrences of this use-case in the parser definitions and delete this block
                children.addAll(elements);
                continue;
            }

            // build "sequence (token, one-of)"
            SequenceElementType sequenceElement = new SurrogateSequenceElementType(bundle, subject, nextId());
            sequenceElement.set(SURROGATE_SEQUENCE, true);
            children.add(sequenceElement);

            ElementTypeBase leadElement = tokens.size() == 1 ?
                    createSimpleLeadElement(variant, sequenceElement) :
                    createCompositeLeadElement(variant, sequenceElement);

            ElementTypeBase mainElement = elements.size() == 1 ?
                    createSimpleMainElement(variant, sequenceElement) :
                    createCompositeMainElement(variant, sequenceElement);

            sequenceElement.setElements(List.of(leadElement, mainElement));
        }
        subject.setElements(children);
        subject.sortChildren();
    }

    private ElementTypeBase createSimpleLeadElement(PathVariant variant, SequenceElementType parent) {
        TokenType tokenType = variant.tokens.iterator().next();
        return createLeadElement(parent, tokenType, variant.leadElements);
    }

    private ElementTypeBase createCompositeLeadElement(PathVariant variant, SequenceElementType parent) {
        List<LeafElementType> leadElements = new ArrayList<>(variant.tokens.size());
        for (TokenType tokenType : variant.tokens) {
            Set<LeafElementType> leafs = variant.getLeafs(tokenType);

            LeafElementType leadElement = createLeadElement(parent, tokenType, leafs);
            leadElements.add(leadElement);
        }
        SurrogateOneOfElementType leadElement = new SurrogateOneOfElementType(parent, nextId());
        leadElement.setElements(leadElements);
        leadElement.sortable = subject.sortable;
        leadElement.basic = true;
        return leadElement;
    }

    private LeafElementType createLeadElement(SequenceElementType parent, TokenType tokenType, Set<LeafElementType> leafs) {
        LeafElementType leadElement = tokenType.isIdentifier() ?
                new IdentifierElementType(parent, nextId()) :
                new TokenElementType(parent, tokenType, nextId());
        leadElement.surrogateFor = unwrapSurrogates(leafs);
        for (LeafElementType surrogateFor : leadElement.surrogateFor) {
            if (surrogateFor.surrogatedBy == null) surrogateFor.surrogatedBy = new LinkedHashSet<>();
            surrogateFor.surrogatedBy.add(leadElement);
        }

        leadElement.set(SURROGATE_LEAD, true);
        return leadElement;
    }

    private static ElementTypeBase createSimpleMainElement(PathVariant variant, SequenceElementType parent) {
        ElementTypeBase mainElement = variant.mainElements.iterator().next();
        if (mainElement.wrapping != null && mainElement.wrapping.optional) {
            parent.wrapping = mainElement.wrapping;
        }

        //secondSequenceElement.changeParent(subject, sequenceElementType);
        return mainElement;
    }

    private ElementTypeBase createCompositeMainElement(PathVariant variant, SequenceElementType parent) {
        Set<ElementTypeBase> elements = variant.mainElements;
        OneOfElementType mainElement = new OneOfElementType(parent, nextId());
        mainElement.setElements(elements);
        mainElement.sortable = subject.sortable;
        mainElement.sortChildren();
        for (ElementTypeBase element : elements) {
            if (element.wrapping != null && element.wrapping.optional) {
                mainElement.wrapping = element.wrapping;
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
        return mainElement;
    }

    private String nextId() {
        return subject.nextChildId();
    }

    private List<PathVariant> findAmbiguousPaths() {
        Map<TokenType, PathVariantMappings> paths = new LinkedHashMap<>();
        Map<TokenType, PathVariantMappings> ambiguousPaths = new LinkedHashMap<>();

        for (ElementTypeRef child : subject.children) {
            Set<LeafElementType> possibleLeafs = child.elementType.cache.getFirstPossibleLeafs();
            for (LeafElementType leaf : possibleLeafs) {
                if (leaf.is(OPTIONAL_WRAPPING)) continue;
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

        // ambiguous paths (many tokens leading to many elements)
        Map<Set<ElementTypeBase>, PathVariant> ambiguousPathVariants = new LinkedHashMap<>();
        for (TokenType tokenType : ambiguousPaths.keySet()) {
            PathVariantMappings mappings = ambiguousPaths.get(tokenType);
            Set<LeafElementType> leafs = mappings.leafs();
            Set<ElementTypeBase> elements = mappings.elements();

            PathVariant pathVariant = ambiguousPathVariants.computeIfAbsent(elements, e -> new PathVariant(e));
            pathVariant.ambiguous = true;
            pathVariant.addLeafs(leafs);

            paths.remove(tokenType);
            ambiguousElements.addAll(elements);
        }

        // unambiguous paths (many tokens leading to one element)
        Map<ElementTypeBase, PathVariant> unambiguousPathVariants = new LinkedHashMap<>();
        for (TokenType tokenType : paths.keySet()) {
            PathVariantMappings mappings = paths.get(tokenType);
            ElementTypeBase element = mappings.firstValue();
            PathVariant pathVariant = unambiguousPathVariants.computeIfAbsent(element, e -> new PathVariant(e));
            pathVariant.addLeaf(mappings.firstKey());

            // paths where leading token does not appear more than once, nor does the one-of child element
            pathVariant.ambiguous = ambiguousElements.contains(element);
        }

        List<PathVariant> pathVariants = new ArrayList<>(ambiguousPathVariants.size() + unambiguousPathVariants.size());
        pathVariants.addAll(ambiguousPathVariants.values());
        pathVariants.addAll(unambiguousPathVariants.values());
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
        private boolean ambiguous;
        private final Set<TokenType> tokens = new LinkedHashSet<>();
        private final Set<LeafElementType> leadElements = new LinkedHashSet<>();
        private final Set<ElementTypeBase> mainElements = new LinkedHashSet<>();

        public PathVariant(ElementTypeBase mainElement) {
            mainElements.add(mainElement);
        }

        public PathVariant(Collection<ElementTypeBase> mainElements) {
            this.mainElements.addAll(mainElements);
        }

        public Set<LeafElementType> getLeafs(TokenType tokenType) {
            return leadElements.stream().filter(l -> l.tokenType == tokenType).collect(Collectors.toSet());
        }

        public void addLeafs(Collection<LeafElementType> leafs) {
            leafs.forEach(leaf -> addLeaf(leaf));
        }
        public void addLeaf(LeafElementType leafs) {
            this.leadElements.add(leafs);
            this.tokens.add(leafs.tokenType);
        }

        @Override
        public String toString() {
            return tokens + " " + mainElements;
        }

    }
}
