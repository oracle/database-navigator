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

import com.dbn.code.common.completion.CodeCompletionContributor;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.language.common.element.TokenPairTemplate;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.path.ParserNode;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

public final class ParserBuilder {
    private final PsiBuilder builder;
    private final Cache cache = new Cache();

    public final TokenMonitor tokenMonitor;
    public final TokenPairMonitor tokenPairMonitor;
    public final ParseErrorMonitor errorMonitor;

    public ParserBuilder(PsiBuilder builder, DBLanguageDialect languageDialect) {
        this.builder = builder;
        this.builder.setDebugMode(false);
        this.tokenMonitor = new TokenMonitor(this);
        this.tokenPairMonitor = new TokenPairMonitor(this, languageDialect);
        this.errorMonitor = new ParseErrorMonitor(this);
    }

    public ASTNode getTreeBuilt() {
        tokenPairMonitor.cleanup();
        return builder.getTreeBuilt();
    }

    public Marker markAndAdvance() {
        Marker marker = mark();
        advance();
        return marker;
    }

    public void advance() {
        tokenPairMonitor.acknowledge(true);
        advanceInternally();
    }

    public void advanceInternally() {
        builder.advanceLexer();
        cache.reset();
    }

    @Nullable
    public TokenPairTemplate getTokenPairTemplate() {
        TokenType token = getToken();
        return token == null ? null : token.getTokenPairTemplate();
    }

    /****************************************************
     *                 Cached  lookups                  *
     ****************************************************/

    @Nullable
    public TokenType getToken() {
        TokenType currentToken = cache.getCurrentToken();
        if (currentToken == null) return null;
        if (currentToken.isChameleon()) return null;
        return currentToken;
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

    public boolean isDummyToken(){
        return cache.isDummyToken();
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
        if (marker == null) return;

        marker.rollbackTo();
        errorMonitor.reset();
        tokenPairMonitor.rollback();
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
        return "position=" + getOffset() + ", surrogate=" + tokenMonitor.getLastSurrogate() + ", token=" + getToken() + "('" + getTokenText() + "')";
    }

    private class Cache {
        private String tokenText;
        private Boolean dummyToken;
        private TokenType currentToken;
        private TokenType previousToken;
        private TokenType nextToken;

        public void reset() {
            currentToken = null;
            tokenText = null;
            dummyToken = null;
            previousToken = null;
            nextToken = null;
        }

        public TokenType getPreviousToken() {
            if (previousToken == null) {
                previousToken = lookBack(1);
            }
            return previousToken;
        }

        public TokenType getCurrentToken() {
            if (currentToken == null) {
                IElementType tokenType = builder.getTokenType();
                currentToken = tokenType instanceof TokenType ? (TokenType) tokenType : null;
            }
            return currentToken;
        }

        public TokenType getNextToken() {
            if (nextToken == null) {
                nextToken = lookAhead(1);
            }
            return nextToken;
        }

        public String getTokenText() {
            if (tokenText == null) {
                tokenText = builder.getTokenText();
            }
            return tokenText;
        }

        public boolean isDummyToken() {
            if (dummyToken == null) {
                String tokenText = getTokenText();
                dummyToken = tokenText != null && tokenText.contains(CodeCompletionContributor.DUMMY_TOKEN) ? Boolean.TRUE : Boolean.FALSE;
            }
            return dummyToken == Boolean.TRUE;
        }
    }
}
