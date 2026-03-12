/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.language.common.element.parser;

import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.TokenTypeBundle;
import com.dbn.language.common.element.TokenPairTemplate;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.impl.WrappingDefinition;
import com.intellij.lang.PsiBuilder.Marker;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TokenPairMonitor extends ParserBuilderExtension {
    private final Map<TokenPairTemplate, TokenPairStack> stacks;
    private final Set<TokenType> tokens;

    public TokenPairMonitor(ParserBuilder builder, DBLanguageDialect languageDialect) {
        super(builder);

        TokenPairTemplate[] tokenPairTemplates = languageDialect.getTokenPairTemplates();
        stacks = new EnumMap<>(TokenPairTemplate.class);
        tokens = new HashSet<>();

        TokenTypeBundle tokenTypes = languageDialect.getParserTokenTypes();
        for (TokenPairTemplate tokenPairTemplate : tokenPairTemplates) {
            stacks.put(tokenPairTemplate, new TokenPairStack(builder, languageDialect, tokenPairTemplate));
            tokens.add(tokenTypes.getTokenType(tokenPairTemplate.getBeginToken()));
            tokens.add(tokenTypes.getTokenType(tokenPairTemplate.getEndToken()));
        }

    }

    public boolean isConsumedMatch(TokenType tokenType) {
        if (builder.getToken() == tokenType) return false;
        if (builder.getPreviousToken() != tokenType) return false;
        if (!tokens.contains(tokenType)) return false;

        TokenPairStack stack = getStack(tokenType);
        if (stack == null) return false;

        if (tokenType == stack.endToken) return true;
        if (tokenType == stack.beginToken) return !stack.isExplicitRange();

        return false;
    }

    public boolean hasConsumedMatch(ElementTypeBase elementType) {
        for (TokenType token : tokens) {
            if (!isConsumedMatch(token)) continue;
            if (!elementType.cache.couldStartWithToken(token)) continue;
            return true;
        }

        return false;
    }

    protected void consumeBeginTokens(ElementTypeBase element) {
        WrappingDefinition wrapping = element.wrapping;
        if (wrapping == null) return;
        if (!wrapping.optional) return;

        TokenElementType beginElement = wrapping.beginElement;
        while(builder.getToken() == beginElement.tokenType) {
            if (!acknowledge(beginElement, false)) return;

            Marker beginTokenMarker = builder.markAndAdvance();
            builder.tokenMonitor.markResolved(beginElement);
            beginTokenMarker.done(beginElement);
        }
    }

    protected void consumeEndTokens(ElementTypeBase element) {
        WrappingDefinition wrapping = element.wrapping;
        if (wrapping == null) return;
        if (!wrapping.optional) return;

        TokenElementType endElement = wrapping.endElement;
        while (builder.getToken() == endElement.tokenType) {
            if (!acknowledge(endElement, false)) return;

            Marker endTokenMarker = builder.markAndAdvance();
            builder.tokenMonitor.markResolved(endElement);
            endTokenMarker.done(endElement);
        }
    }

    public boolean acknowledge(LeafElementType leafElement, boolean borrowed) {
        TokenPairStack stack = getStack(leafElement.tokenType);
        if (stack == null) return  false;

        return stack.acknowledge(leafElement, borrowed);
    }

    public void reset() {
        for (TokenPairStack tokenPairStack : stacks.values()) {
            tokenPairStack.reset();
        }
    }

    public void rollback(ElementTypeBase element) {
        if (element == null) return;
        if (element.wrapping == null) {
            Set<TokenPairTemplate> tokenPairs = element.cache.getFirstPossibleTokenPairs();
            for (TokenPairTemplate tokenPair : tokenPairs) {
                TokenPairStack stack = stacks.get(tokenPair);
                if (stack == null) continue;

                stack.rollback(element);
                return;
            }
        } else {
            TokenPairStack stack = stacks.get(element.wrapping.template);
            if (stack == null) return;

            stack.rollback(element);
        }
    }

    @Nullable
    private TokenPairStack getStack(TokenType tokenType) {
        if (tokenType == null) return null;

        TokenPairTemplate template = tokenType.getTokenPairTemplate();
        if (template == null) return null;

        return stacks.get(template);
    }

    public boolean isExplicitRange(TokenType tokenType) {
        TokenPairStack stack = getStack(tokenType);
        return stack != null && stack.isExplicitRange();
    }
}
