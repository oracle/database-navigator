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

package com.dbn.language.common.element.parser;

import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.path.ParserNode;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

import static com.dbn.code.common.completion.CodeCompletionContributor.DUMMY_TOKEN;
import static com.dbn.common.util.Traces.isCalledThroughClass;

public final class ParserBuilder {
    private final PsiBuilder builder;
    private final Cache cache = new Cache();

    public final TokenMonitor tokenMonitor;
    public final TokenPairMonitor tokenPairMonitor;
    public final ParseErrorMonitor errorMonitor;
    public final boolean codeCompletion;

    public ParserBuilder(PsiBuilder builder, DBLanguageDialect languageDialect) {
        this.builder = builder;
        this.builder.setDebugMode(false);
        this.tokenMonitor = new TokenMonitor(this);
        this.tokenPairMonitor = new TokenPairMonitor(this, languageDialect);
        this.errorMonitor = new ParseErrorMonitor(this);
        this.codeCompletion = isCodeCompletion();
    }

    private static boolean isCodeCompletion() {
        return isCalledThroughClass(c -> c.getName().startsWith("com.intellij.codeInsight.completion"), 30);
    }

    public ASTNode getTreeBuilt() {
        tokenPairMonitor.reset();
        return builder.getTreeBuilt();
    }

    public Marker markAndAdvance() {
        Marker marker = mark();
        advance();
        return marker;
    }

    public void advance() {
        builder.advanceLexer();
        cache.reset();
    }

    /****************************************************
     *                 Cached  lookups                  *
     ****************************************************/

    @Nullable
    public TokenType getToken() {
        return cache.getCurrentToken();
    }

    public TokenType getPreviousToken() {
        return cache.getPreviousToken();
    }

    public TokenType getNextToken() {
        return cache.getNextToken();
    }

    public String getTokenText() {
        return cache.getTokenText();
    }

    public boolean isDummyToken() {
        return codeCompletion && cache.isDummyToken();
    }

    public boolean eof() {
        return builder.eof();
    }

    public int getOffset() {
        return builder.getCurrentOffset();
    }

    @Nullable
    public TokenType lookAhead(int steps) {
        IElementType elementType = builder.lookAhead(steps);
        return elementType instanceof TokenType ? (TokenType) elementType : null;
    }


    private TokenType lookBack(int steps) {
        int cursor = -1;
        int count = 0;
        TokenType tokenType = (TokenType) builder.rawLookup(cursor);
        while (tokenType != null && count <= steps) {
            TokenTypeCategory category = tokenType.getCategory();
            if (category != TokenTypeCategory.WHITESPACE && category != TokenTypeCategory.COMMENT) {
                count++;
                if (count == steps) return tokenType;
            }
            cursor--;
            tokenType = (TokenType) builder.rawLookup(cursor);
        }
        return null;
    }

    /****************************************************
     *                 Marker utilities                 *
     ****************************************************/
    public boolean isErrorAtOffset() {
        return errorMonitor.isErrorAtOffset();
    }

    public void error(String messageText) {
        builder.error(messageText);
    }

    public Marker mark(){
        return builder.mark();
    }

    public Marker mark(ParserNode node){
        tokenPairMonitor.consumeBeginTokens(node.element);
        return builder.mark();
    }

    public void markError(String message) {
        if (errorMonitor.isErrorAtOffset()) return;

        errorMonitor.markError();
        Marker errorMaker = builder.mark();
        errorMaker.error(message);
    }

    public void markerRollbackTo(Marker marker) {
        markerRollbackTo(marker, null);
    }
    public void markerRollbackTo(Marker marker, ElementTypeBase elementType) {
        if (marker == null) return;

        marker.rollbackTo();
        errorMonitor.reset();
        tokenPairMonitor.rollback(elementType);
        cache.reset();
    }

    public void markerDone(Marker marker, ElementTypeBase elementType) {
        if (marker == null) return;

        tokenPairMonitor.consumeEndTokens(elementType);
        marker.done(elementType);
    }

    public void markerDrop(Marker marker) {
        if (marker == null) return;

        marker.drop();
    }

    @Override
    public String toString() {
        return "position=" + getOffset() + ", token=" + getToken() + "('" + getTokenText() + "')";
    }

    private class Cache {
        private String tokenText;
        private TokenType currentToken;
        private TokenType previousToken;
        private TokenType nextToken;
        private boolean dummyToken;
        private boolean dummyTokenResolved;
        private boolean currentTokenResolved;
        private boolean previousTokenResolved;
        private boolean nextTokenResolved;

        public void reset() {
            currentToken = null;
            tokenText = null;
            previousToken = null;
            nextToken = null;
            dummyTokenResolved = false;
            currentTokenResolved = false;
            previousTokenResolved = false;
            nextTokenResolved = false;
        }

        public TokenType getPreviousToken() {
            if (previousTokenResolved) return previousToken;

            previousToken = lookBack(1);
            previousTokenResolved = true;
            return previousToken;
        }

        public TokenType getCurrentToken() {
            if (currentTokenResolved) return currentToken;

            IElementType tokenType = builder.getTokenType();
            if (tokenType instanceof TokenType type && !type.isChameleon()) {
                currentToken = type;
            }
            currentTokenResolved = true;
            return currentToken;
        }

        public TokenType getNextToken() {
            if (nextTokenResolved) return nextToken;

            nextToken = lookAhead(1);
            nextTokenResolved = true;
            return nextToken;
        }

        public String getTokenText() {
            if (tokenText != null) return tokenText;
            return tokenText = builder.getTokenText();
        }

        public boolean isDummyToken() {
            if (!codeCompletion) return false;
            if (dummyTokenResolved) return dummyToken;

            String tokenText = getTokenText();
            dummyToken = tokenText != null && tokenText.endsWith(DUMMY_TOKEN);
            dummyTokenResolved = true;
            return dummyToken;
        }
    }

    public String getCurrentContext() {
        CharSequence text = builder.getOriginalText();
        String tokenText = builder.getTokenText();
        if (tokenText == null) return "";

        int currentOffset = builder.getCurrentOffset();
        String left = text.subSequence(
                Math.max(0, currentOffset-20), currentOffset).toString();
        String right = text.subSequence(
                currentOffset + tokenText.length(), Math.min(currentOffset + tokenText.length() + 20, text.length()-1)).toString();

        return left + " _____ " + tokenText + " _____ " + right;
    }
}
