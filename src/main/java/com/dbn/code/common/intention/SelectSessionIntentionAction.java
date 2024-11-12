package com.dbn.code.common.intention;

import com.dbn.common.icon.Icons;
import com.dbn.common.util.Context;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.injected.editor.VirtualFileWindow;
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

public class SelectSessionIntentionAction extends EditorIntentionAction  {
    @Override
    public EditorIntentionType getType() {
        return EditorIntentionType.SELECT_SESSION;
    }

    @Override
    @NotNull
    public String getText() {
        return "Set current session";
    }

    @Override
    public Icon getIcon(int flags) {
        return Icons.FILE_SESSION_MAPPING;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if (isDatabaseAssistantPrompt(editor, psiElement, COMMENT)) return false;

        PsiFile psiFile = psiElement.getContainingFile();
        if (!isDbLanguagePsiFile(psiFile)) return false;
        if (!isMainEditor(editor)) return false;

        VirtualFile file = psiFile.getVirtualFile();
        if (file instanceof VirtualFileWindow) return false;

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        if (!contextManager.isSessionSelectable(file)) return false;

        DBLanguagePsiFile dbPsiFile = (DBLanguagePsiFile) psiFile;
        ConnectionHandler connection = dbPsiFile.getConnection();
        if (connection == null || connection.isVirtual()) return false;

        return connection.getSettings().getDetailSettings().isEnableSessionManagement();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        PsiFile psiFile = psiElement.getContainingFile();
        if (psiFile instanceof DBLanguagePsiFile) {
            DBLanguagePsiFile dbLanguageFile = (DBLanguagePsiFile) psiFile;
            DataContext dataContext = Context.getDataContext(editor);
            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
            contextManager.promptSessionSelector(dbLanguageFile.getVirtualFile(), dataContext, null);
        }
    }
}
