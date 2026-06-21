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
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.OneOfElementType;
import com.dbn.language.common.element.parser.ParserContext;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.stringAttribute;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

@Slf4j
public class OneOfElementTypeExtension extends ElementTypeExtensionBase<OneOfElementType> {
    private static final ElementTypeRef[] EMPTY_CANDIDATES = new ElementTypeRef[0];

    public final ElementTypeRef[] defaultCandidates;
    public final Map<String, TokenNode> tokens;

    public OneOfElementTypeExtension(OneOfElementType elementType, Element definition) {
        super(elementType, definition);
        defaultCandidates = elementType.children;
        tokens = loadTokens(definition);
    }

    public int pathDepth(ParserContext context) {
        Map<String, TokenNode> nodes = tokens;
        int matchedDepth = 0;

        for (int i = 0; i < depth; i++) {
            TokenType token = i == 0 ? context.builder.getToken() : context.builder.lookAhead(i);
            if (token == null) break;

            TokenNode next = nodes.get(token.getId());
            if (next == null) break;

            matchedDepth = i + 1;
            nodes = next.tokens;
        }

        return matchedDepth;
    }

    public ElementTypeRef[] parseCandidates(ParserContext context) {
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

    private Map<String, TokenNode> loadTokens(Element definition) {
        List<Element> tokenElements = definition.getChildren("token");
        if (tokenElements.isEmpty()) return Map.of();

        Map<String, TokenNode> tokens = new LinkedHashMap<>();
        for (Element tokenElement : tokenElements) {
            TokenNode tokenNode = new TokenNode(tokenElement);
            tokens.put(tokenNode.typeId, tokenNode);
        }
        return unmodifiableMap(tokens);
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

    public class TokenNode {
        public final String typeId;
        public final List<String> tokenIds;
        public final List<String> candidateIds;
        public final ElementTypeRef[] candidates;
        public final Map<String, TokenNode> tokens;

        private TokenNode(Element definition) {
            this.typeId = stringAttribute(definition, "type-id");
            this.tokenIds = unmodifiableList(csvAttribute(definition, "token-ids"));
            this.candidateIds = unmodifiableList(csvAttribute(definition, "candidate-ids"));
            this.candidates = loadCandidates(candidateIds);
            this.tokens = loadTokens(definition);
        }

        private ElementTypeRef[] loadCandidates(List<String> candidateIds) {
            if (candidateIds.isEmpty()) return EMPTY_CANDIDATES;

            List<ElementTypeRef> candidates = new ArrayList<>(candidateIds.size());
            for (String candidateId : candidateIds) {
                ElementTypeRef candidate = resolveCandidate(candidateId);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }
            return candidates.toArray(EMPTY_CANDIDATES);
        }
    }

}
