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

package com.dbn.execution.common.message.action;

import com.dbn.common.icon.Icons;
import com.dbn.execution.common.message.ui.tree.MessagesTree;
import com.dbn.execution.common.message.ui.tree.node.StatementExecutionMessageNode;
import com.dbn.execution.common.ui.ExecutionStatementViewerPopup;
import com.dbn.execution.statement.result.StatementExecutionResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.nls.NlsResources.txt;

public class ExecutedStatementViewAction extends AbstractExecutionMessagesAction {

    @Override
    protected void actionPerformed(
            @NotNull AnActionEvent e,
            @NotNull Project project,
            @NotNull MessagesTree messagesTree) {

        messagesTree.grabFocus();
        StatementExecutionMessageNode execMessageNode =
                (StatementExecutionMessageNode) messagesTree.getSelectionPath().getLastPathComponent();

        StatementExecutionResult executionResult = execMessageNode.getMessage().getExecutionResult();
        ExecutionStatementViewerPopup statementViewer = new ExecutionStatementViewerPopup(executionResult);
        statementViewer.show((Component) e.getInputEvent().getSource());
    }

    @Override
    protected void update(
            @NotNull AnActionEvent e,
            @NotNull Presentation presentation,
            @NotNull Project project,
            @Nullable MessagesTree target) {

        boolean enabled =
                isValid(target) &&
                target.getSelectionPath() != null &&
                target.getSelectionPath().getLastPathComponent() instanceof StatementExecutionMessageNode;

        presentation.setEnabled(enabled);
        presentation.setText(txt("app.execution.action.ViewSqlStatement"));
        presentation.setIcon(Icons.EXEC_RESULT_VIEW_STATEMENT);
    }
}