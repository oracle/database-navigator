package com.dbn.execution.explain.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.DatabaseFeature;
import com.dbn.debugger.DatabaseDebuggerManager;
import com.dbn.execution.explain.ExplainPlanManager;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Checks.isValid;

@BackgroundUpdate
public class ExplainPlanEditorAction extends ProjectAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        Editor editor = Lookups.getEditor(e);
        if (isValid(editor)) {
            FileEditor fileEditor = Editors.getFileEditor(editor);
            ExecutablePsiElement executable = PsiUtil.lookupExecutableAtCaret(editor, true);
            if (fileEditor != null && executable != null && executable.is(ElementTypeAttribute.DATA_MANIPULATION)) {
                ExplainPlanManager explainPlanManager = ExplainPlanManager.getInstance(project);
                explainPlanManager.executeExplainPlan(executable, e.getDataContext(), null);
            }
        }
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setIcon(Icons.STMT_EXECUTION_EXPLAIN);
        presentation.setText("Explain Plan for Statement");

        boolean visible = false;
        boolean enabled = false;

        Editor editor = Lookups.getEditor(e);
        if (editor != null) {
            PsiFile psiFile = PsiUtil.getPsiFile(project, editor.getDocument());
            if (psiFile instanceof DBLanguagePsiFile) {
                DBLanguagePsiFile languagePsiFile = (DBLanguagePsiFile) psiFile;

                ConnectionHandler connection = languagePsiFile.getConnection();
                visible = isVisible(e) && DatabaseFeature.EXPLAIN_PLAN.isSupported(connection);

                if (visible) {
                    ExecutablePsiElement executable = PsiUtil.lookupExecutableAtCaret(editor, true);
                    if (executable != null && executable.is(ElementTypeAttribute.DATA_MANIPULATION)) {
                        enabled = true;
                    }
                }
            }
        }
        presentation.setEnabled(enabled);
        presentation.setVisible(visible);
    }

    public static boolean isVisible(AnActionEvent e) {
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        return !DatabaseDebuggerManager.isDebugConsole(virtualFile);
    }
}
