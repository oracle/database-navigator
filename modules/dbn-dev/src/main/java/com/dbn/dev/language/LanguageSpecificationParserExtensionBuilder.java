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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
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
    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final int ELEMENT_LOG_INTERVAL = 25;
    private static final int ONE_OF_LOG_INTERVAL = 50;
    private static final long PROGRESS_LOG_INTERVAL_NANOS = 5_000_000_000L;
    private static final int MAX_NEXT_TOKEN_LOOK_THROUGH_DEPTH = 12;
    // Separate from emitted trie depth; keep grammar traversal bounded to avoid recursive expression fan-out.
    private static final int MAX_ELEMENT_MATCH_DEPTH = 24;
    private static final int MAX_SAME_CANDIDATE_LOOK_THROUGH_DEPTH = 3;
    private static final int MAX_SAME_CANDIDATE_LOOK_THROUGH_TOKENS = 8;
    private static final Set<String> STRUCTURAL_LOOK_THROUGH_TOKENS = Set.of(
            "CHR_LEFT_PARENTHESIS",
            "CHR_RIGHT_PARENTHESIS",
            "CHR_LEFT_BRACKET",
            "CHR_RIGHT_BRACKET",
            "CHR_DOT",
            "CHR_COMMA");

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
        log("Reading " + input.getParserElementsFile().toPath());
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
            log("Building parser extension definition for " + elementDefs.size() + " named elements");

            Element extensionRoot = new Element("parser-element-extensions");
            extensionRoot.setAttribute("language", definitionRoot.getAttributeValue("language"));
            extensionRoot.setAttribute("source", input.getParserElementsFile().getName());

            for (Element elementDef : elementDefs) {
                String elementId = elementDef.getAttributeValue("id");
                processedElements++;
                if (processedElements == 1 || processedElements % ELEMENT_LOG_INTERVAL == 0) {
                    log("Processing named element " + processedElements + "/" + elementDefs.size() + ": " + elementId);
                }

                NamedElementType elementType = elementId == null ? null : getNamedElementType(elementId);
                if (elementType != null) {
                    collectOneOfExtensions(elementType, elementId, extensionRoot, new HashSet<>());
                } else {
                    log("Skipping unresolved named element: " + elementId);
                }
            }

            log("Parser extension analysis finished. Visited=" + visitedElements +
                    ", one-of analyzed=" + analyzedOneOfs +
                    ", one-of emitted=" + emittedOneOfs);

            Document extensionDocument = new Document(extensionRoot);
            extensionDocument.addContent(0, new DocType("parser-element-extensions", EXT_DTD_PATH));
            copyCopyright(definitionDocument, extensionDocument);

            File extensionFile = input.getParserElementsExtensionFile();
            log("Writing " + extensionFile.toPath());
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

    private static void log(String message) {
        System.out.println(LOG_TIME_FORMAT.format(LocalTime.now()) + " " + message);
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
            log("Analyzing one-of " + analyzedOneOfs + ": " + contextElementId + "/" + oneOfElementType.getId() +
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
        TrieBuildContext context = new TrieBuildContext(contextElementId, oneOfElementType.getId());
        boolean added = writeTokenNodes(extension, candidatesByToken, true, oneOfElementType, context);
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
            OneOfElementType oneOfElementType,
            TrieBuildContext context) {
        List<Element> elements = new ArrayList<>();
        for (Map.Entry<String, TokenCandidates> entry : candidatesByToken.entrySet()) {
            TokenCandidates tokenCandidates = entry.getValue();
            boolean ambiguous = tokenCandidates.size() > 1;
            if (!includeUnambiguous && !ambiguous) continue;

            Element element = new Element(TAG_NODE);
            context.registerTokenNode(1);
            element.setAttribute(ATTR_TOKEN_TYPE_IDS, entry.getKey());
            writeNextTokenNodes(element, List.of(entry.getKey()), tokenCandidates, oneOfElementType, 2, 0, context);
            writeCandidateIdsAttribute(element, tokenCandidates, element.getChildren(TAG_NODE).isEmpty(), List.of(entry.getKey()));
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
            List<String> tokenPath,
            TokenCandidates tokenCandidates,
            OneOfElementType oneOfElementType,
            int depth,
            int sameCandidateDepth,
            TrieBuildContext context) {
        if (tokenCandidates.size() < 2) return;
        if (depth > MAX_NEXT_TOKEN_LOOK_THROUGH_DEPTH) return;

        Map<String, TokenCandidates> candidatesByToken = collectNextTokenCandidates(tokenPath, tokenCandidates, oneOfElementType, context);
        if (candidatesByToken.isEmpty()) return;
        context.registerTrieExpansion(tokenPath, tokenCandidates, candidatesByToken);

        List<Element> elements = new ArrayList<>();
        boolean hasReducingToken = hasReducingToken(candidatesByToken, tokenCandidates);
        for (Map.Entry<String, TokenCandidates> entry : candidatesByToken.entrySet()) {
            boolean sameCandidateSet = sameCandidates(tokenCandidates, entry.getValue());
            // Same-candidate paths do not narrow the one-of by themselves. Keep only bounded
            // structural look-through, otherwise generic expressions fan out into huge tries.
            if (sameCandidateSet && !canExpandSameCandidatePath(entry.getKey(), tokenPath, candidatesByToken, sameCandidateDepth, hasReducingToken)) {
                context.registerPrunedSameCandidatePath(tokenPath, entry.getKey());
                continue;
            }

            List<String> nextTokenPath = appendToken(tokenPath, entry.getKey());
            Element element = new Element(TAG_NODE);
            element.setAttribute(ATTR_TOKEN_TYPE_IDS, entry.getKey());
            writeNextTokenNodes(
                    element,
                    nextTokenPath,
                    entry.getValue(),
                    oneOfElementType,
                    depth + 1,
                    sameCandidateSet ? sameCandidateDepth + 1 : 0,
                    context);

            if (sameCandidateSet && element.getChildren(TAG_NODE).isEmpty()) {
                continue;
            }
            writeCandidateIdsAttribute(element, entry.getValue(), true, nextTokenPath);
            context.registerTokenNode(depth);
            elements.add(element);
        }

        for (Element element : compactTokenNodes(elements)) {
            parent.addContent(element);
        }
    }

    private static boolean canExpandSameCandidatePath(
            String tokenId,
            List<String> tokenPath,
            Map<String, TokenCandidates> candidatesByToken,
            int sameCandidateDepth,
            boolean hasReducingToken) {
        if (sameCandidateDepth >= MAX_SAME_CANDIDATE_LOOK_THROUGH_DEPTH) return false;
        // Once another token already narrows the candidates, avoid chasing expression-internal
        // continuations. The exceptions are compact structural forms where one more token is
        // needed before the ambiguity can reduce: aggregate (*) and qualified names after ".".
        if (hasReducingToken &&
                !isCallableParenthesizedStar(tokenId, tokenPath) &&
                !isCompletingCallableParenthesizedStar(tokenId, tokenPath) &&
                !isQualifiedNameSeparator(tokenId, tokenPath) &&
                !isQualifiedNameContinuation(tokenId, tokenPath)) return false;

        return STRUCTURAL_LOOK_THROUGH_TOKENS.contains(tokenId) ||
                isQualifiedNameSeparator(tokenId, tokenPath) ||
                isQualifiedNameContinuation(tokenId, tokenPath) ||
                isCallableParenthesizedStar(tokenId, tokenPath) ||
                candidatesByToken.size() <= MAX_SAME_CANDIDATE_LOOK_THROUGH_TOKENS;
    }

    private static boolean hasReducingToken(
            Map<String, TokenCandidates> candidatesByToken,
            TokenCandidates tokenCandidates) {
        for (TokenCandidates nextTokenCandidates : candidatesByToken.values()) {
            if (!sameCandidates(tokenCandidates, nextTokenCandidates)) return true;
        }
        return false;
    }

    private static boolean isCallableParenthesizedStar(String tokenId, List<String> tokenPath) {
        return "CHR_STAR".equals(tokenId) &&
                tokenPath.size() >= 2 &&
                "CHR_LEFT_PARENTHESIS".equals(tokenPath.get(tokenPath.size() - 1)) &&
                isCallableToken(tokenPath.get(tokenPath.size() - 2));
    }

    private static boolean isCompletingCallableParenthesizedStar(String tokenId, List<String> tokenPath) {
        return "CHR_RIGHT_PARENTHESIS".equals(tokenId) &&
                tokenPath.size() >= 3 &&
                "CHR_STAR".equals(tokenPath.get(tokenPath.size() - 1)) &&
                "CHR_LEFT_PARENTHESIS".equals(tokenPath.get(tokenPath.size() - 2)) &&
                isCallableToken(tokenPath.get(tokenPath.size() - 3));
    }

    private static boolean isCallableToken(String tokenId) {
        return "IDENTIFIER".equals(tokenId) || tokenId.startsWith("FN_");
    }

    private static boolean isQualifiedNameContinuation(String tokenId, List<String> tokenPath) {
        return isIdentifierToken(tokenId) &&
                !tokenPath.isEmpty() &&
                "CHR_DOT".equals(tokenPath.get(tokenPath.size() - 1));
    }

    private static boolean isQualifiedNameSeparator(String tokenId, List<String> tokenPath) {
        return "CHR_DOT".equals(tokenId) &&
                !tokenPath.isEmpty() &&
                isIdentifierToken(tokenPath.get(tokenPath.size() - 1));
    }

    private static boolean isQualifiedCallPath(List<String> tokenPath) {
        if (tokenPath.size() < 4) return false;
        if (!"CHR_LEFT_PARENTHESIS".equals(tokenPath.get(tokenPath.size() - 1))) return false;

        return hasQualifiedNamePrefix(tokenPath, tokenPath.size() - 1);
    }

    private static boolean isPlainIdentifierCallArgumentPath(List<String> tokenPath) {
        return tokenPath.size() > 2 &&
                "IDENTIFIER".equals(tokenPath.get(0)) &&
                "CHR_LEFT_PARENTHESIS".equals(tokenPath.get(1));
    }

    private static boolean isQualifiedNamePath(List<String> tokenPath) {
        if (tokenPath.size() < 3) return false;
        if (!isIdentifierToken(tokenPath.get(tokenPath.size() - 1))) return false;

        return hasQualifiedNamePrefix(tokenPath, tokenPath.size());
    }

    private static boolean hasQualifiedNamePrefix(List<String> tokenPath, int endIndex) {
        for (int i = 1; i < tokenPath.size() - 1; i++) {
            if (i >= endIndex) break;
            if (!"CHR_DOT".equals(tokenPath.get(i))) continue;
            if (isIdentifierToken(tokenPath.get(i - 1)) && isIdentifierToken(tokenPath.get(i + 1))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIdentifierToken(String tokenId) {
        return "IDENTIFIER".equals(tokenId);
    }

    private static Map<String, TokenCandidates> collectNextTokenCandidates(
            List<String> tokenPath,
            TokenCandidates tokenCandidates,
            OneOfElementType oneOfElementType,
            TrieBuildContext context) {
        Map<String, TokenCandidates> candidatesByToken = new LinkedHashMap<>();
        Map<Candidate, NextTokenMatch> matches = new LinkedHashMap<>();
        Set<String> intrinsicTokenIds = new LinkedHashSet<>();
        MatchContext matchContext = context.newMatchContext(tokenPath);

        for (Candidate candidate : tokenCandidates) {
            NextTokenMatch match = getNextTokenMatch(candidate.elementType, tokenPath, matchContext);
            matches.put(candidate, match);
            intrinsicTokenIds.addAll(match.tokenIds);
        }

        for (Map.Entry<Candidate, NextTokenMatch> entry : matches.entrySet()) {
            Candidate candidate = entry.getKey();
            NextTokenMatch match = entry.getValue();
            Set<String> tokenIds = new LinkedHashSet<>(match.tokenIds);
            if (match.completed) {
                Set<String> followTokenIds = getNextPossibleTokenIds(oneOfElementType);
                followTokenIds.removeAll(intrinsicTokenIds);
                tokenIds.addAll(followTokenIds);
            }
            for (String tokenId : tokenIds) {
                TokenCandidates tokenCandidatesById = candidatesByToken.computeIfAbsent(tokenId, k -> new TokenCandidates());
                tokenCandidatesById.addCandidate(candidate);
            }
        }
        return candidatesByToken;
    }

    private static List<String> appendToken(List<String> tokenPath, String tokenId) {
        List<String> result = new ArrayList<>(tokenPath.size() + 1);
        result.addAll(tokenPath);
        result.add(tokenId);
        return result;
    }

    private static NextTokenMatch getNextTokenMatch(ElementTypeBase elementType, List<String> tokenPath) {
        return getNextTokenMatch(elementType, tokenPath, new MatchContext());
    }

    private static NextTokenMatch getNextTokenMatch(
            ElementTypeBase elementType,
            List<String> tokenPath,
            MatchContext context) {
        return getNextTokenMatch(elementType, tokenPath, context, 0);
    }

    private static NextTokenMatch getNextTokenMatch(
            ElementTypeBase elementType,
            List<String> tokenPath,
            MatchContext context,
            int depth) {
        if (elementType == null || depth > MAX_ELEMENT_MATCH_DEPTH) return NextTokenMatch.empty();
        if (!canStartWith(elementType, tokenPath)) return NextTokenMatch.empty();
        context.registerMatchCall(elementType, tokenPath, depth);
        if (context.isVisiting(elementType)) {
            context.registerCycleHit();
            return NextTokenMatch.empty();
        }

        NextTokenMatch cached = context.get(elementType, tokenPath, depth);
        if (cached != null) return cached;

        // Recursive grammar cycles are pruned during matching. Do not cache results that saw a
        // cycle; they are path-state dependent and can poison later candidates in the same trie.
        NextTokenMatch result;
        int cycleHits = context.cycleHits;
        context.enter(elementType);
        try {
            if (elementType instanceof LeafElementType leafElementType) {
                result = getLeafNextTokenMatch(leafElementType, tokenPath);
            } else if (elementType instanceof SequenceElementType sequenceElementType) {
                result = getSequenceNextTokenMatch(sequenceElementType.children, tokenPath, context, depth);
            } else if (elementType instanceof OneOfElementType oneOfElementType) {
                result = new NextTokenMatch();
                for (ElementTypeRef child : oneOfElementType.children) {
                    result.add(getNextTokenMatch(child.elementType, tokenPath, context, depth + 1));
                }
            } else if (elementType instanceof WrapperElementType wrapperElementType) {
                result = new NextTokenMatch();
                result.add(getWrapperNextTokenMatch(wrapperElementType, tokenPath, context, depth));
                result.add(getNextTokenMatch(wrapperElementType.wrappedElement, tokenPath, context, depth + 1));
            } else if (elementType instanceof QualifiedIdentifierElementType qualifiedIdentifierElementType) {
                result = getQualifiedIdentifierNextTokenMatch(qualifiedIdentifierElementType, tokenPath);
            } else if (elementType instanceof IterationElementType iterationElementType) {
                result = getIterationNextTokenMatch(iterationElementType, tokenPath, context, depth);
            } else {
                result = NextTokenMatch.empty();
            }
        } finally {
            context.exit(elementType);
        }

        if (context.cycleHits == cycleHits) {
            context.put(elementType, tokenPath, depth, result);
        }
        return result.copy();
    }

    private static NextTokenMatch getLeafNextTokenMatch(LeafElementType leafElementType, List<String> tokenPath) {
        if (tokenPath.isEmpty()) {
            NextTokenMatch result = new NextTokenMatch();
            if (!leafElementType.is(OPTIONAL_WRAPPING)) {
                result.tokenIds.add(leafElementType.tokenType.getId());
            }
            return result;
        }

        return tokenPath.size() == 1 && isTokenMatch(leafElementType, tokenPath.get(0)) ?
                NextTokenMatch.completed() :
                NextTokenMatch.empty();
    }

    private static NextTokenMatch getQualifiedIdentifierNextTokenMatch(
            QualifiedIdentifierElementType elementType,
            List<String> tokenPath) {
        NextTokenMatch result = new NextTokenMatch();
        for (LeafElementType[] variant : elementType.variants) {
            result.add(getQualifiedIdentifierVariantNextTokenMatch(elementType, variant, tokenPath));
        }
        return result;
    }

    private static NextTokenMatch getQualifiedIdentifierVariantNextTokenMatch(
            QualifiedIdentifierElementType elementType,
            LeafElementType[] variant,
            List<String> tokenPath) {
        NextTokenMatch result = new NextTokenMatch();
        if (variant.length == 0) return result;

        if (tokenPath.isEmpty()) {
            if (!variant[0].is(OPTIONAL_WRAPPING)) {
                result.tokenIds.add(variant[0].tokenType.getId());
            }
            return result;
        }

        int tokenIndex = 0;
        for (int variantIndex = 0; variantIndex < variant.length; variantIndex++) {
            LeafElementType leaf = variant[variantIndex];
            if (tokenIndex >= tokenPath.size() || !isTokenMatch(leaf, tokenPath.get(tokenIndex))) {
                return NextTokenMatch.empty();
            }
            tokenIndex++;

            boolean lastVariantToken = variantIndex == variant.length - 1;
            if (lastVariantToken) {
                if (tokenIndex == tokenPath.size()) {
                    result.completed = true;
                }
                return result;
            }

            String separatorTokenId = elementType.separatorToken.tokenType.getId();
            if (tokenIndex == tokenPath.size()) {
                result.tokenIds.add(separatorTokenId);
                return result;
            }

            if (!separatorTokenId.equals(tokenPath.get(tokenIndex))) {
                return NextTokenMatch.empty();
            }
            tokenIndex++;

            if (tokenIndex == tokenPath.size()) {
                LeafElementType nextLeaf = variant[variantIndex + 1];
                if (!nextLeaf.is(OPTIONAL_WRAPPING)) {
                    result.tokenIds.add(nextLeaf.tokenType.getId());
                }
                return result;
            }
        }
        return result;
    }

    private static NextTokenMatch getWrapperNextTokenMatch(
            WrapperElementType wrapperElementType,
            List<String> tokenPath,
            MatchContext context,
            int depth) {
        List<ElementTypeRef> children = new ArrayList<>(3);
        children.add(new ElementTypeRef(wrapperElementType.getBeginTokenElement()));
        children.add(new ElementTypeRef(wrapperElementType.wrappedElement, wrapperElementType.wrappedElementOptional, 0, null));
        children.add(new ElementTypeRef(wrapperElementType.getEndTokenElement()));
        return getSequenceNextTokenMatch(children.toArray(ElementTypeRef[]::new), tokenPath, context, depth + 1);
    }

    private static NextTokenMatch getIterationNextTokenMatch(
            IterationElementType iterationElementType,
            List<String> tokenPath,
            MatchContext context,
            int depth) {
        NextTokenMatch result = getNextTokenMatch(iterationElementType.iteratedElement, tokenPath, context, depth + 1);
        if (result.completed) {
            addSeparatorTokenIds(result.tokenIds, iterationElementType);
            result.completed = iterationElementType.minIterations <= 1;
        }

        if (iterationElementType.separatorTokens == null || tokenPath.size() < 2) {
            return result;
        }

        for (int splitIndex = 1; splitIndex < tokenPath.size(); splitIndex++) {
            NextTokenMatch prefixMatch = getNextTokenMatch(
                    iterationElementType.iteratedElement,
                    tokenPath.subList(0, splitIndex),
                    context,
                    depth + 1);
            if (!prefixMatch.completed) continue;
            if (!isSeparator(iterationElementType, tokenPath.get(splitIndex))) continue;

            result.add(getIterationNextTokenMatch(
                    iterationElementType,
                    tokenPath.subList(splitIndex + 1, tokenPath.size()),
                    context,
                    depth + 1));
        }
        return result;
    }

    private static NextTokenMatch getSequenceNextTokenMatch(
            ElementTypeRef[] children,
            List<String> tokenPath,
            MatchContext context,
            int depth) {
        return getSequenceNextTokenMatch(children, 0, tokenPath, context, depth + 1);
    }

    private static NextTokenMatch getSequenceNextTokenMatch(
            ElementTypeRef[] children,
            int startIndex,
            List<String> tokenPath,
            MatchContext context,
            int depth) {
        NextTokenMatch result = new NextTokenMatch();
        if (startIndex >= children.length) {
            if (tokenPath.isEmpty()) {
                result.completed = true;
            }
            return result;
        }

        if (tokenPath.isEmpty()) {
            addFirstPossibleTokenIds(result.tokenIds, children, startIndex);
            if (allOptional(children, startIndex)) {
                result.completed = true;
            }
            return result;
        }

        for (int i = startIndex; i < children.length; i++) {
            ElementTypeRef child = children[i];
            if (child.optional) {
                result.add(getSequenceNextTokenMatch(children, i + 1, tokenPath, context, depth + 1));
            }

            for (int pathLength = 1; pathLength <= tokenPath.size(); pathLength++) {
                NextTokenMatch childMatch = getNextTokenMatch(
                        child.elementType,
                        tokenPath.subList(0, pathLength),
                        context,
                        depth + 1);
                if (childMatch.isEmpty()) continue;

                if (pathLength == tokenPath.size()) {
                    result.addTokenIds(childMatch);
                    if (childMatch.completed) {
                        addFirstPossibleTokenIds(result.tokenIds, children, i + 1);
                        if (allOptional(children, i + 1)) {
                            result.completed = true;
                        }
                    }
                } else if (childMatch.completed) {
                    result.add(getSequenceNextTokenMatch(
                            children,
                            i + 1,
                            tokenPath.subList(pathLength, tokenPath.size()),
                            context,
                            depth + 1));
                }
            }

            break;
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

    private static Set<String> getNextPossibleTokenIds(ElementTypeBase elementType) {
        Set<String> tokenIds = new LinkedHashSet<>();
        for (TokenType tokenType : elementType.cache.getNextPossibleTokens()) {
            tokenIds.add(tokenType.getId());
        }
        return tokenIds;
    }

    private static void addSeparatorTokenIds(Set<String> tokenIds, IterationElementType iterationElementType) {
        if (iterationElementType.separatorTokens == null) return;

        for (TokenElementType separatorToken : iterationElementType.separatorTokens) {
            tokenIds.add(separatorToken.tokenType.getId());
        }
    }

    private static boolean isSeparator(IterationElementType iterationElementType, String tokenId) {
        if (iterationElementType.separatorTokens == null) return false;

        for (TokenElementType separatorToken : iterationElementType.separatorTokens) {
            if (separatorToken.tokenType.getId().equals(tokenId)) return true;
        }
        return false;
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

    private static boolean canStartWith(ElementTypeBase elementType, List<String> tokenPath) {
        if (tokenPath.isEmpty()) return true;

        // Wrapper matching also looks through to the wrapped element below. Keep this fast gate
        // aligned with that behavior, otherwise optional wrapping loses intrinsic continuations
        // such as list separators after the opening token has already been consumed.
        if (elementType instanceof WrapperElementType wrapperElementType &&
                canStartWith(wrapperElementType.wrappedElement, tokenPath)) {
            return true;
        }

        String firstTokenId = tokenPath.get(0);
        for (LeafElementType leaf : elementType.cache.getFirstPossibleLeafs()) {
            if (leaf.is(OPTIONAL_WRAPPING)) continue;
            if (leaf.tokenType.getId().equals(firstTokenId)) return true;
        }
        return false;
    }

    private static ElementTypeRef[] getChildren(ElementTypeBase elementType) {
        if (elementType instanceof SequenceElementType sequenceElementType) return sequenceElementType.children;
        if (elementType instanceof OneOfElementType oneOfElementType) return oneOfElementType.children;
        if (elementType instanceof IterationElementType iterationElementType) return new ElementTypeRef[]{new ElementTypeRef(iterationElementType.iteratedElement)};
        if (elementType instanceof WrapperElementType wrapperElementType) return new ElementTypeRef[]{new ElementTypeRef(wrapperElementType.wrappedElement)};
        return new ElementTypeRef[0];
    }

    private static void writeCandidateIdsAttribute(
            Element element,
            Iterable<Candidate> candidates,
            boolean preferSpecificCandidates,
            List<String> tokenPath) {
        Set<String> candidateIds = new LinkedHashSet<>();
        Iterable<Candidate> orderedCandidates = preferSpecificCandidates ? orderCandidatesBySpecificity(candidates, tokenPath) : candidates;
        for (Candidate candidate : orderedCandidates) {
            candidateIds.add(candidate.candidateId);
        }
        if (!candidateIds.isEmpty()) {
            element.setAttribute(ATTR_CANDIDATE_IDS, String.join(", ", candidateIds));
        }
    }

    private static List<Candidate> orderCandidatesBySpecificity(Iterable<Candidate> candidates, List<String> tokenPath) {
        List<Candidate> orderedCandidates = new ArrayList<>();
        for (Candidate candidate : candidates) {
            orderedCandidates.add(candidate);
        }

        Map<Candidate, CandidateMatchRank> matchRanks = new LinkedHashMap<>();
        MatchContext context = new MatchContext();
        for (Candidate candidate : orderedCandidates) {
            matchRanks.put(candidate, candidateMatchRank(candidate, tokenPath, context));
        }

        orderedCandidates.sort(
                // Only rank by specificity after the branch consumed the current path. If all
                // matches are still prefixes, preserve grammar order instead of letting a wrapper
                // branch win just because it predicts fewer continuation tokens.
                Comparator.comparingInt((Candidate candidate) -> matchRanks.get(candidate).completionRank)
                        .thenComparingInt(candidate -> matchRanks.get(candidate).specificityRank)
                        .thenComparingInt(candidate -> matchRanks.get(candidate).leafRank)
                        .thenComparingInt(candidate -> candidate.branchIndex));
        return orderedCandidates;
    }

    private static CandidateMatchRank candidateMatchRank(Candidate candidate, List<String> tokenPath, MatchContext context) {
        NextTokenMatch match = getNextTokenMatch(candidate.elementType, tokenPath, context);
        boolean qualifiedNamePath = isQualifiedNamePath(tokenPath);
        boolean rankIncomplete = qualifiedNamePath || isQualifiedCallPath(tokenPath) || isPlainIdentifierCallArgumentPath(tokenPath);
        boolean completed = match.completed && !qualifiedNamePath;
        int completionRank = completed ? 0 : 1;
        // Incomplete plain call prefixes preserve grammar order so generic wrapper branches do not
        // steal built-in functions. Once a plain identifier call has consumed an argument token,
        // however, grammar order can keep a broad function branch ahead of an equally valid but
        // tighter constructor branch. At that point we rank by structural specificity again.
        // Qualified names are different: a dotted name can be both a complete reference and the
        // prefix of a call, so specificity has to break that tie.
        int specificityRank = completed || rankIncomplete ? match.tokenIds.size() : 0;
        int leafRank = completed || rankIncomplete ? firstPossibleLeafCount(candidate) : 0;
        return new CandidateMatchRank(completionRank, specificityRank, leafRank);
    }

    private static int firstPossibleLeafCount(Candidate candidate) {
        int count = 0;
        for (LeafElementType leaf : candidate.elementType.cache.getFirstPossibleLeafs()) {
            if (!leaf.is(OPTIONAL_WRAPPING)) count++;
        }
        return count;
    }

    private static boolean sameCandidates(TokenCandidates candidates1, TokenCandidates candidates2) {
        return candidates1.candidateKeys().equals(candidates2.candidateKeys());
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

        private Set<String> candidateKeys() {
            return candidates.keySet();
        }

        @Override
        public java.util.Iterator<Candidate> iterator() {
            return candidates.values().iterator();
        }
    }

    private static class CandidateMatchRank {
        private final int completionRank;
        private final int specificityRank;
        private final int leafRank;

        private CandidateMatchRank(int completionRank, int specificityRank, int leafRank) {
            this.completionRank = completionRank;
            this.specificityRank = specificityRank;
            this.leafRank = leafRank;
        }
    }

    private static class MatchContext {
        private final TrieBuildContext trieBuildContext;
        private final List<String> rootTokenPath;
        private final Set<ElementTypeBase> visiting = new HashSet<>();
        private final Map<MatchKey, NextTokenMatch> cache = new LinkedHashMap<>();
        private int cycleHits;

        private MatchContext() {
            this(null, List.of());
        }

        private MatchContext(TrieBuildContext trieBuildContext, List<String> rootTokenPath) {
            this.trieBuildContext = trieBuildContext;
            this.rootTokenPath = List.copyOf(rootTokenPath);
        }

        private boolean isVisiting(ElementTypeBase elementType) {
            return visiting.contains(elementType);
        }

        private void enter(ElementTypeBase elementType) {
            visiting.add(elementType);
        }

        private void exit(ElementTypeBase elementType) {
            visiting.remove(elementType);
        }

        private void registerCycleHit() {
            cycleHits++;
        }

        private void registerMatchCall(ElementTypeBase elementType, List<String> tokenPath, int depth) {
            if (trieBuildContext != null) {
                trieBuildContext.registerMatchCall(rootTokenPath, elementType, tokenPath, depth, cache.size());
            }
        }

        private NextTokenMatch get(ElementTypeBase elementType, List<String> tokenPath, int depth) {
            NextTokenMatch match = cache.get(new MatchKey(elementType, tokenPath, depth));
            return match == null ? null : match.copy();
        }

        private void put(ElementTypeBase elementType, List<String> tokenPath, int depth, NextTokenMatch match) {
            cache.put(new MatchKey(elementType, tokenPath, depth), match.copy());
        }
    }

    private static class MatchKey {
        private final ElementTypeBase elementType;
        private final List<String> tokenPath;
        private final int depth;

        private MatchKey(ElementTypeBase elementType, List<String> tokenPath, int depth) {
            this.elementType = elementType;
            this.tokenPath = List.copyOf(tokenPath);
            this.depth = depth;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof MatchKey matchKey)) return false;

            return elementType == matchKey.elementType &&
                    depth == matchKey.depth &&
                    tokenPath.equals(matchKey.tokenPath);
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(elementType);
            result = 31 * result + tokenPath.hashCode();
            result = 31 * result + depth;
            return result;
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

        private boolean isEmpty() {
            return tokenIds.isEmpty() && !completed;
        }

        private static NextTokenMatch empty() {
            return new NextTokenMatch();
        }

        private static NextTokenMatch completed() {
            NextTokenMatch match = new NextTokenMatch();
            match.completed = true;
            return match;
        }

        private NextTokenMatch copy() {
            NextTokenMatch match = new NextTokenMatch();
            match.tokenIds.addAll(tokenIds);
            match.completed = completed;
            return match;
        }
    }

    private static class TrieBuildContext {
        private final String contextElementId;
        private final String oneOfId;
        private int maxDepth;
        private long trieExpansions;
        private long matchCalls;
        private long prunedSameCandidatePaths;
        private long lastProgressLogNanos = System.nanoTime();

        private TrieBuildContext(String contextElementId, String oneOfId) {
            this.contextElementId = contextElementId;
            this.oneOfId = oneOfId;
        }

        private void registerTokenNode(int depth) {
            maxDepth = Math.max(maxDepth, depth);
        }

        private MatchContext newMatchContext(List<String> tokenPath) {
            return new MatchContext(this, tokenPath);
        }

        private void registerTrieExpansion(
                List<String> tokenPath,
                TokenCandidates tokenCandidates,
                Map<String, TokenCandidates> candidatesByToken) {
            trieExpansions++;
            logProgress(
                    "trie",
                    "path=" + tokenPath(tokenPath) +
                            " candidates=" + tokenCandidates.size() +
                            " nextTokens=" + candidatesByToken.size());
        }

        private void registerMatchCall(
                List<String> rootTokenPath,
                ElementTypeBase elementType,
                List<String> tokenPath,
                int depth,
                int cacheSize) {
            matchCalls++;
            logProgress(
                    "match",
                    "rootPath=" + tokenPath(rootTokenPath) +
                            " tokenPath=" + tokenPath(tokenPath) +
                            " element=" + elementType.getId() +
                            " depth=" + depth +
                            " cache=" + cacheSize);
        }

        private void registerPrunedSameCandidatePath(List<String> tokenPath, String tokenId) {
            prunedSameCandidatePaths++;
            logProgress(
                    "prune",
                    "path=" + tokenPath(tokenPath) +
                            " skipped=" + tokenId);
        }

        private void logProgress(String phase, String details) {
            long now = System.nanoTime();
            if (now - lastProgressLogNanos < PROGRESS_LOG_INTERVAL_NANOS) return;

            lastProgressLogNanos = now;
            log("  Progress one-of " + contextElementId + "/" + oneOfId +
                    " phase=" + phase +
                    " triePaths=" + trieExpansions +
                    " matchCalls=" + matchCalls +
                    " pruned=" + prunedSameCandidatePaths +
                    " maxDepth=" + maxDepth +
                    " " + details);
        }

        private String tokenPath(List<String> tokenPath) {
            return tokenPath.isEmpty() ? "<empty>" : String.join(" ", tokenPath);
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
