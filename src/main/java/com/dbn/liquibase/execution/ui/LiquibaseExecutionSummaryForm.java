package com.dbn.liquibase.execution.ui;

import com.dbn.common.message.MessageType;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.messages.DBNMessageForm;
import com.dbn.connection.ConnectionHandler;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.object.DBSchema;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.nls.NlsResources.txt;

/** Summary panel for a Liquibase operation result. */
public class LiquibaseExecutionSummaryForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel connectionLabel;
    private JLabel schemaLabel;
    private JLabel operationLabel;
    private JLabel statusLabel;
    private JLabel durationLabel;
    private JPanel messagePanel;
    private DBNMessageForm messageForm;

    LiquibaseExecutionSummaryForm(@NotNull LiquibaseExecutionResult result) {
        super(null, result.getProject());

        ConnectionHandler connection = result.getConnection();
        connectionLabel.setIcon(connection.getIcon());
        connectionLabel.setText(connection.getName());

        DBSchema schema = result.getSchema();
        schemaLabel.setIcon(schema.getIcon());
        schemaLabel.setText(schema.getName());

        operationLabel.setText(result.getOperation().name());
        statusLabel.setText(result.isSuccessful() ? "Successful" : "In progress or failed");
        long duration = result.getEndTime() > 0 ? result.getEndTime() - result.getStartTime() : 0;
        durationLabel.setText(duration > 0 ? duration + " ms" : "");

        initMessageForm(result, schema);
    }

    private void initMessageForm(@NotNull LiquibaseExecutionResult result, @NotNull DBSchema schema) {
        TitledMessage message = createMessage(result, schema);
        messageForm = new DBNMessageForm(this, message);
        messageForm.setMessage(message);
        messagePanel.add(messageForm.getComponent());
        result.addListener(() -> Dispatch.run(mainPanel, () -> updateMessageForm(result)));
        if (result.getEndTime() > 0) updateMessageForm(result);
    }

    private void updateMessageForm(@NotNull LiquibaseExecutionResult result) {
        messageForm.setMessage(createMessage(result, result.getSchema()));
    }

    @NotNull
    private static TitledMessage createMessage(
            @NotNull LiquibaseExecutionResult result,
            @NotNull DBSchema schema) {
        if (result.getEndTime() == 0) {
            return new TitledMessage(
                    MessageType.PROCESSING,
                    txt("app.liquibase.action.Liquibase"),
                    txt("prc.liquibase.text.Initializing", schema.getName()));
        }

        MessageType messageType = result.isSuccessful() ? MessageType.SUCCESS : MessageType.ERROR;
        String messageKey = result.isSuccessful() ?
                "prc.liquibase.text.OperationCompleted" :
                "prc.liquibase.text.OperationFailed";
        return new TitledMessage(
                messageType,
                txt("app.liquibase.action.Liquibase"),
                txt(messageKey, schema.getName()));
    }

    @NotNull
    @Override
    public JPanel getMainComponent() { return mainPanel; }
}
