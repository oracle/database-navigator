package com.dbn.language.common.element.parser;

import com.dbn.code.common.completion.CodeCompletionContributor;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.TokenPairTemplate;
import com.dbn.language.common.element.path.ParserNode;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

public final class ParserBuilder {
    private final PsiBuilder builder;
    public final TokenPairMonitor tokenPairMonitor;
    private final ParseErrorMonitor errorMonitor;
    private final Cache cache = new Cache();

    public ParserBuilder(PsiBuilder builder, DBLanguageDialect languageDialect) {
        this.builder = builder;
        this.builder.setDebugMode(false);
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
        tokenPairMonitor.consumeBeginTokens(node);
        return builder.mark();
    }

    public void markError(String message) {
        if (!errorMonitor.isErrorAtOffset()) {
            errorMonitor.markError();
            Marker errorMaker = builder.mark();
            errorMaker.error(message);
        }
    }

    public void markerRollbackTo(Marker marker) {
        if (marker != null) {
            marker.rollbackTo();
            errorMonitor.reset();
            tokenPairMonitor.rollback();
            cache.reset();
        }
    }

    public void markerDone(Marker marker, ElementType elementType) {
        markerDone(marker, elementType, null);
    }

    public void markerDone(Marker marker, ElementType elementType, @Nullable ParserNode node) {
        if (marker != null) {
            tokenPairMonitor.consumeEndTokens(node);
            marker.done((IElementType) elementType);
        }
    }

    public void markerDrop(Marker marker) {
        if (marker != null) {
            marker.drop();
        }
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
