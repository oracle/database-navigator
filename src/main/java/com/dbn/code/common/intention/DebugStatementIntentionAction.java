package com.dbn.code.common.intention;

import com.dbn.common.icon.Icons;
import com.dbn.common.util.Context;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.debugger.DatabaseDebuggerManager;
import com.dbn.execution.statement.StatementExecutionManager;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.language.common.DBLanguagePsiFile;
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
import static com.dbn.common.util.Files.isDbLanguagePsiFile;

public class DebugStatementIntentionAction extends EditorIntentionAction {
    @Override
    public EditorIntentionType getType() {
        return EditorIntentionType.DEBUG_STATEMENT;
    }

    @Override
    @NotNull
    public String getText() {
        return "Debug statement";
    }


    @Override
    public Icon getIcon(int flags) {
        return Icons.STMT_EXECUTION_DEBUG;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        // do not show the intention for a db-assistant context (comment, selection or select-ai-statement)
        if (isDatabaseAssistantPrompt(editor, psiElement)) return false;

        PsiFile psiFile = psiElement.getContainingFile();
        if (isNotValid(psiFile)) return false;
        if (!isDbLanguagePsiFile(psiFile)) return false;

        VirtualFile file = psiFile.getVirtualFile();
        if (isNotValid(file)) return false;
        if (!isDbLanguageFile(file)) return false;

        ExecutablePsiElement executable = PsiUtil.lookupExecutableAtCaret(editor, true);
        if (isNotValid(executable)) return false;

        FileEditor fileEditor = Editors.getFileEditor(editor);
        if (isNotValid(fileEditor)) return false;

        return executable.is(ElementTypeAttribute.DEBUGGABLE);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        PsiFile psiFile = psiElement.getContainingFile();
        if (isNotValid(project)) return;
        if (isNotValid(editor)) return;
        if (isNotValid(psiFile)) return;
        if (!isDbLanguagePsiFile(psiFile)) return;

        ExecutablePsiElement executable = PsiUtil.lookupExecutableAtCaret(editor, true);
        if (isNotValid(executable)) return;

        FileEditor fileEditor = Editors.getFileEditor(editor);
        if (isNotValid(fileEditor)) return;

        DBLanguagePsiFile databasePsiFile = (DBLanguagePsiFile) psiFile;
        DataContext dataContext = Context.getDataContext(editor);

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        contextManager.selectConnectionAndSchema(
                databasePsiFile.getVirtualFile(),
                dataContext,
                () -> ConnectionAction.invoke(null, false, databasePsiFile,
                        action -> {
                            StatementExecutionManager executionManager = StatementExecutionManager.getInstance(project);
                            StatementExecutionProcessor executionProcessor = executionManager.getExecutionProcessor(fileEditor, executable, true);
                            if (executionProcessor != null) {
                                DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
                                debuggerManager.startStatementDebugger(executionProcessor);
                            }
                        }));
    }
}
