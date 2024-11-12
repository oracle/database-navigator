package com.dbn.language.common.element.parser;

import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.TokenPairTemplate;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.impl.WrappingDefinition;
import com.dbn.language.common.element.path.ParserNode;
import com.intellij.lang.PsiBuilder.Marker;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class TokenPairMonitor {
    private final Map<TokenPairTemplate, TokenPairStack> stacks;
    private final ParserBuilder builder;

    public TokenPairMonitor(ParserBuilder builder, DBLanguageDialect languageDialect) {
        this.builder = builder;

        TokenPairTemplate[] tokenPairTemplates = languageDialect.getTokenPairTemplates();
        stacks = new EnumMap<>(TokenPairTemplate.class);
        for (TokenPairTemplate tokenPairTemplate : tokenPairTemplates) {
            stacks.put(tokenPairTemplate, new TokenPairStack(builder, languageDialect, tokenPairTemplate));
        }
    }

    protected void consumeBeginTokens(@Nullable ParserNode node) {
        if (node == null) return;

        WrappingDefinition wrapping = node.element.wrapping;
        if (wrapping == null) return;

        TokenElementType beginElement = wrapping.beginElementType;
        TokenType beginToken = beginElement.tokenType;
        while(builder.getToken() == beginToken) {
            Marker beginTokenMarker = builder.mark();
            acknowledge(false);
            builder.advanceInternally();
            beginTokenMarker.done(beginElement);
        }
    }

    protected void consumeEndTokens(@Nullable ParserNode node) {
        if (node == null) return;

        WrappingDefinition wrapping = node.element.wrapping;
        if (wrapping == null) return;

        TokenElementType endElement = wrapping.endElementType;
        TokenType endToken = endElement.tokenType;
        while (builder.getToken() == endToken && !isExplicitRange(endToken)) {
            Marker endTokenMarker = builder.mark();
            acknowledge(false);
            builder.advanceInternally();
            endTokenMarker.done(endElement);
        }
    }

    protected void acknowledge(boolean explicit) {
        TokenType token = builder.getToken();
        TokenPairStack tokenPairStack = getStack(token);
        if (tokenPairStack != null) {
            tokenPairStack.acknowledge(explicit);
        }
    }

    public void cleanup() {
        for (TokenPairStack tokenPairStack : stacks.values()) {
            tokenPairStack.cleanup(true);
        }

    }

    public void rollback() {
        for (TokenPairStack tokenPairStack : stacks.values()) {
            tokenPairStack.rollback();
        }
    }

    @Nullable
    private TokenPairStack getStack(TokenType tokenType) {
        if (tokenType == null) return null;

        TokenPairTemplate template = tokenType.getTokenPairTemplate();
        if (template != null) {
            return stacks.get(template);
        }
        return null;
    }

    public boolean isExplicitRange(TokenType tokenType) {
        TokenPairStack stack = getStack(tokenType);
        return stack != null && stack.isExplicitRange();
    }

    public void setExplicitRange(TokenType tokenType, boolean value) {
        TokenPairStack stack = getStack(tokenType);
        if (stack != null) stack.setExplicitRange(value);
    }
}
