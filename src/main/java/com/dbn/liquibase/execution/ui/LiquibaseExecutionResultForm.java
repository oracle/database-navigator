package com.dbn.liquibase.execution.ui;

import com.dbn.common.action.DataKeys;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.util.Accessibility;
import com.dbn.common.util.Actions;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.execution.logging.LogOutput;
import com.dbn.execution.logging.LogOutputContext;
import com.dbn.execution.logging.ui.DatabaseLoggingResultConsole;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.intellij.openapi.actionSystem.ActionToolbar;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/** Console form for Liquibase operation output. */
public class LiquibaseExecutionResultForm extends ExecutionResultFormBase<LiquibaseExecutionResult> {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel summaryPanel;
    private JPanel consolePanel;
    private JTabbedPane contentTabbedPane;

    private DatabaseLoggingResultConsole console;
    private LiquibaseProcessedItemsTableModel processedItemsTableModel;
    private int outputOffset;

    public LiquibaseExecutionResultForm(@NotNull LiquibaseExecutionResult result) {
        super(result);
        initActionsPanel();
        initSummaryPanel();
        initConsolePanel();
        initProcessedItemsPanel();
        initResultListeners();
        updateResult(result, processedItemsTableModel);
        Disposer.register(this, console);
    }

    private void initActionsPanel() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, false, "DBN.Execution.Liquibase.Result");
        Accessibility.setAccessibleName(actionToolbar, txt("app.liquibase.aria.ExecutionResultActions"));
        actionsPanel.add(actionToolbar.getComponent());
    }

    private void initSummaryPanel() {
        LiquibaseExecutionResult result = getExecutionResult();
        summaryPanel.add(new LiquibaseExecutionSummaryForm(result).getComponent(), BorderLayout.CENTER);
    }

    private void initConsolePanel() {
        LiquibaseExecutionResult result = getExecutionResult();
        console = new DatabaseLoggingResultConsole(result.getConnection(), result.getName(), false);
        consolePanel = new JPanel(new BorderLayout());
        consolePanel.add(console.getComponent());
        contentTabbedPane.addTab("Console", consolePanel);
    }

    private void initProcessedItemsPanel() {
        LiquibaseExecutionResult result = getExecutionResult();
        processedItemsTableModel = new LiquibaseProcessedItemsTableModel(result);

        LiquibaseProcessedItemsTable processedItemsTable = new LiquibaseProcessedItemsTable(this, processedItemsTableModel);
        contentTabbedPane.addTab("Processed Items", new com.intellij.ui.components.JBScrollPane(processedItemsTable));
    }

    private void initResultListeners() {
        LiquibaseExecutionResult result = getExecutionResult();
        result.addListener(() -> Dispatch.run(false, () -> updateResult(result, processedItemsTableModel)));
    }

    private void updateResult(@NotNull LiquibaseExecutionResult result, @NotNull LiquibaseProcessedItemsTableModel tableModel) {
        updateConsoleOutput(result);
        tableModel.refresh();
    }

    private void updateConsoleOutput(@NotNull LiquibaseExecutionResult result) {
        List<LogOutput> output = result.getOutput();
        while (outputOffset < output.size()) {
            writeOutput(output.get(outputOffset));
            outputOffset++;
        }
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
