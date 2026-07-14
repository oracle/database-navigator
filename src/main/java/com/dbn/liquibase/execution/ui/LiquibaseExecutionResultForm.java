package com.dbn.liquibase.execution.ui;

import com.dbn.common.action.DataKeys;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.util.Accessibility;
import com.dbn.common.util.Actions;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.execution.common.result.ui.ExecutionResultLogConsole;
import com.dbn.execution.logging.LogOutput;
import com.dbn.execution.logging.LogOutputContext;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.intellij.openapi.actionSystem.ActionToolbar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/** Console form for Liquibase operation output. */
public class LiquibaseExecutionResultForm extends ExecutionResultFormBase<LiquibaseExecutionResult> {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel summaryPanel;
    private JTabbedPane contentTabbedPane;

    private ExecutionResultLogConsole console;
    private LiquibaseSnapshotItemsTableModel snapshotItemsTableModel;
    private LiquibaseChangeSetItemsTableModel changeSetItemsTableModel;
    private LiquibaseComparisonItemsTableModel comparisonItemsTableModel;
    private int outputOffset;

    public LiquibaseExecutionResultForm(@NotNull LiquibaseExecutionResult result) {
        super(result);
        initActionsPanel();
        initSummaryPanel();
        initConsolePanel();
        initContentItemsPanel();
        initResultListeners();
        updateResult(result, snapshotItemsTableModel, changeSetItemsTableModel, comparisonItemsTableModel);
    }

    private void initActionsPanel() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, false, "DBN.Execution.Liquibase.Result");
        Accessibility.setAccessibleName(actionToolbar, txt("app.liquibase.aria.ExecutionResultActions"));
        actionsPanel.add(actionToolbar.getComponent());
    }

    private void initSummaryPanel() {
        LiquibaseExecutionResult result = getExecutionResult();
        LiquibaseExecutionSummaryForm summaryForm = new LiquibaseExecutionSummaryForm(this, result);
        summaryPanel.add(summaryForm.getComponent());
    }

    private void initConsolePanel() {
        LiquibaseExecutionResult result = getExecutionResult();
        console = new ExecutionResultLogConsole(result.getConnection(), "Console", false);
        console.installOn(contentTabbedPane);
        Disposer.register(this, console);
    }

    private void initContentItemsPanel() {
        LiquibaseExecutionResult result = getExecutionResult();
        if (result.getOperation().supportsComparisonItems()) {
            initComparisonItemsPanel(result);
        } else if (result.getOperation().supportsSnapshotItems()) {
            initSnapshotItemsPanel(result);
        } else if (result.getOperation().supportsChangeSetItems()) {
            initChangeSetItemsPanel(result);
        }
    }

    private void initComparisonItemsPanel(@NotNull LiquibaseExecutionResult result) {
        comparisonItemsTableModel = new LiquibaseComparisonItemsTableModel(result);
        LiquibaseComparisonItemsTable comparisonItemsTable =
                new LiquibaseComparisonItemsTable(this, comparisonItemsTableModel);
        contentTabbedPane.addTab(txt("app.liquibase.title.ComparisonItems"), new DBNScrollPane(comparisonItemsTable));
    }

    private void initSnapshotItemsPanel(@NotNull LiquibaseExecutionResult result) {
        snapshotItemsTableModel = new LiquibaseSnapshotItemsTableModel(result);

        LiquibaseSnapshotItemsTable snapshotItemsTable = new LiquibaseSnapshotItemsTable(this, snapshotItemsTableModel);
        contentTabbedPane.addTab(txt("app.liquibase.title.SnapshotItems"), new DBNScrollPane(snapshotItemsTable));
    }

    private void initChangeSetItemsPanel(@NotNull LiquibaseExecutionResult result) {
        changeSetItemsTableModel = new LiquibaseChangeSetItemsTableModel(result);

        LiquibaseChangeSetItemsTable changeSetItemsTable =
                new LiquibaseChangeSetItemsTable(this, changeSetItemsTableModel);
        contentTabbedPane.addTab(txt("app.liquibase.title.ChangeSetItems"), new DBNScrollPane(changeSetItemsTable));
    }

    private void initResultListeners() {
        LiquibaseExecutionResult result = getExecutionResult();
        result.addListener(() -> Dispatch.run(false, () -> updateResult(
                result,
                snapshotItemsTableModel,
                changeSetItemsTableModel,
                comparisonItemsTableModel)));
    }

    private void updateResult(
            @NotNull LiquibaseExecutionResult result,
            @Nullable LiquibaseSnapshotItemsTableModel snapshotTableModel,
            @Nullable LiquibaseChangeSetItemsTableModel changeSetTableModel,
            @Nullable LiquibaseComparisonItemsTableModel comparisonTableModel) {
        boolean outputChanged = updateConsoleOutput(result);
        if (snapshotTableModel != null) snapshotTableModel.refresh();
        if (changeSetTableModel != null) changeSetTableModel.refresh();
        if (comparisonTableModel != null) comparisonTableModel.refresh();
        if (outputChanged) console.markOutputUnread();
    }

    private boolean updateConsoleOutput(@NotNull LiquibaseExecutionResult result) {
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
        summaryPanel.removeAll();
        contentTabbedPane.removeAll();
        outputOffset = 0;

        initSummaryPanel();
        initConsolePanel();
        initContentItemsPanel();
        initResultListeners();
        updateResult(getExecutionResult(), snapshotItemsTableModel, changeSetItemsTableModel, comparisonItemsTableModel);

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
