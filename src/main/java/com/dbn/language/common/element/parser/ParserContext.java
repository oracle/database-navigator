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
