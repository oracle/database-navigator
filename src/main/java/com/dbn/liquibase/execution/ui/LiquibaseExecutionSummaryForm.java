package com.dbn.liquibase.execution.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.message.MessageType;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.task.TaskStatus;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.messages.DBNMessageForm;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.LiquibaseOperationSupport;
import com.dbn.liquibase.execution.LiquibaseRollbackInstruction;
import com.dbn.liquibase.execution.LiquibaseUpdateInstruction;
import com.dbn.liquibase.execution.LiquibaseUpdateType;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
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
import java.util.Objects;

import static com.dbn.common.text.TextContent.plain;
import static com.dbn.common.ui.link.Hyperlinks.onHyperlinkAccess;
import static com.dbn.common.ui.util.Tooltips.setToolTipText;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.liquibase.execution.LiquibaseFeature.CHANGELOG_TAG;
import static com.dbn.liquibase.execution.LiquibaseFeature.CHANGESET_ITEMS;
import static com.dbn.liquibase.execution.LiquibaseFeature.CHECKPOINT_TAG;
import static com.dbn.liquibase.execution.LiquibaseFeature.COMPARISON_ITEMS;
import static com.dbn.liquibase.execution.LiquibaseFeature.DATABASE_TAG;
import static com.dbn.liquibase.execution.LiquibaseFeature.ROLLBACK;
import static com.dbn.liquibase.execution.LiquibaseFeature.SNAPSHOT_ITEMS;
import static com.dbn.liquibase.execution.LiquibaseFeature.TRACKING_TABLES;
import static com.dbn.liquibase.execution.LiquibaseFeature.UPDATE_INSTRUCTION;
import static com.dbn.liquibase.execution.LiquibaseFeature.WORKSPACE;
import static com.dbn.liquibase.execution.LiquibaseRollbackType.COUNT;
import static com.dbn.liquibase.execution.LiquibaseRollbackType.TAG;
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
    private JLabel processedItemsCaptionLabel;
    private JLabel processedItemsLabel;
    private JLabel statusLabel;
    private JLabel durationLabel;
    private JLabel rollbackTypeCaptionLabel;
    private JLabel rollbackTypeLabel;
    private JLabel updateTypeCaptionLabel;
    private JLabel updateTypeLabel;
    private JLabel updateValueCaptionLabel;
    private JLabel updateValueLabel;
    private JLabel tagCaptionLabel;
    private JLabel tagLabel;
    private JLabel changelogLabel;
    private JLabel documentationLabel;
    private JLabel databaseChangeLogLabel;
    private JLabel databaseChangeLogLockLabel;
    private DBNHyperlinkLabel changelogLink;
    private DBNHyperlinkLabel documentationLink;
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
        boolean sameConnection = hasSameConnection(result);
        setSchemaContext(
                result.getSourceSchema(),
                sourceConnectionCaptionLabel,
                sourceConnectionLabel,
                sourceSchemaCaptionLabel,
                sourceSchemaLabel,
                true);
        setSchemaContext(
                result.getTargetSchema(),
                targetConnectionCaptionLabel,
                targetConnectionLabel,
                targetSchemaCaptionLabel,
                targetSchemaLabel,
                !sameConnection);

        operationLabel.setText(result.getOperation().getName());
        setToolTipText(operationLabel, result.getOperation().getDescription());
        updateStatus(result);
        updateProcessedItems(result);
        updateRollbackInfo(result);
        updateUpdateInfo(result);
        updateTagInfo(result);

        onHyperlinkAccess(changelogLink, e -> openChangelog(result));
        onHyperlinkAccess(documentationLink, e -> openDocumentation(result));
        onHyperlinkAccess(databaseChangeLogLink,e -> navigateToTable(result, result.getDatabaseChangeLogTableName()));
        onHyperlinkAccess(databaseChangeLogLockLink,e -> navigateToTable(result, result.getDatabaseChangeLogLockTableName()));

        databaseChangeLogInfoLabel.setContent(plain(txt("app.liquibase.hint.DatabaseChangeLogTable")));
        databaseChangeLogLockInfoLabel.setContent(plain(txt("app.liquibase.hint.DatabaseChangeLogLockTable")));
        updateChangelogLink(result);
        updateDocumentationLink(result);
        updateLiquibaseTableLinks(result);

        initMessageForm(result, result.getRelevantSchema());
    }

    private void initContextLabels(@NotNull LiquibaseExecutionResult result) {
        DBSchema sourceSchema = result.getSourceSchema();
        DBSchema targetSchema = result.getTargetSchema();
        boolean sourceVisible = sourceSchema != null;
        boolean targetVisible = targetSchema != null;
        boolean sameConnection = hasSameConnection(result);
        boolean qualified = sourceVisible && targetVisible;

        sourceConnectionCaptionLabel.setText(txt(qualified && !sameConnection ?
                "app.liquibase.label.SourceConnection" :
                "app.object.label.Connection"));
        sourceSchemaCaptionLabel.setText(txt(qualified ?
                "app.liquibase.label.SourceSchema" :
                "app.object.label.Schema"));
        targetConnectionCaptionLabel.setText(txt(qualified ?
                "app.liquibase.label.TargetConnection" :
                "app.object.label.Connection"));
        targetSchemaCaptionLabel.setText(txt(qualified ?
                "app.liquibase.label.TargetSchema" :
                "app.object.label.Schema"));
    }

    private void setSchemaContext(
            @Nullable DBSchema schema,
            @NotNull JLabel connectionCaptionLabel,
            @NotNull JLabel connectionLabel,
            @NotNull JLabel schemaCaptionLabel,
            @NotNull JLabel schemaLabel,
            boolean connectionVisible) {
        boolean visible = schema != null;
        connectionCaptionLabel.setVisible(visible && connectionVisible);
        connectionLabel.setVisible(visible && connectionVisible);
        schemaCaptionLabel.setVisible(visible);
        schemaLabel.setVisible(visible);
        if (!visible) return;

        ConnectionHandler connection = schema.getConnection();
        connectionLabel.setIcon(connection.getIcon());
        connectionLabel.setText(connection.getName());
        schemaLabel.setIcon(schema.getIcon());
        schemaLabel.setText(schema.getName());
    }

    private static boolean hasSameConnection(@NotNull LiquibaseExecutionResult result) {
        DBSchema sourceSchema = result.getSourceSchema();
        DBSchema targetSchema = result.getTargetSchema();
        if (sourceSchema == null) return false;
        if (targetSchema == null) return false;

        ConnectionId sourceConnectionId = sourceSchema.getConnection().getConnectionId();
        ConnectionId targetConnectionId = targetSchema.getConnection().getConnectionId();
        return Objects.equals(sourceConnectionId, targetConnectionId);
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
        updateProcessedItems(result);
        updateRollbackInfo(result);
        updateUpdateInfo(result);
        updateTagInfo(result);
        updateChangelogLink(result);
        updateDocumentationLink(result);
        updateLiquibaseTableLinks(result);
        messageForm.setMessage(createMessage(result, result.getRelevantSchema()));
        updateDuration(result);
    }

    private void updateChangelogLink(@NotNull LiquibaseExecutionResult result) {
        LiquibaseOperation operation = result.getOperation();
        boolean relevant = operation.requires(WORKSPACE);
        changelogLabel.setVisible(relevant);
        if (!relevant) {
            changelogLink.setVisible(false);
            setToolTipText(changelogLink, null);
            return;
        }

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

        openFile(result, changelogPath);
    }

    private void updateDocumentationLink(@NotNull LiquibaseExecutionResult result) {
        boolean relevant = result.getOperation() == LiquibaseOperation.GENERATE_DATABASE_DOCUMENTATION;
        documentationLabel.setVisible(relevant);
        documentationLink.setVisible(false);
        setToolTipText(documentationLink, null);
        if (!relevant) return;

        Path documentationPath = result.getDocumentationPath();
        if (documentationPath == null || !Files.isRegularFile(documentationPath)) return;

        documentationLink.setHyperlinkText(documentationPath.getFileName().toString());
        setToolTipText(documentationLink, documentationPath.toString());
        documentationLink.setVisible(true);
    }

    private void openDocumentation(@NotNull LiquibaseExecutionResult result) {
        Path documentationPath = result.getDocumentationPath();
        if (documentationPath == null) return;

        openFile(result, documentationPath);
    }

    private void openFile(@NotNull LiquibaseExecutionResult result, @NotNull Path path) {

        Background.run(() -> {
            LocalFileSystem fileSystem = LocalFileSystem.getInstance();
            VirtualFile file = fileSystem.refreshAndFindFileByIoFile(path.toFile());
            if (file == null) return;

            Editors.openFileEditor(result.getProject(), file, true);
        });
    }

    private void updateLiquibaseTableLinks(@NotNull LiquibaseExecutionResult result) {
        LiquibaseOperation operation = result.getOperation();
        if (!operation.supports(TRACKING_TABLES)) {
            databaseChangeLogLabel.setVisible(false);
            databaseChangeLogLink.setVisible(false);
            databaseChangeLogInfoLabel.setVisible(false);
            databaseChangeLogLockLabel.setVisible(false);
            databaseChangeLogLockLink.setVisible(false);
            databaseChangeLogLockInfoLabel.setVisible(false);
            return;
        }

        updateLiquibaseTableLink(
                result.getDatabaseChangeLogTableName(),
                databaseChangeLogLabel,
                databaseChangeLogLink,
                databaseChangeLogInfoLabel);
        updateLiquibaseTableLink(
                result.getDatabaseChangeLogLockTableName(),
                databaseChangeLogLockLabel,
                databaseChangeLogLockLink,
                databaseChangeLogLockInfoLabel);
    }

    private static void updateLiquibaseTableLink(
            @NotNull String tableName,
            @NotNull JLabel label,
            @NotNull DBNHyperlinkLabel link,
            @NotNull DBNInfoLabel infoLabel) {
        label.setVisible(true);
        link.setIcon(Icons.DBO_TABLE);
        link.setHyperlinkText(tableName);
        link.setVisible(true);
        infoLabel.setVisible(true);
    }

    private void navigateToTable(
            @NotNull LiquibaseExecutionResult result,
            @NotNull String tableName) {
        DBSchema schema = result.getTargetSchema();
        DBSchema navigationSchema = schema == null ? result.getRelevantSchema() : schema;

        Progress.prompt(
                result.getProject(),
                navigationSchema,
                true,
                txt("prc.objects.title.LoadingObjects"),
                txt("prc.objects.text.LoadingObjects", tableName),
                progress -> {
                    progress.checkCanceled();
                    DBObject table = navigationSchema.getTable(tableName);
                    progress.checkCanceled();
                    if (table != null) table.navigate(true);
                });
    }

    private void updateStatus(@NotNull LiquibaseExecutionResult result) {
        updateStatusLabel(result);
        updateDuration(result);
    }

    private void updateStatusLabel(@NotNull LiquibaseExecutionResult result) {
        statusLabel.setText(result.getStatus().getName());
        statusLabel.setForeground(getStatusColor(result.getStatus()));
    }

    private void updateProcessedItems(@NotNull LiquibaseExecutionResult result) {
        LiquibaseOperationSupport support = result.getOperation().getSupport();
        boolean visible = support.supports(SNAPSHOT_ITEMS) ||
                support.supports(COMPARISON_ITEMS) ||
                support.supports(CHANGESET_ITEMS);
        processedItemsCaptionLabel.setVisible(visible);
        processedItemsLabel.setVisible(visible);
        if (!visible) return;

        int count = support.supports(SNAPSHOT_ITEMS) ? result.getSnapshotItems().size() :
                support.supports(COMPARISON_ITEMS) ? result.getComparisonItems().size() :
                result.getChangeSetItems().size();
        processedItemsLabel.setText(Integer.toString(count));
    }

    private void updateRollbackInfo(@NotNull LiquibaseExecutionResult result) {
        LiquibaseOperationSupport support = result.getOperation().getSupport();
        boolean rollback = support.supports(ROLLBACK);
        rollbackTypeCaptionLabel.setVisible(rollback);
        rollbackTypeLabel.setVisible(rollback);
        if (!rollback) return;

        rollbackTypeCaptionLabel.setText(txt("app.liquibase.label.RollbackType"));
        rollbackTypeLabel.setText(result.getRollbackInstruction().getType().getName());
    }

    private void updateUpdateInfo(@NotNull LiquibaseExecutionResult result) {
        boolean update = result.getOperation().getSupport().supports(UPDATE_INSTRUCTION);
        updateTypeCaptionLabel.setVisible(update);
        updateTypeLabel.setVisible(update);
        updateValueCaptionLabel.setVisible(false);
        updateValueLabel.setVisible(false);
        if (!update) return;

        LiquibaseUpdateInstruction instruction = result.getUpdateInstruction();
        updateTypeCaptionLabel.setText(txt("app.liquibase.label.UpdateType"));
        updateTypeLabel.setText(instruction.getType().getName());
        if (instruction.getType() == LiquibaseUpdateType.COUNT) {
            updateValueCaptionLabel.setText(txt("app.liquibase.label.UpdateCount"));
            updateValueLabel.setText(Integer.toString(instruction.getCount()));
            updateValueCaptionLabel.setVisible(true);
            updateValueLabel.setVisible(true);
        } else if (instruction.getType() == LiquibaseUpdateType.TAG) {
            updateValueCaptionLabel.setText(txt("app.liquibase.label.UpdateTag"));
            updateValueLabel.setText(instruction.getTag());
            updateValueCaptionLabel.setVisible(isNotEmpty(instruction.getTag()));
            updateValueLabel.setVisible(isNotEmpty(instruction.getTag()));
        }
    }

    private void updateTagInfo(@NotNull LiquibaseExecutionResult result) {
        LiquibaseOperation operation = result.getOperation();
        LiquibaseOperationSupport support = operation.getSupport();

        if (support.supports(CHANGELOG_TAG)) {
            updateTagInfo(result.getChangelogTag(), "app.liquibase.label.ChangelogTag");
        } else if (support.supports(DATABASE_TAG)) {
            updateTagInfo(result.getDatabaseTag(), "app.liquibase.label.DatabaseTag");
        } else if (support.supports(CHECKPOINT_TAG)) {
            updateTagInfo(result.getCheckpointTag(), "app.liquibase.label.CheckpointTag");
        } else if (support.supports(ROLLBACK)) {
            updateRollbackTagInfo(result);
        } else {
            clearTagInfo();
        }
    }

    private void updateRollbackTagInfo(@NotNull LiquibaseExecutionResult result) {
        LiquibaseRollbackInstruction instruction = result.getRollbackInstruction();
        if (instruction.getType() == TAG) {
            updateTagInfo(instruction.getTag(), "app.liquibase.label.RollbackTag");
            return;
        }

        if (instruction.getType() == COUNT) {
            updateTagInfo(Integer.toString(instruction.getCount()), "app.liquibase.label.RollbackCount");
            return;
        }

        updateTagInfo(
                ensureFormatter().formatDateTime(instruction.getDate()),
                "app.liquibase.label.RollbackDate");
    }

    private void updateTagInfo(@Nullable String tag, @NotNull String captionKey) {
        boolean visible = isNotEmpty(tag);
        tagCaptionLabel.setVisible(visible);
        tagLabel.setVisible(visible);
        if (visible) {
            tagCaptionLabel.setText(txt(captionKey));
            tagLabel.setText(tag);
        }
    }

    private void clearTagInfo() {
        tagCaptionLabel.setVisible(false);
        tagLabel.setVisible(false);
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
                status == TaskStatus.CANCELLED || status == TaskStatus.SKIPPED ? MessageType.WARNING :
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
