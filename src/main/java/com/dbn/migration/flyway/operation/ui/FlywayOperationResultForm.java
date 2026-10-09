/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.migration.flyway.operation.ui;

import com.dbn.common.action.DataKeys;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.task.TaskStatus;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.util.Accessibility;
import com.dbn.common.util.Actions;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.execution.common.result.ui.ExecutionResultLogConsole;
import com.dbn.execution.logging.LogOutput;
import com.dbn.execution.logging.LogOutputContext;
import com.dbn.migration.flyway.operation.FlywayOperationResult;
import com.intellij.openapi.actionSystem.ActionToolbar;
import org.flywaydb.core.api.output.ValidateOutput;
import org.flywaydb.core.api.output.ValidateResult;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import java.util.List;

import static com.dbn.migration.flyway.operation.FlywayFeature.MIGRATION_ITEMS;
import static com.dbn.nls.NlsResources.txt;

/**
 * Console form for Flyway operation output.
 */
public class FlywayOperationResultForm extends ExecutionResultFormBase<FlywayOperationResult> {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel summaryPanel;
    private JTabbedPane contentTabbedPane;

    private ExecutionResultLogConsole console;
    private JTextArea validationItemsTextArea;
    private int outputOffset;
    private TaskStatus actionStatus;

    public FlywayOperationResultForm(@NotNull FlywayOperationResult result) {
        super(result);
        initActionsPanel();
        initSummaryPanel();
        initConsolePanel();
        initValidationItemsPanel();
        initResultListener();
        updateResult(result);
    }

    private void initActionsPanel() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(
                actionsPanel,
                false,
                "DBN.Execution.Flyway.Result");
        Accessibility.setAccessibleName(
                actionToolbar,
                txt("app.flyway.aria.ExecutionResultActions"));
        actionsPanel.add(actionToolbar.getComponent());
    }

    private void initSummaryPanel() {
        FlywayOperationResult result = getExecutionResult();
        FlywayOperationResultSummaryForm summaryForm = new FlywayOperationResultSummaryForm(this, result);
        summaryPanel.add(summaryForm.getComponent());
    }

    private void initConsolePanel() {
        FlywayOperationResult result = getExecutionResult();
        console = new ExecutionResultLogConsole(result.getConnection(), "Console", false);
        console.installOn(contentTabbedPane);
        Disposer.register(this, console);
    }

    private void initValidationItemsPanel() {
        FlywayOperationResult result = getExecutionResult();
        if (!result.getOperation().getSupport().supports(MIGRATION_ITEMS)) return;

        validationItemsTextArea = new JTextArea();
        validationItemsTextArea.setEditable(false);
        validationItemsTextArea.setLineWrap(true);
        validationItemsTextArea.setWrapStyleWord(true);
        contentTabbedPane.addTab(
                txt("app.flyway.title.ValidationItems"),
                new DBNScrollPane(validationItemsTextArea));
    }

    private void initResultListener() {
        FlywayOperationResult result = getExecutionResult();
        result.addListener(() -> Dispatch.run(false, () -> updateResult(result)));
    }

    private void updateResult(@NotNull FlywayOperationResult result) {
        boolean outputChanged = updateConsoleOutput(result);
        updateValidationItems(result);
        if (outputChanged) console.markOutputUnread();
        updateActionToolbarState(result);
    }

    private void updateActionToolbarState(@NotNull FlywayOperationResult result) {
        TaskStatus status = result.getStatus();
        if (status == actionStatus) return;

        actionStatus = status;
        updateActionToolbars();
    }

    private void updateValidationItems(@NotNull FlywayOperationResult result) {
        if (validationItemsTextArea == null) return;

        ValidateResult validationResult = result.getValidationResult();
        if (validationResult == null) {
            validationItemsTextArea.setText("");
            return;
        }

        StringBuilder text = new StringBuilder();
        List<ValidateOutput> invalidMigrations = validationResult.invalidMigrations;
        for (ValidateOutput migration : invalidMigrations) {
            if (text.length() > 0) text.append('\n');
            text.append(migration.version == null ? "" : migration.version)
                    .append(" - ")
                    .append(migration.description == null ? "" : migration.description);
            if (migration.filepath != null) {
                text.append("\n").append(migration.filepath);
            }
            if (migration.errorDetails != null && migration.errorDetails.errorMessage != null) {
                text.append("\n").append(migration.errorDetails.errorMessage);
            }
        }
        validationItemsTextArea.setText(text.toString());
        validationItemsTextArea.setCaretPosition(0);
    }

    private boolean updateConsoleOutput(@NotNull FlywayOperationResult result) {
        int initialOffset = outputOffset;
        List<LogOutput> output = result.getOutput();
        while (outputOffset < output.size()) {
            writeOutput(output.get(outputOffset));
            outputOffset++;
        }
        return outputOffset > initialOffset;
    }

    private void writeOutput(@NotNull LogOutput output) {
        LogOutputContext context = new LogOutputContext(getExecutionResult().getConnection());
        context.setHideEmptyLines(false);
        console.writeToConsole(context, output);
    }

    @Override
    public void rebuildForm() {
        Disposer.dispose(console);
        summaryPanel.removeAll();
        contentTabbedPane.removeAll();
        validationItemsTextArea = null;
        outputOffset = 0;

        initSummaryPanel();
        initConsolePanel();
        initValidationItemsPanel();
        initResultListener();
        updateResult(getExecutionResult());
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.FLYWAY_EXECUTION_RESULT.is(dataId)) return getExecutionResult();
        return null;
    }
}
