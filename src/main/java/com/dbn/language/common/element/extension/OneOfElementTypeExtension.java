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

package com.dbn.language.common.element.extension;

import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.TokenPairTemplate;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.OneOfElementType;
import com.dbn.language.common.element.parser.ParserContext;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

@Slf4j
public class OneOfElementTypeExtension extends ElementTypeExtensionBase<OneOfElementType> {
    private static final ElementTypeRef[] EMPTY_CANDIDATES = new ElementTypeRef[0];
    private static final LeafElementType[] EMPTY_LEAFS = new LeafElementType[0];
    private static final String TAG_NODE = "node";
    private static final String ATTR_TOKEN_TYPE_IDS = "tt";
    private static final String ATTR_PARSE_CANDIDATE_IDS = "pc";
    private static final String ATTR_COMPLETION_CANDIDATE_IDS = "cc";
    private static final String ATTR_NODE_ID = "id";
    private static final String ATTR_NODE_REF = "ref";

    public final ElementTypeRef[] defaultCandidates;
    public final Map<String, TokenNode> tokens;
    private final Map<String, Element> nodeDefinitions;
    private final Map<String, TokenNode> tokenNodes = new HashMap<>();
    private final TokenType[] optionalWrappingBeginTokens;
    private final ElementTypeRef[][] optionalWrappingCandidates;

    public OneOfElementTypeExtension(OneOfElementType elementType, Element definition) {
        super(elementType, definition);
        defaultCandidates = elementType.children;
        nodeDefinitions = loadNodeDefinitions(definition);
        tokens = loadTokens(definition, EMPTY_CANDIDATES);
        TokenPairTemplate[] templates = elementType.getLanguageDialect().getTokenPairTemplates();
        optionalWrappingBeginTokens = new TokenType[templates.length];
        optionalWrappingCandidates = new ElementTypeRef[templates.length][];
        for (int i = 0; i < templates.length; i++) {
            TokenType beginToken = elementType.bundle.tokenTypeBundle.getTokenType(templates[i].getBeginToken());
            optionalWrappingBeginTokens[i] = beginToken;
            optionalWrappingCandidates[i] = loadOptionalWrappingCandidates(beginToken);
        }
    }

    public ElementTypeRef[] parseCandidates(ParserContext context) {
        ElementTypeRef[] candidates = parseTrieCandidates(context);
        TokenType beginToken = context.builder.tokenPairMonitor.getConsumedOptionalBegin();
        if (beginToken == null) return candidates;

        ElementTypeRef[] wrappingCandidates = getOptionalWrappingCandidates(beginToken);
        return wrappingCandidates.length == 0 ? candidates : mergeCandidates(candidates, wrappingCandidates);
    }

    private ElementTypeRef[] parseTrieCandidates(ParserContext context) {
        TokenNode candidateNode = null;
        Map<String, TokenNode> nodes = tokens;

        for (int i = 0; i < depth; i++) {
            TokenType token = i == 0 ? context.builder.getToken() : context.builder.lookAhead(i);
            if (token == null) break;

            TokenNode next = nodes.get(token.getId());
            if (next == null) break;

            if (next.candidates.length > 0) {
                candidateNode = next;
            }
            nodes = next.tokens;
        }

        return candidateNode == null ? defaultCandidates : candidateNode.candidates;
    }

    private ElementTypeRef[] loadOptionalWrappingCandidates(TokenType beginToken) {
        int count = 0;
        for (ElementTypeRef candidate : defaultCandidates) {
            if (candidate.elementType.cache.couldStartWithToken(beginToken)) {
                count++;
            }
        }
        if (count == 0) return EMPTY_CANDIDATES;

        ElementTypeRef[] candidates = new ElementTypeRef[count];
        int index = 0;
        for (ElementTypeRef candidate : defaultCandidates) {
            if (candidate.elementType.cache.couldStartWithToken(beginToken)) {
                candidates[index++] = candidate;
            }
        }
        return candidates;
    }

    private ElementTypeRef[] getOptionalWrappingCandidates(TokenType beginToken) {
        for (int i = 0; i < optionalWrappingBeginTokens.length; i++) {
            if (optionalWrappingBeginTokens[i] == beginToken) {
                return optionalWrappingCandidates[i];
            }
        }
        return EMPTY_CANDIDATES;
    }

    private ElementTypeRef[] mergeCandidates(ElementTypeRef[] candidates, ElementTypeRef[] wrappingCandidates) {
        int count = 0;
        boolean ordered = true;
        for (ElementTypeRef candidate : defaultCandidates) {
            if (contains(candidates, candidate) || contains(wrappingCandidates, candidate)) {
                ordered &= count < candidates.length && candidates[count] == candidate;
                count++;
            }
        }
        if (ordered && count == candidates.length) return candidates;

        ElementTypeRef[] result = new ElementTypeRef[count];
        int index = 0;
        for (ElementTypeRef candidate : defaultCandidates) {
            if (contains(candidates, candidate) || contains(wrappingCandidates, candidate)) {
                result[index++] = candidate;
            }
        }
        return result;
    }

    private static boolean contains(ElementTypeRef[] candidates, ElementTypeRef candidate) {
        for (ElementTypeRef existing : candidates) {
            if (existing == candidate) return true;
        }
        return false;
    }

    public LeafElementType[] nextLeafs(List<TokenType> tokenPath) {
        if (tokenPath.isEmpty()) return EMPTY_LEAFS;

        TokenNode candidateNode = null;
        Map<String, TokenNode> nodes = tokens;

        for (TokenType token : tokenPath) {
            if (token == null) break;

            TokenNode next = nodes.get(token.getId());
            if (next == null) return EMPTY_LEAFS;

            candidateNode = next;
            nodes = next.tokens;
        }

        return candidateNode == null ? EMPTY_LEAFS : candidateNode.nextLeafs;
    }

    private ElementTypeRef resolveCandidate(String candidateId) {
        OneOfElementType elementType = this.elementType;
        for (ElementTypeRef child : elementType.children) {
            if (candidateId.equals(child.elementType.getId())) {
                return child;
            }
        }

        log.warn("DBN - [{}] unresolved one-of extension candidate '{}' (one-of = {})",
                elementType.getLanguageDialect().getID(), candidateId, elementType.getId());
        return null;
    }

    private LeafElementType resolveLeaf(String leafId) {
        OneOfElementType elementType = this.elementType;
        ElementTypeBase leaf = elementType.bundle.getBuilder().getElementType(leafId);
        if (leaf instanceof LeafElementType leafElementType) {
            return leafElementType;
        }

        log.warn("DBN - [{}] unresolved one-of extension next leaf '{}' (one-of = {})",
                elementType.getLanguageDialect().getID(), leafId, elementType.getId());
        return null;
    }

    public class TokenNode {
        public final ElementTypeRef[] candidates;
        public final LeafElementType[] nextLeafs;
        public final Map<String, TokenNode> tokens;

        private TokenNode(Element definition, ElementTypeRef[] inheritedCandidates) {
            this.candidates = loadCandidates(definition, inheritedCandidates);
            this.nextLeafs = loadNextLeafs(definition);
            this.tokens = loadTokens(definition, candidates);
        }

        private ElementTypeRef[] loadCandidates(
                Element definition,
                ElementTypeRef[] inheritedCandidates) {
            List<String> candidateIds = unmodifiableList(csvAttribute(definition, ATTR_PARSE_CANDIDATE_IDS));
            if (candidateIds.isEmpty()) return inheritedCandidates;

            List<ElementTypeRef> candidates = new ArrayList<>(candidateIds.size());
            for (String candidateId : candidateIds) {
                ElementTypeRef candidate = resolveCandidate(candidateId);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }
            return candidates.toArray(EMPTY_CANDIDATES);
        }

        private LeafElementType[] loadNextLeafs(Element definition) {
            List<String> nextLeafIds = unmodifiableList(csvAttribute(definition, ATTR_COMPLETION_CANDIDATE_IDS));
            if (nextLeafIds.isEmpty()) return EMPTY_LEAFS;

            List<LeafElementType> nextLeafs = new ArrayList<>(nextLeafIds.size());
            for (String nextLeafId : nextLeafIds) {
                LeafElementType nextLeaf = resolveLeaf(nextLeafId);
                if (nextLeaf != null) {
                    nextLeafs.add(nextLeaf);
                }
            }
            return nextLeafs.toArray(EMPTY_LEAFS);
        }
    }

    private Map<String, TokenNode> loadTokens(
            Element definition,
            ElementTypeRef[] inheritedCandidates) {
        List<Element> tokenElements = definition.getChildren(TAG_NODE);
        if (tokenElements.isEmpty()) return Map.of();

        Map<String, TokenNode> tokens = new LinkedHashMap<>();
        for (Element tokenElement : tokenElements) {
            Element nodeDefinition = resolveNodeDefinition(tokenElement);
            List<String> tokenTypeIds = unmodifiableList(csvAttribute(nodeDefinition, ATTR_TOKEN_TYPE_IDS));
            if (tokenTypeIds.isEmpty()) {
                throw new IllegalStateException("One-of extension node has no token types");
            }
            TokenNode tokenNode = loadTokenNode(nodeDefinition, inheritedCandidates);
            for (String tokenTypeId : tokenTypeIds) {
                tokens.put(tokenTypeId.intern(), tokenNode);
            }
        }
        return unmodifiableMap(tokens);
    }

    private TokenNode loadTokenNode(
            Element definition,
            ElementTypeRef[] inheritedCandidates) {
        String nodeId = definition.getAttributeValue(ATTR_NODE_ID);
        if (nodeId != null) {
            TokenNode tokenNode = tokenNodes.get(nodeId);
            if (tokenNode != null) return tokenNode;
        }

        TokenNode tokenNode = new TokenNode(definition, inheritedCandidates);
        if (nodeId != null) {
            tokenNodes.put(nodeId, tokenNode);
        }
        return tokenNode;
    }

    private Element resolveNodeDefinition(Element definition) {
        String nodeRef = definition.getAttributeValue(ATTR_NODE_REF);
        if (nodeRef == null) return definition;

        Element nodeDefinition = nodeDefinitions.get(nodeRef);
        if (nodeDefinition == null) {
            throw new IllegalStateException("Unresolved one-of extension node reference " + nodeRef);
        }
        return nodeDefinition;
    }

    private static Map<String, Element> loadNodeDefinitions(Element definition) {
        Map<String, Element> nodeDefinitions = new HashMap<>();
        collectNodeDefinitions(definition, nodeDefinitions);
        return unmodifiableMap(nodeDefinitions);
    }

    private static void collectNodeDefinitions(
            Element definition,
            Map<String, Element> nodeDefinitions) {
        for (Element node : definition.getChildren(TAG_NODE)) {
            String nodeId = node.getAttributeValue(ATTR_NODE_ID);
            if (nodeId != null && nodeDefinitions.put(nodeId, node) != null) {
                throw new IllegalStateException("Duplicate one-of extension node id " + nodeId);
            }
            collectNodeDefinitions(node, nodeDefinitions);
        }
    }
}
