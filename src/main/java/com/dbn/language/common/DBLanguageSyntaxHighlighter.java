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

package com.dbn.language.common;

import com.dbn.common.util.XmlContents;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jdom.Document;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class DBLanguageSyntaxHighlighter extends SyntaxHighlighterBase {
    protected Map colors = new HashMap<>();
    protected Map backgrounds = new HashMap();

    private final DBLanguageDialect languageDialect;
    private final TokenTypeBundle tokenTypes;

    public DBLanguageSyntaxHighlighter(DBLanguageDialect languageDialect, String tokenTypesFile) {
        Document document = loadDefinition(tokenTypesFile);
        tokenTypes = new TokenTypeBundle(languageDialect, document);
        this.languageDialect = languageDialect;
    }

    @SneakyThrows
    private Document loadDefinition(String tokenTypesFile) {
        return XmlContents.fileToDocument(getResourceLookupClass(), tokenTypesFile);
    }

    protected Class getResourceLookupClass() {
        return getClass();
    }

    @NotNull
    protected abstract Lexer createLexer();

    @Override
    @NotNull
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        if (tokenType instanceof SimpleTokenType) {
            SimpleTokenType simpleTokenType = (SimpleTokenType) tokenType;
            return simpleTokenType.getTokenHighlights(() -> pack(
                        getAttributeKeys(tokenType, backgrounds),
                        getAttributeKeys(tokenType, colors)));
        } else {
            return TextAttributesKey.EMPTY_ARRAY;
        }
    }

    private static TextAttributesKey getAttributeKeys(IElementType tokenType, Map map) {
        return (TextAttributesKey) map.get(tokenType);
    }

    @Override
    @NotNull
    public Lexer getHighlightingLexer() {
        return createLexer();
    }
}
