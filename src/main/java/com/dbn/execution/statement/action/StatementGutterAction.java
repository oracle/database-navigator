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

package com.dbn.execution.statement.action;

import com.dbn.common.action.BasicAction;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.execution.ExecutionStatus;
import com.dbn.execution.statement.StatementExecutionContext;
import com.dbn.execution.statement.StatementExecutionManager;
import com.dbn.execution.statement.processor.StatementExecutionCursorProcessor;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.execution.statement.result.StatementExecutionResult;
import com.dbn.execution.statement.result.StatementExecutionStatus;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.PsiFileRef;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.nls.NlsResources.txt;

public class StatementGutterAction extends BasicAction {
    private final PsiFileRef<DBLanguagePsiFile> psiFile;
    private ExecutablePsiElement psiElement;

    public StatementGutterAction(ExecutablePsiElement psiElement) {
        this.psiFile = PsiFileRef.of(psiElement.getFile());
        this.psiElement = psiElement;
    }

    @Nullable
    private DBLanguagePsiFile getPsiFile() {
        return psiFile.get();
    }

    @Nullable
    public ExecutablePsiElement getPsiElement() {
        if (isValid(psiElement)) return psiElement;

        // try to restore orphaned gutter actions
        psiElement = resolvePsiElement();
        return psiElement;
    }

    @Nullable
    private synchronized ExecutablePsiElement resolvePsiElement() {
        DBLanguagePsiFile psiFile = getPsiFile();
        if (psiFile == null) return null;

        ExecutablePsiElement psiElement = this.psiElement;
        if (psiElement == null) return null;

        TextRange textRange = psiElement.getTextRange();
        PsiElement psiElementAtOffset = psiFile.findElementAt(textRange.getStartOffset());

        BasePsiElement<?> basePsiElement = BasePsiElement.from(psiElementAtOffset);
        if (basePsiElement == null) return null;

        if (basePsiElement instanceof ExecutablePsiElement) {
            psiElement = (ExecutablePsiElement) basePsiElement;
        } else {
            psiElement = basePsiElement.findEnclosingElement(ExecutablePsiElement.class);
        }

        if (isNotValid(psiElement)) return null;
        if (!psiElement.matchesTextRange(textRange)) return null;

        return psiElement;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor(false);
        DataContext dataContext = e.getDataContext();

        if (executionProcessor != null && !executionProcessor.isDirty()) {
            Project project = executionProcessor.getProject();
            StatementExecutionManager executionManager = StatementExecutionManager.getInstance(project);
            StatementExecutionContext context = executionProcessor.getExecutionContext();
            if (context.is(ExecutionStatus.EXECUTING) || context.is(ExecutionStatus.QUEUED)) {
                executionProcessor.cancelExecution();
            } else {
                StatementExecutionResult executionResult = executionProcessor.getExecutionResult();
                if (executionResult == null || !(executionProcessor instanceof StatementExecutionCursorProcessor) || executionProcessor.isDirty()) {
                    executionManager.executeStatement(executionProcessor, dataContext);
                } else {
                    executionProcessor.navigateToResult();
                }
            }
        } else {
            executionProcessor = getExecutionProcessor(true);
            if (executionProcessor != null) {
                Project project = executionProcessor.getProject();
                StatementExecutionManager executionManager = StatementExecutionManager.getInstance(project);
                executionManager.executeStatement(executionProcessor, dataContext);
            }
        }
    }


    @NotNull
    public Icon getIcon() {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor(false);
        if (executionProcessor == null) return Icons.STMT_EXECUTION_RUN;

        StatementExecutionContext context = executionProcessor.getExecutionContext();
        if (context.is(ExecutionStatus.EXECUTING)) return Icons.STMT_EXECUTION_STOP;
        if (context.is(ExecutionStatus.QUEUED)) return Icons.STMT_EXECUTION_STOP_QUEUED;

        StatementExecutionResult executionResult = executionProcessor.getExecutionResult();
        if (executionResult == null) return Icons.STMT_EXECUTION_RUN;

        StatementExecutionStatus executionStatus = executionResult.getExecutionStatus();
        if (executionStatus == StatementExecutionStatus.SUCCESS){
            if (executionProcessor instanceof StatementExecutionCursorProcessor) {
                return executionProcessor.isDirty() ?
                        Icons.STMT_EXEC_RESULTSET_RERUN :
                        Icons.STMT_EXEC_RESULTSET;
            } else {
                return Icons.STMT_EXECUTION_INFO_RERUN;
            }
        }

        if (executionStatus == StatementExecutionStatus.ERROR) return Icons.STMT_EXECUTION_ERROR_RERUN;
        if (executionStatus == StatementExecutionStatus.WARNING) return Icons.STMT_EXECUTION_WARNING_RERUN;

        return Icons.STMT_EXECUTION_RUN;
    }

    @Nullable
    private StatementExecutionProcessor getExecutionProcessor(boolean create) {
        DBLanguagePsiFile psiFile = getPsiFile();
        if (psiFile == null) return null;

        ExecutablePsiElement psiElement = getPsiElement();
        if (psiElement == null) return null;

        Project project = psiFile.getProject();
        Document document = Documents.getDocument(psiFile);
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        FileEditor[] selectedEditors = fileEditorManager.getSelectedEditors();
        for (FileEditor fileEditor : selectedEditors) {
            Editor editor = Editors.getEditor(fileEditor);
            if (editor == null) continue;
            if (editor.getDocument() != document) continue;

            StatementExecutionManager executionManager = StatementExecutionManager.getInstance(project);
            return executionManager.getExecutionProcessor(fileEditor, psiElement, create);
        }
        return null;
    }


    @Nullable
    public String getTooltipText() {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor(false);
        if (isNotValid(executionProcessor)) return null;

        StatementExecutionResult executionResult = executionProcessor.getExecutionResult();
        if (executionResult == null) {
            StatementExecutionContext context = executionProcessor.getExecutionContext();
            if (context.is(ExecutionStatus.EXECUTING)) return txt("app.execution.tooltip.StatementExecutionInProgress");
            if (context.is(ExecutionStatus.QUEUED)) return txt("app.execution.tooltip.StatementExecutionQueued");
            
        } else {
            StatementExecutionStatus executionStatus = executionResult.getExecutionStatus();
            if (executionStatus == StatementExecutionStatus.ERROR)   return txt("app.execution.tooltip.StatementExecutedWithErrors");
            if (executionStatus == StatementExecutionStatus.WARNING) return txt("app.execution.tooltip.StatementExecutedWithWarnings");
            if (executionStatus == StatementExecutionStatus.SUCCESS) return txt("app.execution.tooltip.StatementExecutedSuccessfully");
        }
        return null;
    }
}
