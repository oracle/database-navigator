package com.dbn.code.common.intention;

import com.dbn.common.icon.Icons;
import com.dbn.execution.script.ScriptExecutionManager;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.dbn.common.dispose.Checks.allValid;
import static com.dbn.common.util.Editors.isMainEditor;
import static com.dbn.common.util.Files.isDbLanguagePsiFile;
import static com.dbn.connection.mapping.FileConnectionContextManager.hasConnectivityContext;
import static com.dbn.debugger.DatabaseDebuggerManager.isDebugConsole;

public class ExecuteScriptIntentionAction extends EditorIntentionAction  {
    @Override
    public EditorIntentionType getType() {
        return EditorIntentionType.EXECUTE_SCRIPT;
    }

    @Override
    @NotNull
    public String getText() {
        return "Execute SQL script";
    }


    @Override
    public Icon getIcon(int flags) {
        return Icons.EXECUTE_SQL_SCRIPT;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if (isDatabaseAssistantPrompt(editor, psiElement)) return false;

        PsiFile psiFile = psiElement.getContainingFile();
        if (!isDbLanguagePsiFile(psiFile)) return false;

        VirtualFile file = psiFile.getVirtualFile();
        if (file instanceof VirtualFileWindow) return false;
        if (file instanceof DBSourceCodeVirtualFile) return false;
        if (isDebugConsole(file)) return false;
        if (!hasConnectivityContext(file)) return false;
        if (!isMainEditor(editor)) return false;

        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        PsiFile psiFile = psiElement.getContainingFile();
        if (!allValid(project, editor, psiFile)) return;
        if (!isDbLanguagePsiFile(psiFile)) return;

        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        documentManager.saveDocument(editor.getDocument());
        ScriptExecutionManager scriptExecutionManager = ScriptExecutionManager.getInstance(project);
        scriptExecutionManager.executeScript(psiFile.getVirtualFile());
    }
}
