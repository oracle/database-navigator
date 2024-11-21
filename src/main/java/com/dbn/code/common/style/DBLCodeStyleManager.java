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

package com.dbn.code.common.style;

import com.dbn.DatabaseNavigator;
import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.util.Documents;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.language.common.psi.LeafPsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.language.common.psi.TokenPsiElement;
import com.intellij.lang.Language;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.util.Documents.getEditors;

@State(
    name = DBLCodeStyleManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DBLCodeStyleManager extends ProjectComponentBase implements PersistentState {

    public static final String COMPONENT_NAME = "DBNavigator.Project.CodeStyleManager";

    private DBLCodeStyleManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static DBLCodeStyleManager getInstance(@NotNull Project project) {
        return projectService(project, DBLCodeStyleManager.class);
    }

    public void formatCase(@NotNull PsiFile file) {
        Document document = Documents.getDocument(file);
        if (document == null || !document.isWritable()) return;

        Editor[] editors = getEditors(document);
        if (editors.length == 1) {
            Editor editor = editors[0];
            SelectionModel selectionModel = editor.getSelectionModel();
            int selectionStart = selectionModel.getSelectionStart();
            int selectionEnd = selectionModel.getSelectionEnd();
            format(document, file, selectionStart, selectionEnd);
        }
    }

    public CodeStyleCaseSettings getCodeStyleCaseSettings(DBLanguage language) {
        return language.codeStyleSettings(getProject()).getCaseSettings();
    }

    private void format(Document document, PsiElement psiElement, int startOffset, int endOffset){
        Language language = PsiUtil.getLanguage(psiElement);
        if (!(language instanceof DBLanguage)) return;

        CodeStyleCaseSettings styleCaseSettings = getCodeStyleCaseSettings((DBLanguage) language);
        PsiElement child = psiElement.getFirstChild();
        while (child != null) {
            if (child instanceof LeafPsiElement) {
                TextRange textRange = child.getTextRange();
                boolean isInRange =
                        startOffset == endOffset || (
                                textRange.getStartOffset() >= startOffset &&
                                        textRange.getEndOffset() <= endOffset);
                if (isInRange) {
                    CodeStyleCaseOption caseOption = null;
                    if (child instanceof IdentifierPsiElement) {
                        IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) child;
                        if (identifierPsiElement.isObject() && !identifierPsiElement.isQuoted()) {
                            caseOption = styleCaseSettings.getObjectCaseOption();
                        }
                    }
                    else if (child instanceof TokenPsiElement) {
                        TokenPsiElement tokenPsiElement = (TokenPsiElement) child;
                        TokenType tokenType = tokenPsiElement.getTokenType();
                        caseOption =
                                tokenType.isKeyword() ? styleCaseSettings.getKeywordCaseOption() :
                                        tokenType.isFunction() ? styleCaseSettings.getFunctionCaseOption() :
                                                tokenType.isParameter() ? styleCaseSettings.getParameterCaseOption() :
                                                        tokenType.isDataType() ? styleCaseSettings.getDatatypeCaseOption() : null;
                    }

                    if (caseOption != null) {
                        String text = child.getText();
                        String newText = caseOption.format(text);

                        if (newText != null && !Objects.equals(newText, text))
                            document.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), newText);

                    }
                }
            } else {
                format(document, child, startOffset, endOffset);
            }
            child = child.getNextSibling();
        }
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {

    }
}