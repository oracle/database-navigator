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

package com.dbn.code.common.intention;

import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.options.ConfigId;
import com.dbn.options.ProjectSettingsManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static com.dbn.assistant.editor.AssistantPrompt.Flavor.COMMENT;
import static com.dbn.assistant.editor.AssistantPrompt.Flavor.SELECTION;

public class SetupCodeCompletionIntentionAction extends EditorIntentionAction {
    @Override
    public EditorIntentionType getType() {
        return EditorIntentionType.EDITOR_SETTINGS;
    }

    @Override
    @NotNull
    public String getText() {
        return "Setup code completion";
    }

    @Override
    public Icon getIcon(int flags) {
        return null;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if (isDatabaseAssistantPrompt(editor, psiElement, COMMENT, SELECTION)) return false;

        PsiFile psiFile = psiElement.getContainingFile();
        return psiFile instanceof DBLanguagePsiFile && psiFile.getVirtualFile().getParent() != null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        ProjectSettingsManager settingsManager = ProjectSettingsManager.getInstance(project);
        settingsManager.openProjectSettings(ConfigId.CODE_COMPLETION);
    }
}