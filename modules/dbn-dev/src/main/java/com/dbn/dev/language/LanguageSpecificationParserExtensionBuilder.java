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

import com.dbn.common.util.Lists;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.NamedElementType;
import com.dbn.language.common.element.impl.OneOfElementType;
import com.dbn.language.common.element.impl.SequenceElementType;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.impl.WrapperElementType;
import org.jdom.Comment;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dbn.dev.language.LanguageSpecificationXmlUtil.outputPrettyString;
import static com.dbn.language.common.element.util.ElementTypeAttribute.OPTIONAL_WRAPPING;

public class LanguageSpecificationParserExtensionBuilder implements LanguageSpecificationArtifactBuilder {
    private static final String EXT_DTD_PATH = "../../../common/definition/language-parser-elements-ext.dtd";
    private static final String ATTR_TOKEN_TYPE_IDS = "token-type-ids";
    private static final String ATTR_CANDIDATE_IDS = "candidate-ids";
    private static final String TAG_NODE = "node";
    private static final int ELEMENT_LOG_INTERVAL = 25;
    private static final int ONE_OF_LOG_INTERVAL = 50;
    private static final int MAX_NEXT_TOKEN_LOOK_THROUGH_DEPTH = 12;

    private final LanguageSpecificationBuilderInput input;
    private ElementTypeBundle bundle;
    private int processedElements;
    private int visitedElements;
    private int analyzedOneOfs;
    private int emittedOneOfs;

    public LanguageSpecificationParserExtensionBuilder(LanguageSpecificationBuilderInput input) {
        this.input = input;
    }

    @Override
    public void build() throws Exception {
        System.out.println("Reading " + input.getParserElementsFile().toPath());
        new LanguageSpecificationParserBundleLoader(input).load(this::buildExtension, false, false);
    }

    private void buildExtension(ElementTypeBundle bundle, ElementTypeBundle.Builder builder) {
        try {
            this.bundle = bundle;
            processedElements = 0;
            visitedElements = 0;
            analyzedOneOfs = 0;
            emittedOneOfs = 0;

            Document definitionDocument = builder.getDefinitionDocument();
            Element definitionRoot = definitionDocument.getRootElement();
            List<Element> elementDefs = definitionRoot.getChildren("element-def");
            System.out.println("Building parser extension definition for " + elementDefs.size() + " named elements");

            Element extensionRoot = new Element("parser-element-extensions");
            extensionRoot.setAttribute("language", definitionRoot.getAttributeValue("language"));
            extensionRoot.setAttribute("source", input.getParserElementsFile().getName());

            for (Element elementDef : elementDefs) {
                String elementId = elementDef.getAttributeValue("id");
                processedElements++;
                if (processedElements == 1 || processedElements % ELEMENT_LOG_INTERVAL == 0) {
                    System.out.println("Processing named element " + processedElements + "/" + elementDefs.size() + ": " + elementId);
                }

                NamedElementType elementType = elementId == null ? null : getNamedElementType(elementId);
                if (elementType != null) {
                    collectOneOfExtensions(elementType, elementId, extensionRoot, new HashSet<>());
                } else {
                    System.out.println("Skipping unresolved named element: " + elementId);
                }
            }

            System.out.println("Parser extension analysis finished. Visited=" + visitedElements +
                    ", one-of analyzed=" + analyzedOneOfs +
                    ", one-of emitted=" + emittedOneOfs);

            Document extensionDocument = new Document(extensionRoot);
            extensionDocument.addContent(0, new DocType("parser-element-extensions", EXT_DTD_PATH));
            copyCopyright(definitionDocument, extensionDocument);

            File extensionFile = input.getParserElementsExtensionFile();
            System.out.println("Writing " + extensionFile.toPath());
            Files.writeString(extensionFile.toPath(), outputPrettyString(extensionDocument), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not build parser extension definition", e);
        } finally {
            this.bundle = null;
        }
    }

    private NamedElementType getNamedElementType(String elementId) {
        return bundle == null ? null : bundle.getNamedElementType(elementId);
    }

    private static void copyCopyright(Document sourceDocument, Document extensionDocument) {
        for (Object content : sourceDocument.getContent()) {
            if (content instanceof Comment comment && comment.getText().contains("Copyright")) {
                extensionDocument.addContent(0, comment.clone());
                return;
            }
        }
    }

    private void collectOneOfExtensions(ElementTypeBase elementType, String contextElementId, Element extensionRoot, Set<ElementTypeBase> visited) {
        if (!visited.add(elementType)) return;
        visitedElements++;

        if (elementType instanceof OneOfElementType oneOfElementType) {
            Element extension = buildOneOfExtension(oneOfElementType, contextElementId);
            if (extension != null) {
                extensionRoot.addContent(extension);
                emittedOneOfs++;
            }
        }

        if (elementType instanceof NamedElementType namedElementType && !namedElementType.getId().equals(contextElementId)) {
            visited.remove(elementType);
            return;
        }

        for (ElementTypeRef child : getChildren(elementType)) {
            collectOneOfExtensions(child.elementType, contextElementId, extensionRoot, visited);
        }

        visited.remove(elementType);
    }

    private Element buildOneOfExtension(OneOfElementType oneOfElementType, String contextElementId) {
        ElementTypeRef[] children = oneOfElementType.children;
        if (children.length < 2) return null;
        analyzedOneOfs++;
        if (analyzedOneOfs == 1 || analyzedOneOfs % ONE_OF_LOG_INTERVAL == 0) {
            System.out.println("Analyzing one-of " + analyzedOneOfs + ": " + contextElementId + "/" + oneOfElementType.getId() +
                    " children=" + children.length);
        }

        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < children.length; i++) {
            ElementTypeRef child = children[i];
            Candidate candidate = new Candidate(i, child.elementType);
            candidates.add(candidate);
        }

        Map<String, TokenCandidates> candidatesByToken = collectFirstTokenCandidates(candidates);
        addOptionalWrappingCandidates(candidatesByToken, oneOfElementType, candidates);
        if (!hasAmbiguousToken(candidatesByToken)) return null;

        Element extension = new Element("one-of-extension");
        extension.setAttribute("id", oneOfElementType.getId());
        TrieBuildContext context = new TrieBuildContext();
        boolean added = writeTokenNodes(extension, candidatesByToken, true, context);
        if (!added) return null;

        extension.setAttribute("depth", Integer.toString(context.maxDepth));
        return extension;
    }

    private Map<String, TokenCandidates> collectFirstTokenCandidates(List<Candidate> candidates) {
        Map<String, TokenCandidates> candidatesByToken = new LinkedHashMap<>();
        for (Candidate candidate : candidates) {
            addFirstTokenCandidates(candidatesByToken, candidate, candidate.elementType);
        }
        return candidatesByToken;
    }

    private void addOptionalWrappingCandidates(
            Map<String, TokenCandidates> candidatesByToken,
            OneOfElementType oneOfElementType,
            List<Candidate> candidates) {
        if (oneOfElementType.wrapping == null || !oneOfElementType.wrapping.optional) return;

        String beginTokenId = oneOfElementType.wrapping.beginElement.tokenType.getId();
        for (Candidate candidate : candidates) {
            ElementTypeBase elementType = candidate.elementType;
            if (elementType instanceof WrapperElementType wrapperElementType &&
                    wrapperElementType.getBeginTokenElement().tokenType.getId().equals(beginTokenId)) {
                addFirstTokenCandidates(candidatesByToken, candidate, wrapperElementType.wrappedElement);
            } else {
                addFirstTokenCandidates(candidatesByToken, candidate, elementType);
            }
        }
    }

    private static void addFirstTokenCandidates(
            Map<String, TokenCandidates> candidatesByToken,
            Candidate candidate,
            ElementTypeBase elementType) {
        for (LeafElementType leaf : elementType.cache.getFirstPossibleLeafs()) {
            if (leaf.is(OPTIONAL_WRAPPING)) continue;

            TokenType tokenType = leaf.tokenType;
            TokenCandidates tokenCandidates = candidatesByToken.computeIfAbsent(tokenType.getId(), k -> new TokenCandidates());
            tokenCandidates.addCandidate(candidate);
        }
    }

    private boolean writeTokenNodes(
            Element parent,
            Map<String, TokenCandidates> candidatesByToken,
            boolean includeUnambiguous,
            TrieBuildContext context) {
        List<Element> elements = new ArrayList<>();
        for (Map.Entry<String, TokenCandidates> entry : candidatesByToken.entrySet()) {
            TokenCandidates tokenCandidates = entry.getValue();
            boolean ambiguous = tokenCandidates.size() > 1;
            if (!includeUnambiguous && !ambiguous) continue;

            Element element = new Element(TAG_NODE);
            context.registerTokenNode(1);
            element.setAttribute(ATTR_TOKEN_TYPE_IDS, entry.getKey());
            writeCandidateIdsAttribute(element, tokenCandidates);
            writeNextTokenNodes(element, entry.getKey(), tokenCandidates, context);
            elements.add(element);
        }

        for (Element element : compactTokenNodes(elements)) {
            parent.addContent(element);
        }
        return !elements.isEmpty();
    }

    private boolean hasAmbiguousToken(Map<String, TokenCandidates> candidatesByToken) {
        for (TokenCandidates tokenCandidates : candidatesByToken.values()) {
            if (tokenCandidates.size() > 1) return true;
        }
        return false;
    }

    private static void writeNextTokenNodes(
            Element parent,
            String firstTokenId,
            TokenCandidates tokenCandidates,
            TrieBuildContext context) {
        if (tokenCandidates.size() < 2) return;

        Map<String, TokenCandidates> candidatesByToken = collectNextTokenCandidates(firstTokenId, tokenCandidates);
        if (candidatesByToken.isEmpty()) return;

        List<Element> elements = new ArrayList<>();
        for (Map.Entry<String, TokenCandidates> entry : candidatesByToken.entrySet()) {
            Element element = new Element(TAG_NODE);
            context.registerTokenNode(2);
            element.setAttribute(ATTR_TOKEN_TYPE_IDS, entry.getKey());
            writeCandidateIdsAttribute(element, entry.getValue());
            elements.add(element);
        }

        for (Element element : compactTokenNodes(elements)) {
            parent.addContent(element);
        }
    }

    private static Map<String, TokenCandidates> collectNextTokenCandidates(String firstTokenId, TokenCandidates tokenCandidates) {
        Map<String, TokenCandidates> candidatesByToken = new LinkedHashMap<>();
        for (Candidate candidate : tokenCandidates) {
            Set<String> tokenIds = getNextTokenIds(candidate.elementType, firstTokenId);
            for (String tokenId : tokenIds) {
                TokenCandidates tokenCandidatesById = candidatesByToken.computeIfAbsent(tokenId, k -> new TokenCandidates());
                tokenCandidatesById.addCandidate(candidate);
            }
        }
        return candidatesByToken;
    }

    private static Set<String> getNextTokenIds(ElementTypeBase elementType, String firstTokenId) {
        NextTokenMatch match = getNextTokenMatch(elementType, firstTokenId, new HashSet<>(), 0);
        return match.tokenIds;
    }

    private static NextTokenMatch getNextTokenMatch(
            ElementTypeBase elementType,
            String firstTokenId,
            Set<ElementTypeBase> visiting,
            int depth) {
        if (elementType == null || depth > MAX_NEXT_TOKEN_LOOK_THROUGH_DEPTH) return NextTokenMatch.empty();
        if (!visiting.add(elementType)) return NextTokenMatch.empty();

        try {
            if (elementType instanceof LeafElementType leafElementType) {
                return isTokenMatch(leafElementType, firstTokenId) ?
                        NextTokenMatch.completed() :
                        NextTokenMatch.empty();
            }

            if (elementType instanceof SequenceElementType sequenceElementType) {
                return getSequenceNextTokenMatch(sequenceElementType.children, firstTokenId, visiting, depth);
            }

            if (elementType instanceof OneOfElementType oneOfElementType) {
                NextTokenMatch result = new NextTokenMatch();
                for (ElementTypeRef child : oneOfElementType.children) {
                    result.add(getNextTokenMatch(child.elementType, firstTokenId, visiting, depth + 1));
                }
                return result;
            }

            if (elementType instanceof WrapperElementType wrapperElementType) {
                NextTokenMatch result = new NextTokenMatch();
                if (isTokenMatch(wrapperElementType.getBeginTokenElement(), firstTokenId)) {
                    addFirstPossibleTokenIds(result.tokenIds, wrapperElementType.wrappedElement);
                    if (wrapperElementType.wrappedElementOptional) {
                        result.tokenIds.add(wrapperElementType.getEndTokenElement().tokenType.getId());
                    }
                }
                result.add(getNextTokenMatch(wrapperElementType.wrappedElement, firstTokenId, visiting, depth + 1));
                return result;
            }

            if (elementType instanceof IterationElementType iterationElementType) {
                NextTokenMatch result = getNextTokenMatch(iterationElementType.iteratedElement, firstTokenId, visiting, depth + 1);
                if (result.completed) {
                    addSeparatorTokenIds(result.tokenIds, iterationElementType);
                    result.completed = iterationElementType.minIterations <= 1;
                }
                return result;
            }

            return NextTokenMatch.empty();
        } finally {
            visiting.remove(elementType);
        }
    }

    private static NextTokenMatch getSequenceNextTokenMatch(
            ElementTypeRef[] children,
            String firstTokenId,
            Set<ElementTypeBase> visiting,
            int depth) {
        NextTokenMatch result = new NextTokenMatch();
        for (int i = 0; i < children.length; i++) {
            ElementTypeRef child = children[i];
            NextTokenMatch childMatch = getNextTokenMatch(child.elementType, firstTokenId, visiting, depth + 1);
            result.addTokenIds(childMatch);

            if (childMatch.completed) {
                addFirstPossibleTokenIds(result.tokenIds, children, i + 1);
                if (allOptional(children, i + 1)) {
                    result.completed = true;
                }
            }

            if (!child.optional) break;
        }
        return result;
    }

    private static void addFirstPossibleTokenIds(Set<String> tokenIds, ElementTypeRef[] children, int startIndex) {
        for (int i = startIndex; i < children.length; i++) {
            ElementTypeRef child = children[i];
            addFirstPossibleTokenIds(tokenIds, child.elementType);
            if (!child.optional) break;
        }
    }

    private static void addFirstPossibleTokenIds(Set<String> tokenIds, ElementTypeBase elementType) {
        for (LeafElementType leaf : elementType.cache.getFirstPossibleLeafs()) {
            if (leaf.is(OPTIONAL_WRAPPING)) continue;
            tokenIds.add(leaf.tokenType.getId());
        }
    }

    private static void addSeparatorTokenIds(Set<String> tokenIds, IterationElementType iterationElementType) {
        if (iterationElementType.separatorTokens == null) return;

        for (TokenElementType separatorToken : iterationElementType.separatorTokens) {
            tokenIds.add(separatorToken.tokenType.getId());
        }
    }

    private static boolean allOptional(ElementTypeRef[] children, int startIndex) {
        for (int i = startIndex; i < children.length; i++) {
            if (!children[i].optional) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTokenMatch(ElementTypeBase elementType, String tokenId) {
        if (!(elementType instanceof LeafElementType leafElementType)) return false;
        if (leafElementType.is(OPTIONAL_WRAPPING)) return false;

        return leafElementType.tokenType.getId().equals(tokenId);
    }

    private static ElementTypeRef[] getChildren(ElementTypeBase elementType) {
        if (elementType instanceof SequenceElementType sequenceElementType) return sequenceElementType.children;
        if (elementType instanceof OneOfElementType oneOfElementType) return oneOfElementType.children;
        if (elementType instanceof IterationElementType iterationElementType) return new ElementTypeRef[]{new ElementTypeRef(iterationElementType.iteratedElement)};
        if (elementType instanceof WrapperElementType wrapperElementType) return new ElementTypeRef[]{new ElementTypeRef(wrapperElementType.wrappedElement)};
        return new ElementTypeRef[0];
    }

    private static void writeCandidateIdsAttribute(Element element, Iterable<Candidate> candidates) {
        Set<String> candidateIds = new LinkedHashSet<>();
        for (Candidate candidate : candidates) {
            candidateIds.add(candidate.candidateId);
        }
        if (!candidateIds.isEmpty()) {
            element.setAttribute(ATTR_CANDIDATE_IDS, String.join(", ", candidateIds));
        }
    }

    private static List<Element> compactTokenNodes(List<Element> elements) {
        if (elements.size() < 2) return elements;

        Map<String, List<Element>> elementsBySignature = new LinkedHashMap<>();
        for (Element element : elements) {
            elementsBySignature.computeIfAbsent(tokenContentSignature(element), k -> new ArrayList<>()).add(element);
        }

        List<Element> compacted = new ArrayList<>(elementsBySignature.size());
        for (List<Element> group : elementsBySignature.values()) {
            compacted.add(group.size() == 1 ? group.get(0) : mergeTokenNodes(group));
        }
        return compacted;
    }

    private static Element mergeTokenNodes(List<Element> elements) {
        Element result = elements.get(0);
        Set<String> tokenTypeIds = new LinkedHashSet<>();

        for (Element element : elements) {
            tokenTypeIds.addAll(tokenTypeIds(element));
        }

        result.setAttribute(ATTR_TOKEN_TYPE_IDS, String.join(", ", tokenTypeIds));
        return result;
    }

    private static String tokenContentSignature(Element element) {
        StringBuilder signature = new StringBuilder();
        signature.append("candidate-ids=").append(attributeValue(element, ATTR_CANDIDATE_IDS));
        for (Element child : element.getChildren(TAG_NODE)) {
            signature.append("|child=").append(tokenFullSignature(child));
        }
        return signature.toString();
    }

    private static String tokenFullSignature(Element element) {
        StringBuilder signature = new StringBuilder();
        signature.append("token-type-ids=").append(String.join(",", tokenTypeIds(element)));
        signature.append(";candidate-ids=").append(attributeValue(element, ATTR_CANDIDATE_IDS));
        for (Element child : element.getChildren(TAG_NODE)) {
            signature.append("|child=").append(tokenFullSignature(child));
        }
        return signature.toString();
    }

    private static List<String> tokenTypeIds(Element element) {
        return csvAttribute(element, ATTR_TOKEN_TYPE_IDS);
    }

    private static List<String> csvAttribute(Element element, String name) {
        String value = element.getAttributeValue(name);
        return value == null ? List.of() : Lists.fromCsv(value);
    }

    private static String attributeValue(Element element, String name) {
        String value = element.getAttributeValue(name);
        return value == null ? "" : value;
    }

    private static class TokenCandidates implements Iterable<Candidate> {
        private final Map<String, Candidate> candidates = new LinkedHashMap<>();

        private void addCandidate(Candidate candidate) {
            candidates.put(candidate.key(), candidate);
        }

        private int size() {
            return candidates.size();
        }

        @Override
        public java.util.Iterator<Candidate> iterator() {
            return candidates.values().iterator();
        }
    }

    private static class NextTokenMatch {
        private final Set<String> tokenIds = new LinkedHashSet<>();
        private boolean completed;

        private void add(NextTokenMatch match) {
            addTokenIds(match);
            completed = completed || match.completed;
        }

        private void addTokenIds(NextTokenMatch match) {
            tokenIds.addAll(match.tokenIds);
        }

        private static NextTokenMatch empty() {
            return new NextTokenMatch();
        }

        private static NextTokenMatch completed() {
            NextTokenMatch match = new NextTokenMatch();
            match.completed = true;
            return match;
        }
    }

    private static class TrieBuildContext {
        private int maxDepth;

        private void registerTokenNode(int depth) {
            maxDepth = Math.max(maxDepth, depth);
        }
    }

    private static class Candidate {
        private final int branchIndex;
        private final ElementTypeBase elementType;
        private final String candidateId;

        private Candidate(int branchIndex, ElementTypeBase elementType) {
            this.branchIndex = branchIndex;
            this.elementType = elementType;
            candidateId = elementType.getId();
        }

        private String key() {
            return branchIndex + ":" + candidateId;
        }
    }
}
