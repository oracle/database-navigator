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

import com.dbn.language.common.element.ElementType;
import com.dbn.vfs.DatabaseFileViewProvider;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class DBLanguageParserDefinition implements ParserDefinition {
    public final DBLanguageParser parser;
    public final SharedTokenTypeBundle sharedTokenTypes;

    public DBLanguageParserDefinition(DBLanguageParser parser) {
        this.parser = parser;
        this.sharedTokenTypes = parser.getTokenTypes().getSharedTokenTypes();
    }

    public boolean isInitialized() {
        return parser.isInitialized();
    }

    @Override
    public PsiElement createElement(ASTNode astNode) {
        IElementType et = astNode.getElementType();
        if(et instanceof ElementType elementType) {
            PsiElement psiElement = elementType.createPsiElement(astNode);
            //return WeakPsiDelegate.wrap(psiElement);
            return psiElement;
        }
        return new ASTWrapperPsiElement(astNode);
    }

    @Override
    @NotNull
    public abstract DBLanguageParser createParser(Project project);

    @NotNull
    @Override
    public IFileElementType getFileNodeType() {
        return getParser().languageDialect.getBaseLanguage().getFileElementType();
        /*DBLanguageDialect languageDialect = parser.getLanguageDialect();
        return languageDialect.getFileElementType();*/
    }

    @Override
    @NotNull
    public TokenSet getWhitespaceTokens() {
        return sharedTokenTypes.whitespaceTokens;
    }

    @Override
    @NotNull
    public TokenSet getCommentTokens() {
        return sharedTokenTypes.commentTokens;
    }

    @Override
    @NotNull
    public TokenSet getStringLiteralElements() {
        return sharedTokenTypes.stringTokens;
    }

    @NotNull
    @Override
    public final PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        if (viewProvider instanceof DatabaseFileViewProvider) {
            // ensure the document is initialized
            // TODO cleanup - causes SOE (may not be required any more)
            //FileDocumentManager.getInstance().getDocument(viewProvider.getVirtualFile());
        }
        return createPsiFile(viewProvider);
    }

    @NotNull
    protected abstract PsiFile createPsiFile(FileViewProvider viewProvider);
}
