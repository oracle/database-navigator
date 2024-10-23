package com.dbn.debugger.common.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.database.DatabaseFeature;
import com.dbn.debugger.DatabaseDebuggerManager;
import com.dbn.execution.statement.StatementExecutionManager;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.vfs.DBConsoleType;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

@BackgroundUpdate
public class DebugStatementEditorAction extends ProjectAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        Editor editor = Lookups.getEditor(e);
        if (editor != null) {
            VirtualFile virtualFile = Documents.getVirtualFile(editor);
            ExecutablePsiElement executablePsiElement = null;
            if (virtualFile instanceof DBConsoleVirtualFile) {
                DBConsoleVirtualFile consoleVirtualFile = (DBConsoleVirtualFile) virtualFile;
                if (consoleVirtualFile.getType() == DBConsoleType.DEBUG) {
                    PsiFile file = Documents.getFile(editor);
                    if (file != null) {
                        BasePsiElement basePsiElement = PsiUtil.lookupElementAtOffset(file, ElementTypeAttribute.EXECUTABLE, 100);
                        if (basePsiElement instanceof ExecutablePsiElement) {
                            executablePsiElement = (ExecutablePsiElement) basePsiElement;
                        }
                    }
                }
            }

            if (executablePsiElement == null) {
                executablePsiElement = PsiUtil.lookupExecutableAtCaret(editor, true);
            }

            if (executablePsiElement != null && executablePsiElement.is(ElementTypeAttribute.DEBUGGABLE)) {
                FileEditor fileEditor = Editors.getFileEditor(editor);
                if (fileEditor != null) {
                    StatementExecutionManager statementExecutionManager = StatementExecutionManager.getInstance(project);
                    StatementExecutionProcessor executionProcessor = statementExecutionManager.getExecutionProcessor(fileEditor, executablePsiElement, true);
                    if (executionProcessor != null) {
                        DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
                        debuggerManager.startStatementDebugger(executionProcessor);
                    }
                }
            }
        }
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setIcon(Icons.STMT_EXECUTION_DEBUG);
        presentation.setText("Debug Statement");
        Editor editor = Lookups.getEditor(e);
        boolean enabled = false;
        boolean visible = false;
        if (editor != null) {
            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
            VirtualFile virtualFile = Documents.getVirtualFile(editor);
            if (virtualFile != null) {
                enabled = DatabaseDebuggerManager.isDebugConsole(virtualFile);

                ConnectionHandler connection = contextManager.getConnection(virtualFile);
                if (DatabaseFeature.DEBUGGING.isSupported(connection)){
                    visible = true;
                    if (!enabled) {
                        PsiFile psiFile = PsiUtil.getPsiFile(project, editor.getDocument());
                        if (psiFile != null) {
                            ExecutablePsiElement executablePsiElement = PsiUtil.lookupExecutableAtCaret(editor, true);
                            if (executablePsiElement != null && executablePsiElement.is(ElementTypeAttribute.DEBUGGABLE)) {
                                enabled = true;
                            }
                        }
                    }
                }
            }
        }
        presentation.setEnabled(enabled);
        presentation.setVisible(visible);

    }
}
