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

import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.ExecVariableElementType;
import com.dbn.language.common.element.impl.IdentifierElementType;
import com.dbn.language.common.element.impl.IterationElementType;
import com.dbn.language.common.element.impl.NamedElementType;
import com.dbn.language.common.element.impl.OneOfElementType;
import com.dbn.language.common.element.impl.QualifiedIdentifierElementType;
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
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dbn.dev.language.LanguageSpecificationXmlUtil.outputPrettyString;

public class LanguageSpecificationParserExtensionBuilder implements LanguageSpecificationArtifactBuilder {
    private static final int MAX_LOOKAHEAD_DEPTH = 8;
    private static final int MAX_TRIE_NODES_PER_ONE_OF = 500;
    private static final int MAX_REPEATED_CANDIDATE_SET = 2;
    private static final String EXT_DTD_PATH = "../../../common/definition/language-parser-elements-ext.dtd";
    private static final int ELEMENT_LOG_INTERVAL = 25;
    private static final int ONE_OF_LOG_INTERVAL = 50;
    private static final int PREFIX_LOG_INTERVAL = 100000;
    private static final int CACHE_HIT_LOG_INTERVAL = 100000;

    private final LanguageSpecificationBuilderInput input;
    private ElementTypeBundle bundle;
    private ElementTypeBundle.Builder bundleBuilder;
    private int processedElements;
    private int visitedElements;
    private int analyzedOneOfs;
    private int emittedOneOfs;
    private int prunedOneOfs;
    private int resolvedPrefixes;
    private int prefixCacheHits;
    private Map<ElementTypeBase, Map<PrefixCacheKey, PrefixMatch>> prefixCache;

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
            bundleBuilder = builder;
            processedElements = 0;
            visitedElements = 0;
            analyzedOneOfs = 0;
            emittedOneOfs = 0;
            prunedOneOfs = 0;
            resolvedPrefixes = 0;
            prefixCacheHits = 0;
            prefixCache = new IdentityHashMap<>();

            Document sourceDocument = builder.getDocument();
            Element sourceRoot = sourceDocument.getRootElement();
            List<Element> elementDefs = sourceRoot.getChildren("element-def");
            System.out.println("Building parser extension definition for " + elementDefs.size() + " named elements");

            Element extensionRoot = new Element("parser-element-extensions");
            extensionRoot.setAttribute("language", sourceRoot.getAttributeValue("language"));
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
                    ", one-of emitted=" + emittedOneOfs +
                    ", one-of pruned=" + prunedOneOfs +
                    ", prefix resolutions=" + resolvedPrefixes +
                    ", cache hits=" + prefixCacheHits +
                    ", prefix cache=" + prefixCache.size());

            Document extensionDocument = new Document(extensionRoot);
            extensionDocument.addContent(0, new DocType("parser-element-extensions", EXT_DTD_PATH));
            copyCopyright(sourceDocument, extensionDocument);

            File extensionFile = input.getParserElementsExtensionFile();
            System.out.println("Writing " + extensionFile.toPath());
            Files.writeString(extensionFile.toPath(), outputPrettyString(extensionDocument), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not build parser extension definition", e);
        } finally {
            prefixCache = null;
            this.bundle = null;
            bundleBuilder = null;
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

        Element oneOfDefinition = getDefinition(oneOfElementType);
        List<Element> childDefinitions = oneOfDefinition == null ? List.of() : oneOfDefinition.getChildren();
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < children.length; i++) {
            ElementTypeRef child = children[i];
            Element childDefinition = i < childDefinitions.size() ? childDefinitions.get(i) : null;
            Candidate candidate = new Candidate(i, child.elementType, childDefinition);
            candidates.add(candidate);
        }

        if (!hasAmbiguousToken(List.of(), candidates)) return null;

        Element extension = new Element("one-of-extension");
        extension.setAttribute("id", oneOfElementType.getId());
        TrieBuildContext context = new TrieBuildContext();
        boolean added = writeTokenNodes(extension, List.of(), candidates, true, context);
        if (context.pruned) {
            prunedOneOfs++;
            System.out.println("Pruned token trie: " + contextElementId + "/" + oneOfElementType.getId() +
                    " nodes=" + context.tokenNodes);
        }
        return added ? extension : null;
    }

    private boolean writeTokenNodes(
            Element parent,
            List<String> prefix,
            List<Candidate> candidates,
            boolean includeUnambiguous,
            TrieBuildContext context) {
        CandidateSetKey candidateSetKey = CandidateSetKey.from(candidates);
        context.enter(candidateSetKey);
        try {
            return writeTokenNodesInner(parent, prefix, candidates, includeUnambiguous, context);
        } finally {
            context.exit(candidateSetKey);
        }
    }

    private boolean writeTokenNodesInner(
            Element parent,
            List<String> prefix,
            List<Candidate> candidates,
            boolean includeUnambiguous,
            TrieBuildContext context) {
        Map<String, List<Candidate>> candidatesByToken = groupCandidatesByNextToken(prefix, candidates);
        boolean added = false;
        for (Map.Entry<String, List<Candidate>> entry : candidatesByToken.entrySet()) {
            String token = entry.getKey();
            List<Candidate> tokenCandidates = entry.getValue();
            boolean ambiguous = tokenCandidates.size() > 1;
            if (!includeUnambiguous && !ambiguous) continue;

            Element element = new Element("token");
            boolean withinNodeBudget = context.registerTokenNode();
            element.setAttribute("type-id", token);
            if (!ambiguous) {
                writeCandidateIdsAttribute(element, tokenCandidates);
            } else {
                List<String> nextPrefix = append(prefix, token);
                CandidateSetKey tokenCandidateSetKey = CandidateSetKey.from(tokenCandidates);
                boolean shouldExpand =
                        withinNodeBudget &&
                        nextPrefix.size() < MAX_LOOKAHEAD_DEPTH &&
                        !context.isRepeated(tokenCandidateSetKey);

                if (!shouldExpand) {
                    context.pruned = true;
                }

                boolean resolved = shouldExpand && writeTokenNodes(element, nextPrefix, tokenCandidates, true, context);
                if (!resolved) {
                    writeCandidateIdsAttribute(element, tokenCandidates);
                }
            }
            parent.addContent(element);
            added = true;
        }
        return added;
    }

    private boolean hasAmbiguousToken(List<String> prefix, List<Candidate> candidates) {
        Map<String, List<Candidate>> candidatesByToken = groupCandidatesByNextToken(prefix, candidates);
        for (List<Candidate> tokenCandidates : candidatesByToken.values()) {
            if (tokenCandidates.size() > 1) return true;
        }
        return false;
    }

    private Map<String, List<Candidate>> groupCandidatesByNextToken(List<String> prefix, List<Candidate> candidates) {
        Map<String, Map<String, Candidate>> candidatesByToken = new LinkedHashMap<>();
        for (Candidate candidate : candidates) {
            Set<String> tokens = resolveNextTokens(candidate.elementType, prefix);
            for (String token : tokens) {
                candidatesByToken.computeIfAbsent(token, k -> new LinkedHashMap<>()).put(candidate.key(), candidate);
            }
        }
        Map<String, List<Candidate>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Candidate>> entry : candidatesByToken.entrySet()) {
            result.put(entry.getKey(), List.copyOf(entry.getValue().values()));
        }
        return result;
    }

    private static List<String> append(List<String> prefix, String token) {
        List<String> result = new ArrayList<>(prefix);
        result.add(token);
        return result;
    }

    private Set<String> resolveNextTokens(ElementTypeBase elementType, List<String> prefix) {
        PrefixMatch match = resolvePrefix(elementType, prefix, 0, new HashSet<>());
        return match.nextTokens;
    }

    private PrefixMatch resolvePrefix(
            ElementTypeBase elementType,
            List<String> prefix,
            int offset,
            Set<PrefixVisit> visitingElements) {
        if (elementType == null) return PrefixMatch.empty();
        if (offset > prefix.size()) return PrefixMatch.empty();

        PrefixCacheKey cacheKey = new PrefixCacheKey(prefix, offset);
        PrefixMatch cachedMatch = getCachedPrefixMatch(elementType, cacheKey);
        if (cachedMatch != null) {
            prefixCacheHits++;
            if (prefixCacheHits % CACHE_HIT_LOG_INTERVAL == 0) {
                System.out.println("Prefix cache hits: " + prefixCacheHits +
                        " current=" + elementType.getId() +
                        " key=" + cacheKey);
            }
            return cachedMatch;
        }

        resolvedPrefixes++;
        if (resolvedPrefixes % PREFIX_LOG_INTERVAL == 0) {
            System.out.println("Resolved prefix calls: " + resolvedPrefixes +
                    " current=" + elementType.getId() +
                    " key=" + cacheKey +
                    " stack=" + visitingElements.size());
        }

        PrefixVisit visit = new PrefixVisit(elementType, cacheKey);
        if (!visitingElements.add(visit)) {
            return PrefixMatch.incomplete();
        }

        try {
            PrefixMatch result;
            if (elementType instanceof TokenElementType tokenElementType) {
                result = matchToken(tokenElementType.tokenType.getId(), prefix, offset);
            } else if (elementType instanceof IdentifierElementType || elementType instanceof QualifiedIdentifierElementType) {
                result = matchToken(elementType.bundle.getTokenTypeBundle().getIdentifier().getId(), prefix, offset);
            } else if (elementType instanceof ExecVariableElementType) {
                result = matchToken(elementType.bundle.getTokenTypeBundle().getVariable().getId(), prefix, offset);
            } else if (elementType instanceof SequenceElementType sequenceElementType) {
                result = resolveSequencePrefix(sequenceElementType.children, prefix, offset, visitingElements);
            } else if (elementType instanceof OneOfElementType oneOfElementType) {
                result = resolveOneOfPrefix(oneOfElementType, prefix, offset, visitingElements);
            } else if (elementType instanceof IterationElementType iterationElementType) {
                result = resolveIterationPrefix(iterationElementType, prefix, offset, visitingElements);
            } else if (elementType instanceof WrapperElementType wrapperElementType) {
                result = resolveWrapperPrefix(wrapperElementType, prefix, offset, visitingElements);
            } else {
                result = PrefixMatch.completed(offset);
            }
            return cachePrefixMatch(elementType, cacheKey, result);
        } finally {
            visitingElements.remove(visit);
        }
    }

    private PrefixMatch resolveOneOfPrefix(
            OneOfElementType oneOfElementType,
            List<String> prefix,
            int offset,
            Set<PrefixVisit> visitingElements) {
        PrefixMatch result = new PrefixMatch();
        for (ElementTypeRef child : oneOfElementType.children) {
            result.add(resolvePrefix(child.elementType, prefix, offset, visitingElements));
        }
        return result;
    }

    private PrefixMatch resolveSequencePrefix(
            ElementTypeRef[] children,
            List<String> prefix,
            int offset,
            Set<PrefixVisit> visitingElements) {
        PrefixMatch result = new PrefixMatch();
        Set<Integer> activeOffsets = new LinkedHashSet<>();
        activeOffsets.add(offset);

        for (ElementTypeRef child : children) {
            Set<Integer> nextOffsets = new LinkedHashSet<>();
            for (int activeOffset : activeOffsets) {
                PrefixMatch childMatch = resolvePrefix(child.elementType, prefix, activeOffset, visitingElements);
                result.addNextTokens(childMatch);
                nextOffsets.addAll(childMatch.completedOffsets);
                if (child.optional) {
                    nextOffsets.add(activeOffset);
                }
            }

            activeOffsets = nextOffsets;
            if (activeOffsets.isEmpty()) break;
        }

        result.completedOffsets.addAll(activeOffsets);
        return result;
    }

    private PrefixMatch resolveIterationPrefix(
            IterationElementType iterationElementType,
            List<String> prefix,
            int offset,
            Set<PrefixVisit> visitingElements) {
        PrefixMatch result = new PrefixMatch();
        PrefixMatch firstMatch = resolvePrefix(iterationElementType.iteratedElement, prefix, offset, visitingElements);
        result.addNextTokens(firstMatch);

        Set<IterationState> activeStates = new LinkedHashSet<>();
        for (int completedOffset : firstMatch.completedOffsets) {
            activeStates.add(new IterationState(completedOffset, 1));
        }

        Set<IterationState> visitedStates = new HashSet<>();
        while (!activeStates.isEmpty()) {
            Set<IterationState> nextStates = new LinkedHashSet<>();
            for (IterationState activeState : activeStates) {
                if (!visitedStates.add(activeState)) continue;

                if (activeState.count >= Math.max(1, iterationElementType.minIterations)) {
                    result.completedOffsets.add(activeState.offset);
                }

                if (iterationElementType.separatorTokens == null || iterationElementType.separatorTokens.length == 0) {
                    continue;
                }

                for (TokenElementType separatorToken : iterationElementType.separatorTokens) {
                    PrefixMatch separatorMatch = matchToken(separatorToken.tokenType.getId(), prefix, activeState.offset);
                    result.addNextTokens(separatorMatch);
                    for (int separatorOffset : separatorMatch.completedOffsets) {
                        PrefixMatch nextMatch = resolvePrefix(iterationElementType.iteratedElement, prefix, separatorOffset, visitingElements);
                        result.addNextTokens(nextMatch);
                        for (int completedOffset : nextMatch.completedOffsets) {
                            nextStates.add(new IterationState(completedOffset, activeState.count + 1));
                        }
                    }
                }
            }
            activeStates = nextStates;
        }

        return result;
    }

    private PrefixMatch resolveWrapperPrefix(
            WrapperElementType wrapperElementType,
            List<String> prefix,
            int offset,
            Set<PrefixVisit> visitingElements) {
        PrefixMatch result = new PrefixMatch();
        PrefixMatch beginMatch = matchToken(wrapperElementType.getBeginTokenElement().tokenType.getId(), prefix, offset);
        result.addNextTokens(beginMatch);

        Set<Integer> wrappedOffsets = new LinkedHashSet<>();
        for (int beginOffset : beginMatch.completedOffsets) {
            PrefixMatch wrappedMatch = resolvePrefix(wrapperElementType.wrappedElement, prefix, beginOffset, visitingElements);
            result.addNextTokens(wrappedMatch);
            wrappedOffsets.addAll(wrappedMatch.completedOffsets);
            if (wrapperElementType.wrappedElementOptional) {
                wrappedOffsets.add(beginOffset);
            }
        }

        for (int wrappedOffset : wrappedOffsets) {
            PrefixMatch endMatch = matchToken(wrapperElementType.getEndTokenElement().tokenType.getId(), prefix, wrappedOffset);
            result.add(endMatch);
        }

        return result;
    }

    private static PrefixMatch matchToken(String token, List<String> prefix, int offset) {
        if (offset > prefix.size()) return PrefixMatch.empty();
        if (offset == prefix.size()) {
            return PrefixMatch.next(token);
        }

        return token.equals(prefix.get(offset)) ?
                PrefixMatch.completed(offset + 1) :
                PrefixMatch.empty();
    }

    private PrefixMatch getCachedPrefixMatch(ElementTypeBase elementType, PrefixCacheKey cacheKey) {
        Map<PrefixCacheKey, PrefixMatch> matches = prefixCache.get(elementType);
        return matches == null ? null : matches.get(cacheKey);
    }

    private PrefixMatch cachePrefixMatch(ElementTypeBase elementType, PrefixCacheKey cacheKey, PrefixMatch match) {
        if (match.incomplete) return match;

        prefixCache.computeIfAbsent(elementType, k -> new LinkedHashMap<>()).put(cacheKey, match);
        return match;
    }

    private Element getDefinition(ElementTypeBase elementType) {
        return bundleBuilder.getDefinition(elementType);
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
            element.setAttribute("candidate-ids", String.join(", ", candidateIds));
        }
    }

    private static class PrefixMatch {
        private final Set<String> nextTokens = new LinkedHashSet<>();
        private final Set<Integer> completedOffsets = new LinkedHashSet<>();
        private boolean incomplete;

        private void add(PrefixMatch match) {
            addNextTokens(match);
            completedOffsets.addAll(match.completedOffsets);
        }

        private void addNextTokens(PrefixMatch match) {
            nextTokens.addAll(match.nextTokens);
            incomplete = incomplete || match.incomplete;
        }

        private static PrefixMatch empty() {
            return new PrefixMatch();
        }

        private static PrefixMatch incomplete() {
            PrefixMatch match = new PrefixMatch();
            match.incomplete = true;
            return match;
        }

        private static PrefixMatch next(String token) {
            PrefixMatch match = new PrefixMatch();
            match.nextTokens.add(token);
            return match;
        }

        private static PrefixMatch completed(int offset) {
            PrefixMatch match = new PrefixMatch();
            match.completedOffsets.add(offset);
            return match;
        }
    }

    private record PrefixCacheKey(List<String> prefix, int offset) {
        private PrefixCacheKey {
            prefix = List.copyOf(prefix);
        }
    }

    private record PrefixVisit(ElementTypeBase elementType, PrefixCacheKey cacheKey) {}

    private record IterationState(int offset, int count) {}

    private static class TrieBuildContext {
        private final Map<CandidateSetKey, Integer> candidateSetVisits = new LinkedHashMap<>();
        private int tokenNodes;
        private boolean pruned;

        private void enter(CandidateSetKey candidateSetKey) {
            candidateSetVisits.merge(candidateSetKey, 1, Integer::sum);
        }

        private void exit(CandidateSetKey candidateSetKey) {
            Integer count = candidateSetVisits.get(candidateSetKey);
            if (count == null || count == 1) {
                candidateSetVisits.remove(candidateSetKey);
            } else {
                candidateSetVisits.put(candidateSetKey, count - 1);
            }
        }

        private boolean registerTokenNode() {
            tokenNodes++;
            return tokenNodes <= MAX_TRIE_NODES_PER_ONE_OF;
        }

        private boolean isRepeated(CandidateSetKey candidateSetKey) {
            return candidateSetVisits.getOrDefault(candidateSetKey, 0) >= MAX_REPEATED_CANDIDATE_SET;
        }
    }

    private record CandidateSetKey(List<String> keys) {
        private static CandidateSetKey from(List<Candidate> candidates) {
            List<String> keys = new ArrayList<>(candidates.size());
            for (Candidate candidate : candidates) {
                keys.add(candidate.key());
            }
            return new CandidateSetKey(keys);
        }

        private CandidateSetKey {
            keys = List.copyOf(keys);
        }
    }

    private static class Candidate {
        private final int branchIndex;
        private final ElementTypeBase elementType;
        private final String candidateId;

        private Candidate(int branchIndex, ElementTypeBase elementType, Element definition) {
            this.branchIndex = branchIndex;
            this.elementType = elementType;
            String refId = definition == null ? null : definition.getAttributeValue("ref-id");
            String id = definition == null ? null : definition.getAttributeValue("id");
            candidateId = refId != null ? refId : id != null ? id : elementType.getId();
        }

        private String key() {
            return branchIndex + ":" + candidateId;
        }
    }
}
