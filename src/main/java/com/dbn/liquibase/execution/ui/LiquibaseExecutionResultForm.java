package com.dbn.liquibase.execution.ui;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.thread.Dispatch;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.execution.logging.LogOutput;
import com.dbn.execution.logging.LogOutputContext;
import com.dbn.execution.logging.ui.DatabaseLoggingResultConsole;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

/** Console form for Liquibase operation output. */
public class LiquibaseExecutionResultForm extends ExecutionResultFormBase<LiquibaseExecutionResult> {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel summaryPanel;
    private JPanel consolePanel;
    private JTabbedPane contentTabbedPane;

    private final DatabaseLoggingResultConsole console;
    private final LiquibaseProcessedItemsTableModel processedItemsTableModel;

    public LiquibaseExecutionResultForm(@NotNull LiquibaseExecutionResult result) {
        super(result);
        console = new DatabaseLoggingResultConsole(result.getConnection(), result.getName(), false);
        summaryPanel.add(new LiquibaseExecutionSummaryForm(result).getComponent(), BorderLayout.CENTER);
        consolePanel.add(console.getComponent(), BorderLayout.CENTER);
        contentTabbedPane.addTab("Console", consolePanel);
        processedItemsTableModel = new LiquibaseProcessedItemsTableModel(result);

        LiquibaseProcessedItemsTable processedItemsTable = new LiquibaseProcessedItemsTable(this, processedItemsTableModel);
        contentTabbedPane.addTab("Processed Items", new com.intellij.ui.components.JBScrollPane(processedItemsTable));
        result.addListener(() -> Dispatch.run(false, () -> processedItemsTableModel.refresh()));
        writeOutput(result.getConsoleOutput());
        writeOutput(result.getErrorOutput());
        Disposer.register(this, console);
    }

    private void writeOutput(String output) {
        if (output.isEmpty()) return;
        LogOutputContext context = new LogOutputContext(getExecutionResult().getConnection());
        context.setHideEmptyLines(false);
        console.writeToConsole(context, LogOutput.createStdOutput(output));
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
