package com.dbn.language.common.element.parser;

import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.cache.ElementLookupContext;
import com.dbn.language.common.element.impl.LeafElementType;
import com.intellij.lang.PsiBuilder;

public class ParserContext extends ElementLookupContext {
    public final long timestamp = System.currentTimeMillis();
    public final ParserBuilder builder;
    public transient LeafElementType lastResolvedLeaf;
    private TokenType wavedTokenType;
    private int wavedTokenTypeOffset;

    public ParserContext(PsiBuilder builder, DBLanguageDialect languageDialect, double databaseVersion) {
        super(null, databaseVersion);
        this.builder = new ParserBuilder(builder, languageDialect);
    }

    public boolean isWavedTokenType(TokenType tokenType) {
        return tokenType == wavedTokenType && builder.getOffset() == wavedTokenTypeOffset;
    }

    public void setWavedTokenType(TokenType wavedTokenType) {
        this.wavedTokenType = wavedTokenType;
        this.wavedTokenTypeOffset = builder.getOffset();
    }
}
