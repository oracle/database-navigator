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
import com.dbn.migration.flyway.operation.FlywayOperationSupport;
import com.intellij.openapi.actionSystem.ActionToolbar;
import org.flywaydb.core.api.output.InfoOutput;
import org.flywaydb.core.api.output.InfoResult;
import org.flywaydb.core.api.output.ValidateOutput;
import org.flywaydb.core.api.output.ValidateResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import java.util.List;
import java.util.StringJoiner;

import static com.dbn.migration.flyway.operation.FlywayFeature.MIGRATION_INFO;
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
    private JTextArea migrationItemsTextArea;
    private int outputOffset;
    private TaskStatus actionStatus;

    public FlywayOperationResultForm(@NotNull FlywayOperationResult result) {
        super(result);
        initActionsPanel();
        initSummaryPanel();
        initConsolePanel();
        initMigrationItemsPanel();
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

    private void initMigrationItemsPanel() {
        FlywayOperationResult result = getExecutionResult();
        FlywayOperationSupport support = result.getOperation().getSupport();
        if (!support.supports(MIGRATION_ITEMS)) return;

        migrationItemsTextArea = new JTextArea();
        migrationItemsTextArea.setEditable(false);
        migrationItemsTextArea.setLineWrap(true);
        migrationItemsTextArea.setWrapStyleWord(true);
        contentTabbedPane.addTab(
                support.supports(MIGRATION_INFO)
                        ? txt("app.flyway.title.MigrationInfo")
                        : txt("app.flyway.title.ValidationItems"),
                new DBNScrollPane(migrationItemsTextArea));
    }

    private void initResultListener() {
        FlywayOperationResult result = getExecutionResult();
        result.addListener(() -> Dispatch.run(false, () -> updateResult(result)));
    }

    private void updateResult(@NotNull FlywayOperationResult result) {
        boolean outputChanged = updateConsoleOutput(result);
        updateMigrationItems(result);
        if (outputChanged) console.markOutputUnread();
        updateActionToolbarState(result);
    }

    private void updateActionToolbarState(@NotNull FlywayOperationResult result) {
        TaskStatus status = result.getStatus();
        if (status == actionStatus) return;

        actionStatus = status;
        updateActionToolbars();
    }

    private void updateMigrationItems(@NotNull FlywayOperationResult result) {
        if (migrationItemsTextArea == null) return;

        InfoResult infoResult = result.getInfoResult();
        if (infoResult != null) {
            updateInfoItems(infoResult);
            return;
        }

        ValidateResult validationResult = result.getValidationResult();
        if (validationResult != null) {
            updateValidationItems(validationResult);
        } else {
            migrationItemsTextArea.setText("");
        }
    }

    private void updateInfoItems(@NotNull InfoResult infoResult) {
        if (infoResult.migrations == null) {
            migrationItemsTextArea.setText("");
            return;
        }

        StringBuilder text = new StringBuilder();
        for (InfoOutput migration : infoResult.migrations) {
            if (text.length() > 0) text.append('\n');
            StringJoiner values = new StringJoiner(" | ");
            appendValue(values, migration.category);
            appendValue(values, migration.version);
            appendValue(values, migration.description);
            appendValue(values, migration.type);
            appendValue(values, migration.state);
            appendValue(values, migration.filepath);
            appendValue(values, migration.installedOnUTC);
            appendValue(values, migration.installedBy);
            appendValue(values, Integer.toString(migration.executionTime));
            text.append(values);
        }
        migrationItemsTextArea.setText(text.toString());
        migrationItemsTextArea.setCaretPosition(0);
    }

    private void updateValidationItems(@Nullable ValidateResult validationResult) {
        if (validationResult == null) {
            migrationItemsTextArea.setText("");
            return;
        }

        StringBuilder text = new StringBuilder();
        List<ValidateOutput> invalidMigrations = validationResult.invalidMigrations;
        for (ValidateOutput migration : invalidMigrations) {
            if (!text.isEmpty()) text.append('\n');
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
        migrationItemsTextArea.setText(text.toString());
        migrationItemsTextArea.setCaretPosition(0);
    }

    private static void appendValue(@NotNull StringJoiner values, @Nullable String value) {
        values.add(value == null ? "" : value);
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
        migrationItemsTextArea = null;
        outputOffset = 0;

        initSummaryPanel();
        initConsolePanel();
        initMigrationItemsPanel();
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
