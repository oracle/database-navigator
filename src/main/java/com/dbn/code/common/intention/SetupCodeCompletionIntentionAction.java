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

import javax.swing.*;

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