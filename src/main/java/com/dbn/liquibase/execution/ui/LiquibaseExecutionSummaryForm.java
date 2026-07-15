package com.dbn.liquibase.execution.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.message.MessageType;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.task.TaskStatus;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.link.Hyperlinks;
import com.dbn.common.ui.messages.DBNMessageForm;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.LiquibaseOperationSupport;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.dbn.common.text.TextContent.plain;
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
    private JLabel changeSetCountCaptionLabel;
    private JLabel changeSetCountLabel;
    private JLabel statusLabel;
    private JLabel durationLabel;
    private JLabel databaseChangeLogLabel;
    private JLabel databaseChangeLogLockLabel;
    private DBNHyperlinkLabel changelogLink;
    private DBNHyperlinkLabel databaseChangeLogLink;
    private DBNHyperlinkLabel databaseChangeLogLockLink;
    private DBNInfoLabel databaseChangeLogInfoLabel;
    private DBNInfoLabel databaseChangeLogLockInfoLabel;
    private JPanel messagePanel;
    private DBNMessageForm messageForm;
    private final LiquibaseExecutionResult result;
    private final Timer durationTimer;

    LiquibaseExecutionSummaryForm(Disposable parent, @NotNull LiquibaseExecutionResult result) {
        super(parent, result.getProject());
        this.result = result;
        durationTimer = new Timer(1000, e -> updateDuration(result));
        durationTimer.setRepeats(true);

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
        updateChangeSetCount(result);
        Hyperlinks.onHyperlinkAccess(changelogLink, e -> openChangelog(result));
        Hyperlinks.onHyperlinkAccess(databaseChangeLogLink,
                e -> navigateToTable(result, result.getDatabaseChangeLogTableName()));
        Hyperlinks.onHyperlinkAccess(databaseChangeLogLockLink,
                e -> navigateToTable(result, result.getDatabaseChangeLogLockTableName()));
        databaseChangeLogInfoLabel.setContent(plain(txt("cfg.liquibase.hint.DatabaseChangeLogTable")));
        databaseChangeLogLockInfoLabel.setContent(plain(txt("cfg.liquibase.hint.DatabaseChangeLogLockTable")));
        updateChangelogLink(result);
        updateLiquibaseTableLinks(result);

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
        updateStatusLabel(result);
        updateChangeSetCount(result);
        updateChangelogLink(result);
        updateLiquibaseTableLinks(result);
        messageForm.setMessage(createMessage(result, result.getRelevantSchema()));
        updateDuration(result);
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

    private void updateLiquibaseTableLinks(@NotNull LiquibaseExecutionResult result) {
        LiquibaseOperation operation = result.getOperation();
        LiquibaseOperationSupport support = operation.getSupport();
        if (!support.supportsTrackingTables()) {
            databaseChangeLogLabel.setVisible(false);
            databaseChangeLogLink.setVisible(false);
            databaseChangeLogInfoLabel.setVisible(false);
            databaseChangeLogLockLabel.setVisible(false);
            databaseChangeLogLockLink.setVisible(false);
            databaseChangeLogLockInfoLabel.setVisible(false);
            return;
        }

        DBSchema schema = result.getTargetSchema();
        if (schema == null) schema = result.getRelevantSchema();

        updateLiquibaseTableLink(
                schema,
                result.getDatabaseChangeLogTableName(),
                databaseChangeLogLabel,
                databaseChangeLogLink,
                databaseChangeLogInfoLabel);
        updateLiquibaseTableLink(
                schema,
                result.getDatabaseChangeLogLockTableName(),
                databaseChangeLogLockLabel,
                databaseChangeLogLockLink,
                databaseChangeLogLockInfoLabel);
    }

    private static void updateLiquibaseTableLink(
            @NotNull DBSchema schema,
            @NotNull String tableName,
            @NotNull JLabel label,
            @NotNull DBNHyperlinkLabel link,
            @NotNull DBNInfoLabel infoLabel) {
        DBObject table = schema.getChildObject(DBObjectType.TABLE, tableName);
        if (table == null) {
            label.setVisible(false);
            link.setVisible(false);
            infoLabel.setVisible(false);
            link.setHyperlinkText("");
            return;
        }

        label.setVisible(true);
        link.setIcon(Icons.DBO_TABLE);
        link.setHyperlinkText(table.getName());
        link.setVisible(true);
        infoLabel.setVisible(true);
    }

    private void navigateToTable(
            @NotNull LiquibaseExecutionResult result,
            @NotNull String tableName) {
        DBSchema schema = result.getTargetSchema();
        if (schema == null) schema = result.getRelevantSchema();

        DBObject table = schema.getChildObject(DBObjectType.TABLE, tableName);
        if (table != null) table.navigate(true);
    }

    private void updateStatus(@NotNull LiquibaseExecutionResult result) {
        updateStatusLabel(result);
        updateDuration(result);
    }

    private void updateStatusLabel(@NotNull LiquibaseExecutionResult result) {
        statusLabel.setText(result.getStatus().getName());
        statusLabel.setForeground(getStatusColor(result.getStatus()));
    }

    private void updateChangeSetCount(@NotNull LiquibaseExecutionResult result) {
        boolean visible = result.getOperation().getSupport().supportsChangeSetItems();
        changeSetCountCaptionLabel.setVisible(visible);
        changeSetCountLabel.setVisible(visible);
        if (visible) changeSetCountLabel.setText(Integer.toString(result.getChangeSetItems().size()));
    }

    @NotNull
    private static Color getStatusColor(@NotNull TaskStatus status) {
        if (true) return UIUtil.getLabelForeground();
        // todo cleanup (too colorful)
        return switch (status) {
            case DONE -> Colors.getLabelSuccessForeground();
            case FAILED -> Colors.getLabelErrorForeground();
            case CANCELLED, SKIPPED -> Colors.getLabelWarningForeground();
            default -> UIUtil.getLabelForeground();
        };
    }

    private void updateDuration(@NotNull LiquibaseExecutionResult result) {
        durationLabel.setText(presentableDuration(result.getExecutionDuration(), true));
        if (result.getStatus() == TaskStatus.RUNNING) {
            if (!durationTimer.isRunning()) durationTimer.start();
        } else {
            durationTimer.stop();
        }
    }

    @Override
    public void disposeInner() {
        durationTimer.stop();
        super.disposeInner();
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
        String operationKey = "msg.liquibase." + category + ".Operation_" + suffix;
        String fallbackKey = "msg.liquibase." + category + ".Operation_ANY_" + status.name();
        return txtOr(operationKey, fallbackKey);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() { return mainPanel; }
}
