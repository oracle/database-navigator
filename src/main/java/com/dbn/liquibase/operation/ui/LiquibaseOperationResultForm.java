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

package com.dbn.liquibase.operation.ui;

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
import com.dbn.liquibase.execution.ui.LiquibaseChangeSetItemsTable;
import com.dbn.liquibase.execution.ui.LiquibaseChangeSetItemsTableModel;
import com.dbn.liquibase.execution.ui.LiquibaseComparisonItemsTable;
import com.dbn.liquibase.execution.ui.LiquibaseComparisonItemsTableModel;
import com.dbn.liquibase.execution.ui.LiquibaseExecutionSqlPanel;
import com.dbn.liquibase.execution.ui.LiquibaseLockItemsTable;
import com.dbn.liquibase.execution.ui.LiquibaseLockItemsTableModel;
import com.dbn.liquibase.execution.ui.LiquibaseSnapshotItemsTable;
import com.dbn.liquibase.execution.ui.LiquibaseSnapshotItemsTableModel;
import com.dbn.liquibase.operation.LiquibaseOperationResult;
import com.dbn.liquibase.operation.LiquibaseOperationSupport;
import com.intellij.openapi.actionSystem.ActionToolbar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.util.List;

import static com.dbn.liquibase.operation.LiquibaseFeature.CHANGESET_ITEMS;
import static com.dbn.liquibase.operation.LiquibaseFeature.COMPARISON_ITEMS;
import static com.dbn.liquibase.operation.LiquibaseFeature.LOCK_ITEMS;
import static com.dbn.liquibase.operation.LiquibaseFeature.SNAPSHOT_ITEMS;
import static com.dbn.liquibase.operation.LiquibaseFeature.SQL_OUTPUT;
import static com.dbn.nls.NlsResources.txt;

/** Console form for Liquibase operation output. */
public class LiquibaseOperationResultForm extends ExecutionResultFormBase<LiquibaseOperationResult> {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel summaryPanel;
    private JTabbedPane contentTabbedPane;

    private ExecutionResultLogConsole console;
    private LiquibaseExecutionSqlPanel sqlPanel;
    private LiquibaseSnapshotItemsTableModel snapshotItemsTableModel;
    private LiquibaseChangeSetItemsTableModel changeSetItemsTableModel;
    private LiquibaseComparisonItemsTableModel comparisonItemsTableModel;
    private LiquibaseLockItemsTableModel lockItemsTableModel;
    private int outputOffset;
    private final boolean embedded;
    private TaskStatus actionStatus;

    public LiquibaseOperationResultForm(@NotNull LiquibaseOperationResult result, boolean embedded) {
        super(result);
        this.embedded = embedded;

        initActionsPanel();
        initSummaryPanel();
        initConsolePanel();
        initContentItemsPanel();
        initSqlOutputPanel();
        initResultListeners();
        updateResult(result, snapshotItemsTableModel, changeSetItemsTableModel, comparisonItemsTableModel, lockItemsTableModel);
    }

    private void initActionsPanel() {
        actionsPanel.setVisible(!embedded);
        if (embedded) return;
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, false, "DBN.Execution.Liquibase.Result");
        Accessibility.setAccessibleName(actionToolbar, txt("app.liquibase.aria.ExecutionResultActions"));
        actionsPanel.add(actionToolbar.getComponent());
    }

    private void initSummaryPanel() {
        LiquibaseOperationResult result = getExecutionResult();
        LiquibaseOperationResultSummaryForm summaryForm = new LiquibaseOperationResultSummaryForm(this, result);
        summaryPanel.add(summaryForm.getComponent());
    }

    private void initConsolePanel() {
        LiquibaseOperationResult result = getExecutionResult();
        console = new ExecutionResultLogConsole(result.getConnection(), "Console", false);
        console.installOn(contentTabbedPane);
        Disposer.register(this, console);
    }

    private void initSqlOutputPanel() {
        LiquibaseOperationResult result = getExecutionResult();
        if (!result.getOperation().supports(SQL_OUTPUT)) return;

        sqlPanel = new LiquibaseExecutionSqlPanel(this, result);
        Disposer.register(this, sqlPanel);
        contentTabbedPane.addTab(txt("app.liquibase.title.SqlOutput"), sqlPanel.getComponent());
    }

    private void initContentItemsPanel() {
        LiquibaseOperationResult result = getExecutionResult();
        LiquibaseOperationSupport support = result.getOperation().getSupport();
        if (support.supports(COMPARISON_ITEMS)) {
            initComparisonItemsPanel(result);
        } else if (support.supports(SNAPSHOT_ITEMS)) {
            initSnapshotItemsPanel(result);
        } else if (support.supports(LOCK_ITEMS)) {
            initLockItemsPanel(result);
        } else if (support.supports(CHANGESET_ITEMS)) {
            initChangeSetItemsPanel(result);
        }
    }

    private void initComparisonItemsPanel(@NotNull LiquibaseOperationResult result) {
        comparisonItemsTableModel = new LiquibaseComparisonItemsTableModel(result);
        LiquibaseComparisonItemsTable comparisonItemsTable =
                new LiquibaseComparisonItemsTable(this, comparisonItemsTableModel);
        contentTabbedPane.addTab(txt("app.liquibase.title.ComparisonItems"), new DBNScrollPane(comparisonItemsTable));
    }

    private void initSnapshotItemsPanel(@NotNull LiquibaseOperationResult result) {
        snapshotItemsTableModel = new LiquibaseSnapshotItemsTableModel(result);

        LiquibaseSnapshotItemsTable snapshotItemsTable = new LiquibaseSnapshotItemsTable(this, snapshotItemsTableModel);
        contentTabbedPane.addTab(txt("app.liquibase.title.SnapshotItems"), new DBNScrollPane(snapshotItemsTable));
    }

    private void initChangeSetItemsPanel(@NotNull LiquibaseOperationResult result) {
        changeSetItemsTableModel = new LiquibaseChangeSetItemsTableModel(result);

        LiquibaseChangeSetItemsTable changeSetItemsTable =
                new LiquibaseChangeSetItemsTable(this, changeSetItemsTableModel);
        contentTabbedPane.addTab(txt("app.liquibase.title.ChangeSetItems"), new DBNScrollPane(changeSetItemsTable));
    }

    private void initLockItemsPanel(@NotNull LiquibaseOperationResult result) {
        lockItemsTableModel = new LiquibaseLockItemsTableModel(result);
        LiquibaseLockItemsTable lockItemsTable = new LiquibaseLockItemsTable(this, lockItemsTableModel);
        contentTabbedPane.addTab(txt("app.liquibase.title.LockItems"), new DBNScrollPane(lockItemsTable));
    }

    private void initResultListeners() {
        LiquibaseOperationResult result = getExecutionResult();
        result.addListener(() -> Dispatch.run(false, () -> updateResult(
                result,
                snapshotItemsTableModel,
                changeSetItemsTableModel,
                comparisonItemsTableModel,
                lockItemsTableModel)));
    }

    private void updateResult(
            @NotNull LiquibaseOperationResult result,
            @Nullable LiquibaseSnapshotItemsTableModel snapshotTableModel,
            @Nullable LiquibaseChangeSetItemsTableModel changeSetTableModel,
            @Nullable LiquibaseComparisonItemsTableModel comparisonTableModel,
            @Nullable LiquibaseLockItemsTableModel lockTableModel) {
        boolean outputChanged = updateConsoleOutput(result);
        updateSqlOutput(result);
        if (snapshotTableModel != null) snapshotTableModel.refresh();
        if (changeSetTableModel != null) changeSetTableModel.refresh();
        if (comparisonTableModel != null) comparisonTableModel.refresh();
        if (lockTableModel != null) lockTableModel.refresh();
        if (outputChanged) console.markOutputUnread();
        updateActionToolbarState(result);
    }

    private void updateActionToolbarState(@NotNull LiquibaseOperationResult result) {
        if (embedded) return;

        TaskStatus status = result.getStatus();
        if (status == actionStatus) return;

        actionStatus = status;
        updateActionToolbars();
    }

    private void updateSqlOutput(@NotNull LiquibaseOperationResult result) {
        if (sqlPanel != null) sqlPanel.setText(result.getSqlOutput());
    }

    private boolean updateConsoleOutput(@NotNull LiquibaseOperationResult result) {
        int initialOffset = outputOffset;
        List<LogOutput> output = result.getOutput();
        while (outputOffset < output.size()) {
            writeOutput(output.get(outputOffset));
            outputOffset++;
        }
        return outputOffset > initialOffset;
    }

    @Override
    public void rebuildForm() {
        Disposer.dispose(console);
        Disposer.dispose(sqlPanel);
        sqlPanel = null;
        summaryPanel.removeAll();
        contentTabbedPane.removeAll();
        outputOffset = 0;

        initSummaryPanel();
        initConsolePanel();
        initSqlOutputPanel();
        initContentItemsPanel();
        initResultListeners();
        updateResult(getExecutionResult(), snapshotItemsTableModel, changeSetItemsTableModel, comparisonItemsTableModel, lockItemsTableModel);

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private void writeOutput(@NotNull LogOutput output) {
        LogOutputContext context = new LogOutputContext(getExecutionResult().getConnection());
        context.setHideEmptyLines(false);
        console.writeToConsole(context, output);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.LIQUIBASE_EXECUTION_RESULT.is(dataId)) return getExecutionResult();
        return null;
    }
}
