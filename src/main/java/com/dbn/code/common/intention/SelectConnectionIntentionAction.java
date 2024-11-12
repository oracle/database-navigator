package com.dbn.code.common.intention;

import com.dbn.common.icon.Icons;
import com.dbn.common.util.Context;
import com.dbn.connection.ConnectionSelectorOptions;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.dbn.assistant.editor.AssistantPrompt.Flavor.COMMENT;
import static com.dbn.common.util.Editors.isMainEditor;
import static com.dbn.common.util.Files.isDbLanguagePsiFile;
import static com.dbn.connection.ConnectionSelectorOptions.Option.SHOW_CREATE_CONNECTION;
import static com.dbn.connection.ConnectionSelectorOptions.Option.SHOW_VIRTUAL_CONNECTIONS;
import static com.dbn.connection.ConnectionSelectorOptions.options;

public class SelectConnectionIntentionAction extends EditorIntentionAction {
    @Override
    public EditorIntentionType getType() {
        return EditorIntentionType.SELECT_CONNECTION;
    }

    @Override
    @NotNull
    public String getText() {
        return "Select connection";
    }

    @Override
    public Icon getIcon(int flags) {
        return Icons.FILE_CONNECTION_MAPPING;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if (isDatabaseAssistantPrompt(editor, psiElement, COMMENT)) return false;

        PsiFile psiFile = psiElement.getContainingFile();
        if (!isDbLanguagePsiFile(psiFile)) return false;
        if (!isMainEditor(editor)) return false;

        VirtualFile file = psiFile.getVirtualFile();
        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        return contextManager.isConnectionSelectable(file);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        PsiFile psiFile = psiElement.getContainingFile();
        if (psiFile instanceof DBLanguagePsiFile) {
            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);

            ConnectionSelectorOptions options = options(
                    SHOW_VIRTUAL_CONNECTIONS,
                    SHOW_CREATE_CONNECTION);

            DataContext dataContext = Context.getDataContext(editor);
            contextManager.promptConnectionSelector(psiFile.getVirtualFile(), dataContext, options, null);
        }
    }
}
