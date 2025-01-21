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

package com.dbn.execution.statement.result.action;

import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Progress;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.statement.StatementExecutionManager;
import com.dbn.execution.statement.processor.StatementExecutionCursorProcessor;
import com.dbn.execution.statement.result.StatementExecutionCursorResult;
import com.dbn.execution.statement.variables.StatementExecutionVariablesBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.notification.NotificationGroup.EXECUTION;
import static com.dbn.common.notification.NotificationSupport.sendErrorNotification;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class ExecutionResultVariablesDialogAction extends AbstractExecutionResultAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull StatementExecutionCursorResult executionResult) {
        StatementExecutionCursorProcessor executionProcessor = executionResult.getExecutionProcessor();
        StatementExecutionManager statementExecutionManager = StatementExecutionManager.getInstance(project);
        String statementName = executionResult.getExecutionProcessor().getStatementName();
        statementExecutionManager.promptExecutionDialog(
                executionProcessor,
                DBDebuggerType.NONE,
                () -> Progress.prompt(project, executionProcessor, true,
                        txt("prc.execution.title.ExecutingStatement"),
                        txt("prc.execution.text.ExecutingStatement", statementName),
                        progress -> {
                            try {
                                executionProcessor.execute();
                            } catch (SQLException ex) {
                                conditionallyLog(ex);
                                sendErrorNotification(project, EXECUTION, txt("ntf.execution.error.StatementExecutionError", ex));
                            }
                        }));
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable StatementExecutionCursorResult executionResult) {
        boolean visible = false;
        if (isValid(executionResult)) {
            StatementExecutionCursorProcessor executionProcessor = executionResult.getExecutionProcessor();
            if (isValid(executionProcessor)) {
                StatementExecutionVariablesBundle executionVariables = executionProcessor.getExecutionVariables();
                visible = executionVariables != null && executionVariables.getVariables().size() > 0;
            }
        }
        presentation.setVisible(visible);
        presentation.setText(txt("app.execution.action.OpenVariablesDialog"));
        presentation.setIcon(Icons.EXEC_RESULT_OPEN_EXEC_DIALOG);
    }
}
