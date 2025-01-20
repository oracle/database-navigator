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

import com.dbn.common.icon.Icons;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.util.Editors;
import com.dbn.execution.statement.StatementExecutionManager;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static com.dbn.nls.NlsResources.txt;

public class JumpToExecutionResultIntentionAction extends EditorIntentionAction  {

    private WeakRef<StatementExecutionProcessor> cachedExecutionProcessor;

    @Override
    public EditorIntentionType getType() {
        return EditorIntentionType.EXECUTION_RESULT;
    }

    @Override
    @NotNull
    public String getText() {
        return txt("app.codeEditor.action.NavigateToResult");
    }


    @Override
    public Icon getIcon(int flags) {
/*        if (cachedExecutionProcessor != null) {
            StatementExecutionProcessor executionProcessor = cachedExecutionProcessor.get();
            if (executionProcessor != null) {
                StatementExecutionResult executionResult = executionProcessor.getExecutionResult();
                if (executionResult != null) {
                    StatementExecutionStatus executionStatus = executionResult.getExecutionStatus();
                    if (executionStatus == StatementExecutionStatus.SUCCESS){
                        if (executionProcessor instanceof StatementExecutionCursorProcessor) {
                            return Icons.STMT_EXEC_RESULTSET;
                        } else {
                            return Icons.COMMON_INFO;
                        }
                    } else if (executionStatus == StatementExecutionStatus.ERROR){
                        return Icons.COMMON_ERROR;
                    } else if (executionStatus == StatementExecutionStatus.WARNING){
                        return Icons.COMMON_WARNING;
                    }
                }
            }
        }*/
        return Icons.STMT_EXECUTION_NAVIGATE;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        PsiFile psiFile = psiElement.getContainingFile();
        if (psiFile instanceof DBLanguagePsiFile) {
            ExecutablePsiElement executable = PsiUtil.lookupExecutableAtCaret(editor, true);
            FileEditor fileEditor = Editors.getFileEditor(editor);
            if (executable != null && fileEditor != null) {
                StatementExecutionManager executionManager = StatementExecutionManager.getInstance(project);
                StatementExecutionProcessor executionProcessor = executionManager.getExecutionProcessor(fileEditor, executable, false);
                if (executionProcessor != null && executionProcessor.getExecutionResult() != null) {
                    cachedExecutionProcessor = WeakRef.of(executionProcessor);
                    return true;
                }
            }
        }
        cachedExecutionProcessor = null;
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        ExecutablePsiElement executable = PsiUtil.lookupExecutableAtCaret(editor, true);
        FileEditor fileEditor = Editors.getFileEditor(editor);
        if (executable != null && fileEditor != null) {
            StatementExecutionManager executionManager = StatementExecutionManager.getInstance(project);
            StatementExecutionProcessor executionProcessor = executionManager.getExecutionProcessor(fileEditor, executable, false);
            if (executionProcessor != null) {
                executionProcessor.navigateToResult();
            }
        }
    }
}