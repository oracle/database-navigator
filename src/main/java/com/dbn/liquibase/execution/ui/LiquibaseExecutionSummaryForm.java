package com.dbn.liquibase.execution.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;

/** Summary panel for a Liquibase operation result. */
public class LiquibaseExecutionSummaryForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel connectionLabel;
    private JLabel operationLabel;
    private JLabel statusLabel;
    private JLabel durationLabel;

    LiquibaseExecutionSummaryForm(@NotNull LiquibaseExecutionResult result) {
        super(null, result.getProject());
        connectionLabel.setText(result.getConnection().getName());
        operationLabel.setText(result.getOperation().name());
        statusLabel.setText(result.isSuccessful() ? "Successful" : "In progress or failed");
        long duration = result.getEndTime() > 0 ? result.getEndTime() - result.getStartTime() : 0;
        durationLabel.setText(duration > 0 ? duration + " ms" : "");
    }

    @NotNull
    @Override
    public JPanel getMainComponent() { return mainPanel; }
}
