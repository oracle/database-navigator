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

package com.dbn.execution.statement.variables.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Messages;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.execution.statement.variables.StatementExecutionVariablesBundle;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

public class StatementExecutionInputsDialog extends DBNDialog<StatementExecutionInputForm> {
    private final StatementExecutionProcessor executionProcessor;
    private final ExecuteAction executeAction;
    private final DBDebuggerType debuggerType;
    private final boolean bulkExecution;
    private boolean reuseVariables = false;

    public StatementExecutionInputsDialog(StatementExecutionProcessor executionProcessor, DBDebuggerType debuggerType, boolean bulkExecution) {
        super(executionProcessor.getProject(), (debuggerType.isDebug() ? "Debug" : "Execute") + " statement", true);
        this.executionProcessor = executionProcessor;
        this.debuggerType = debuggerType;
        this.bulkExecution = bulkExecution;
        setModal(true);
        setResizable(true);
        executeAction = new ExecuteAction();
        init();
    }

    @NotNull
    @Override
    protected StatementExecutionInputForm createForm() {
        return new StatementExecutionInputForm(this, executionProcessor, debuggerType, bulkExecution);
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                executeAction,
                getCancelAction(),
                getHelpAction()
        };
    }

    private class ExecuteAction extends AbstractAction {
        ExecuteAction() {
            super(debuggerType.isDebug() ? "Debug" : "Execute", debuggerType.isDebug() ? Icons.STMT_EXECUTION_DEBUG : Icons.STMT_EXECUTION_RUN);
            makeDefaultAction(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            getForm().updateExecutionInput();
            StatementExecutionVariablesBundle executionVariables = executionProcessor.getExecutionVariables();
            Project project = getProject();
            if (executionVariables != null) {
                if (!executionVariables.isProvided()) {
                    Messages.showErrorDialog(
                            project,
                            "Statement execution",
                            "You didn't specify values for all the variables. \n" +
                                    "Please enter values for all the listed variables and try again."
                    );
                } else if (executionVariables.hasErrors()) {
                    Messages.showErrorDialog(
                            project,
                            "Statement execution",
                            "You provided invalid/unsupported variable values. \n" +
                                    "Please correct your input and try again."
                    );
                } else {
                    doOKAction();
                }
            } else {
                doOKAction();
            }
        }
    }

    public boolean isReuseVariables() {
        return reuseVariables;
    }

    public void setReuseVariables(boolean reuseVariables) {
        this.reuseVariables = reuseVariables;
    }
}
