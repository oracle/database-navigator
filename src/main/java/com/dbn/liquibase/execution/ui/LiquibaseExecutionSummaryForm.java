package com.dbn.liquibase.execution.ui;

import com.dbn.common.message.MessageType;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.task.TaskStatus;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.link.Hyperlinks;
import com.dbn.common.ui.messages.DBNMessageForm;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.object.DBSchema;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.dbn.common.ui.util.Tooltips.setToolTipText;
import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.nls.NlsResources.txtOr;

/** Summary panel for a Liquibase operation result. */
public class LiquibaseExecutionSummaryForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel sourceConnectionCaptionLabel;
    private JLabel sourceConnectionLabel;
    private JLabel sourceSchemaCaptionLabel;
    private JLabel sourceSchemaLabel;
    private JLabel targetConnectionCaptionLabel;
    private JLabel targetConnectionLabel;
    private JLabel targetSchemaCaptionLabel;
    private JLabel targetSchemaLabel;
    private JLabel operationLabel;
    private JLabel statusLabel;
    private JLabel durationLabel;
    private DBNHyperlinkLabel changelogLink;
    private JPanel messagePanel;
    private DBNMessageForm messageForm;
    private final LiquibaseExecutionResult result;

    LiquibaseExecutionSummaryForm(Disposable parent, @NotNull LiquibaseExecutionResult result) {
        super(parent, result.getProject());
        this.result = result;

        initContextLabels(result);
        setSchemaContext(
                result.getSourceSchema(),
                sourceConnectionCaptionLabel,
                sourceConnectionLabel,
                sourceSchemaCaptionLabel,
                sourceSchemaLabel);
        setSchemaContext(
                result.getTargetSchema(),
                targetConnectionCaptionLabel,
                targetConnectionLabel,
                targetSchemaCaptionLabel,
                targetSchemaLabel);

        operationLabel.setText(result.getOperation().getName());
        setToolTipText(operationLabel, result.getOperation().getDescription());
        updateStatus(result);
        Hyperlinks.onHyperlinkAccess(changelogLink, e -> openChangelog(result));
        updateChangelogLink(result);

        initMessageForm(result, result.getRelevantSchema());
    }

    private void initContextLabels(@NotNull LiquibaseExecutionResult result) {
        boolean sourceVisible = result.getSourceSchema() != null;
        boolean targetVisible = result.getTargetSchema() != null;
        boolean qualified = sourceVisible && targetVisible;

        sourceConnectionCaptionLabel.setText(txt(qualified ?
                "cfg.liquibase.label.SourceConnection" :
                "app.object.label.Connection"));
        sourceSchemaCaptionLabel.setText(txt(qualified ?
                "cfg.liquibase.label.SourceSchema" :
                "app.object.label.Schema"));
        targetConnectionCaptionLabel.setText(txt(qualified ?
                "cfg.liquibase.label.TargetConnection" :
                "app.object.label.Connection"));
        targetSchemaCaptionLabel.setText(txt(qualified ?
                "cfg.liquibase.label.TargetSchema" :
                "app.object.label.Schema"));
    }

    private void setSchemaContext(
            @Nullable DBSchema schema,
            @NotNull JLabel connectionCaptionLabel,
            @NotNull JLabel connectionLabel,
            @NotNull JLabel schemaCaptionLabel,
            @NotNull JLabel schemaLabel) {
        boolean visible = schema != null;
        connectionCaptionLabel.setVisible(visible);
        connectionLabel.setVisible(visible);
        schemaCaptionLabel.setVisible(visible);
        schemaLabel.setVisible(visible);
        if (!visible) return;

        ConnectionHandler connection = schema.getConnection();
        connectionLabel.setIcon(connection.getIcon());
        connectionLabel.setText(connection.getName());
        schemaLabel.setIcon(schema.getIcon());
        schemaLabel.setText(schema.getName());
    }

    private void initMessageForm(@NotNull LiquibaseExecutionResult result, @NotNull DBSchema schema) {
        TitledMessage message = createMessage(result, schema);
        messageForm = new DBNMessageForm(this, message);
        messageForm.setMessage(message);
        messagePanel.add(messageForm.getComponent());
        result.addListener(() -> Dispatch.run(mainPanel, () -> updateMessageForm(result)));
        if (result.getTiming().getEndTime() > 0) updateMessageForm(result);
    }

    private void updateMessageForm(@NotNull LiquibaseExecutionResult result) {
        updateStatus(result);
        updateChangelogLink(result);
        messageForm.setMessage(createMessage(result, result.getRelevantSchema()));
    }

    private void updateChangelogLink(@NotNull LiquibaseExecutionResult result) {
        Path changelogPath = result.getChangelogPath();
        if (changelogPath == null || !Files.isRegularFile(changelogPath)) {
            changelogLink.setVisible(false);
            setToolTipText(changelogLink, null);
            return;
        }

        changelogLink.setHyperlinkText(changelogPath.getFileName().toString());
        setToolTipText(changelogLink, changelogPath.toString());
        changelogLink.setVisible(true);
    }

    private void openChangelog(@NotNull LiquibaseExecutionResult result) {
        Path changelogPath = result.getChangelogPath();
        if (changelogPath == null) return;

        Background.run(() -> {
            LocalFileSystem fileSystem = LocalFileSystem.getInstance();
            VirtualFile file = fileSystem.refreshAndFindFileByIoFile(changelogPath.toFile());
            if (file == null) return;

            Editors.openFileEditor(result.getProject(), file, true);
        });
    }

    private void updateStatus(@NotNull LiquibaseExecutionResult result) {
        statusLabel.setText(result.getStatus().getName());
        durationLabel.setText(presentableDuration(result.getExecutionDuration(), true));
    }

    @NotNull
    private static TitledMessage createMessage(
            @NotNull LiquibaseExecutionResult result,
            @NotNull DBSchema schema) {
        LiquibaseOperation operation = result.getOperation();
        TaskStatus status = result.getStatus();
        String schemaName = schema.getName();

        if (status == TaskStatus.RUNNING) {
            return new TitledMessage(
                    MessageType.PROCESSING,
                    txt(getTitleKey(operation, status)),
                    txt(getMessageKey(operation, status), schemaName));
        }

        MessageType messageType =
                status == TaskStatus.CANCELLED ? MessageType.WARNING :
                status == TaskStatus.DONE ? MessageType.SUCCESS : MessageType.ERROR;
        return new TitledMessage(
                messageType,
                txt(getTitleKey(operation, status)),
                txt(getMessageKey(operation, status), schemaName));
    }

    @NotNull
    private static String getTitleKey(
            @NotNull LiquibaseOperation operation,
            @NotNull TaskStatus status) {
        return getOperationKey("title", operation, status);
    }

    @NotNull
    private static String getMessageKey(
            @NotNull LiquibaseOperation operation,
            @NotNull TaskStatus status) {
        return getOperationKey("text", operation, status);
    }

    @NotNull
    private static String getOperationKey(
            @NotNull String category,
            @NotNull LiquibaseOperation operation,
            @NotNull TaskStatus status) {
        String suffix = operation.name() + '_' + status.name();
        String operationKey = "prc.liquibase." + category + ".Operation_" + suffix;
        String fallbackKey = "prc.liquibase." + category + ".Operation_ANY_" + status.name();
        return txtOr(operationKey, fallbackKey);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() { return mainPanel; }
}
