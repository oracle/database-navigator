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
    private LiquibaseExecutionItemsTableModel executionItemsTableModel;
    private int outputOffset;

    public LiquibaseExecutionResultForm(@NotNull LiquibaseExecutionResult result) {
        super(result);
        initActionsPanel();
        initSummaryPanel();
        initConsolePanel();
        initExecutionItemsPanel();
        initResultListeners();
        updateResult(result, executionItemsTableModel);
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

    private void initExecutionItemsPanel() {
        LiquibaseExecutionResult result = getExecutionResult();
        if (!result.getOperation().supportsProcessedItems()) return;

        executionItemsTableModel = new LiquibaseExecutionItemsTableModel(result);

        LiquibaseExecutionItemsTable executionItemsTable = new LiquibaseExecutionItemsTable(this, executionItemsTableModel);
        contentTabbedPane.addTab("Processed Items", new DBNScrollPane(executionItemsTable));
    }

    private void initResultListeners() {
        LiquibaseExecutionResult result = getExecutionResult();
        result.addListener(() -> Dispatch.run(false, () -> updateResult(result, executionItemsTableModel)));
    }

    private void updateResult(
            @NotNull LiquibaseExecutionResult result,
            @Nullable LiquibaseExecutionItemsTableModel tableModel) {
        boolean outputChanged = updateConsoleOutput(result);
        if (tableModel != null) tableModel.refresh();
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
        initExecutionItemsPanel();
        initResultListeners();
        updateResult(getExecutionResult(), executionItemsTableModel);

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
