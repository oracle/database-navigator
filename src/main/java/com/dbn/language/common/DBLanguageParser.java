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
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.impl.NamedElementType;
import com.dbn.language.common.element.parser.ParserBuilder;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.ParserNode;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jdom.Document;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public abstract class DBLanguageParser implements PsiParser {
    public final DBLanguageDialect languageDialect;
    private final String defaultParseRootId;
    private final String tokenTypesFile;
    private final String elementTypesFile;

    private final @Getter(lazy = true) TokenTypeBundle tokenTypes = loadTokenTypes();
    private final @Getter(lazy = true) ElementTypeBundle elementTypes = loadElementTypes();

    public DBLanguageParser(DBLanguageDialect languageDialect, @NonNls String tokenTypesFile, @NonNls String elementTypesFile, @NonNls String defaultParseRootId) {
        this.languageDialect = languageDialect;
        this.defaultParseRootId = defaultParseRootId;
        this.tokenTypesFile = tokenTypesFile;
        this.elementTypesFile = elementTypesFile;
    }

    @SneakyThrows
    private Document loadDefinition(String tokenTypesFile) {
        return XmlContents.fileToDocument(getResourceLookupClass(), tokenTypesFile);
    }

    private TokenTypeBundle loadTokenTypes() {
        Document document = loadDefinition(tokenTypesFile);
        return new TokenTypeBundle(languageDialect, document);
    }

    private ElementTypeBundle loadElementTypes() {
        Document document = loadDefinition(elementTypesFile);
        return new ElementTypeBundle(languageDialect, getTokenTypes(), document);
    }


    protected Class getResourceLookupClass() {
        return getClass();
    }

    @Override
    @NotNull
    public ASTNode parse(@NotNull IElementType rootElementType, @NotNull PsiBuilder builder) {
        return parse(rootElementType, builder, defaultParseRootId, 9999);
    }

    @NotNull
    public ASTNode parse(IElementType rootElementType, PsiBuilder psiBuilder, String parseRootId, double databaseVersion) {
        ParserContext context = new ParserContext(psiBuilder, languageDialect, databaseVersion);
        ParserBuilder builder = context.builder;
        if (parseRootId == null ) parseRootId = defaultParseRootId;
        PsiBuilder.Marker marker = builder.mark();

        ElementTypeBundle elementTypes = getElementTypes();
        NamedElementType root =  elementTypes.getNamedElementType(parseRootId);
        if (root == null) {
            root = elementTypes.getRootElementType();
        }

        boolean advanced = false;
        ParserNode rootParseNode = new ParserNode(root, null, 0, 0);

        try {
            while (!builder.eof()) {
                int currentOffset =  builder.getOffset();
                root.parser.parse(rootParseNode, context);
                if (currentOffset == builder.getOffset()) {
                    TokenType token = builder.getToken();
                    /*if (tokenType.isChameleon()) {
                        PsiBuilder.Marker injectedLanguageMarker = builder.mark();
                        builder.advanceLexer();
                        injectedLanguageMarker.done((IElementType) tokenType);
                    }
                    else*/ if (token instanceof ChameleonTokenType) {
                        PsiBuilder.Marker injectedLanguageMarker = builder.mark();
                        builder.advance();
                        injectedLanguageMarker.done((IElementType) token);
                    } else {
                        builder.advance();
                    }
                    advanced = true;
                }
            }
        } catch (ParseException e) {
            conditionallyLog(e);
            while (!builder.eof()) {
                builder.advance();
                advanced = true;
            }
        } catch (StackOverflowError e) {
            builder.markerRollbackTo(marker);
            marker = builder.mark();
            while (!builder.eof()) {
                builder.advance();
                advanced = true;
            }

        }

        if (!advanced) builder.advance();
        marker.done(rootElementType);
        return builder.getTreeBuilt();
    }
}
