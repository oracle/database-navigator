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

package com.dbn.execution.logging.action;

import com.dbn.common.icon.Icons;
import com.dbn.common.util.Messages;
import com.dbn.execution.ExecutionManager;
import com.dbn.execution.logging.DatabaseLoggingResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Conditional.when;
import static com.dbn.nls.NlsResources.txt;

public class DatabaseLogOutputCloseAction extends AbstractDatabaseLoggingAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DatabaseLoggingResult loggingResult) {
        if (loggingResult.getContext().isActive()) {
            Messages.showQuestionDialog(
                    project,
                    txt("msg.execution.title.ProcessActive"),
                    txt("msg.execution.question.ProcessActive"),
                    Messages.OPTIONS_YES_NO, 0,
                    option -> when(option == 0, () -> closeConsole(loggingResult, project)));
        } else {
            closeConsole(loggingResult, project);
        }
    }

    private void closeConsole(DatabaseLoggingResult loggingResult, Project project) {
        loggingResult.getContext().close();
        ExecutionManager executionManager = ExecutionManager.getInstance(project);
        executionManager.removeResultTab(loggingResult);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DatabaseLoggingResult target) {
        presentation.setText(txt("app.shared.action.Close"));
        presentation.setIcon(Icons.EXEC_RESULT_CLOSE);
    }
}
