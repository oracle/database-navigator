package com.dbn.code.common.intention;

import com.dbn.common.icon.Icons;
import com.dbn.common.util.Context;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.DatabaseFeature;
import com.dbn.execution.explain.ExplainPlanManager;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.util.Files.isDbLanguageFile;
import static com.dbn.debugger.DatabaseDebuggerManager.isDebugConsole;

public class ExplainPlanIntentionAction extends EditorIntentionAction {
    @Override
    public EditorIntentionType getType() {
        return EditorIntentionType.EXPLAIN_STATEMENT;
    }

    @Override
    @NotNull
    public String getText() {
        return "Explain plan for statement";
    }


    @Override
    public Icon getIcon(int flags) {
        return Icons.STMT_EXECUTION_EXPLAIN;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if (isDatabaseAssistantPrompt(editor, psiElement)) return false;

        PsiFile psiFile = psiElement.getContainingFile();
        if (isNotValid(psiFile)) return false;

        VirtualFile file = psiFile.getVirtualFile();
        if (isNotValid(file)) return false;
        if (!isDbLanguageFile(file)) return false;
        if (isDebugConsole(file)) return false;

        ExecutablePsiElement executable = PsiUtil.lookupExecutableAtCaret(editor, true);
        if (isNotValid(executable)) return false;

        FileEditor fileEditor = Editors.getFileEditor(editor);
        if (isNotValid(fileEditor)) return false;

        if (executable.is(ElementTypeAttribute.DATA_MANIPULATION)) {
            ConnectionHandler activeConnection = executable.getConnection();
            return DatabaseFeature.EXPLAIN_PLAN.isSupported(activeConnection);
        }

        return false;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        ExecutablePsiElement executable = PsiUtil.lookupExecutableAtCaret(editor, true);
        FileEditor fileEditor = Editors.getFileEditor(editor);
        if (executable != null && fileEditor != null) {
            DataContext dataContext = Context.getDataContext(editor);
            ExplainPlanManager explainPlanManager = ExplainPlanManager.getInstance(project);
            explainPlanManager.executeExplainPlan(executable, dataContext, null);
        }
    }
}
