package com.dbn.liquibase.execution;

import com.dbn.common.icon.Icons;
import com.dbn.common.task.TaskStatus;
import com.dbn.common.ui.util.Listeners;
import com.dbn.common.util.ExecutionTiming;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.execution.ExecutionResultBase;
import com.dbn.execution.logging.LogOutput;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.liquibase.execution.logging.LogOutputBuffer;
import com.dbn.liquibase.execution.ui.LiquibaseExecutionResultForm;
import com.intellij.openapi.project.Project;
import liquibase.changelog.ChangeSet;
import liquibase.diff.ObjectDifferences;
import liquibase.structure.DatabaseObject;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Execution-console result for a Liquibase operation and its console output. */
@Getter
public class LiquibaseExecutionResult extends ExecutionResultBase<LiquibaseExecutionResultForm> {
    @Delegate
    private final LiquibaseExecutionInput input;

    private Path changelogPath;
    private String databaseChangeLogTableName = "DATABASECHANGELOG";
    private String databaseChangeLogLockTableName = "DATABASECHANGELOGLOCK";
    private final Listeners<Runnable> listeners = Listeners.create(this);
    private final LogOutputBuffer output;
    private final StringBuilder sqlOutput = new StringBuilder();
    private final ExecutionTiming timing = new ExecutionTiming();
    private final Map<String, LiquibaseSnapshotItem> snapshotItems = new LinkedHashMap<>();
    private final Map<String, LiquibaseChangeSetItem> changeSetItems = new LinkedHashMap<>();
    private final Map<String, LiquibaseComparisonItem> comparisonItems = new LinkedHashMap<>();
    private volatile TaskStatus status = TaskStatus.NEW;

    @NotNull
    public List<LiquibaseSnapshotItem> getSnapshotItems() {
        synchronized (snapshotItems) {
            return new ArrayList<>(snapshotItems.values());
        }
    }

    @NotNull
    public List<LiquibaseChangeSetItem> getChangeSetItems() {
        synchronized (changeSetItems) {
            return new ArrayList<>(changeSetItems.values());
        }
    }

    @NotNull
    public List<LiquibaseComparisonItem> getComparisonItems() {
        synchronized (comparisonItems) {
            return new ArrayList<>(comparisonItems.values());
        }
    }

    @NotNull
    public LiquibaseSnapshotItem ensureSnapshotItem(@NotNull DatabaseObject databaseObject) {
        return ensureItem(snapshotItems,
                buildDatabaseObjectKey(databaseObject),
                () -> new LiquibaseSnapshotItem(databaseObject));
    }

    @NotNull
    public LiquibaseChangeSetItem ensureChangeSetItem(@NotNull ChangeSet changeSet) {
        return ensureItem(changeSetItems,
                buildChangeSetKey(changeSet),
                () -> new LiquibaseChangeSetItem(changeSet));
    }

    @NotNull
    public LiquibaseChangeSetItem ensureChangeSetItem(
            @NotNull ChangeSet changeSet,
            @NotNull LiquibaseExecutionItemStatus status,
            @Nullable String message) {
        return ensureItem(changeSetItems,
                buildChangeSetKey(changeSet),
                () -> new LiquibaseChangeSetItem(changeSet, status, message));
    }

    @NotNull
    public LiquibaseComparisonItem ensureComparisonItem(
            @Nullable DatabaseObject sourceObject,
            @Nullable DatabaseObject targetObject,
            @NotNull LiquibaseComparisonItemStatus status,
            @Nullable ObjectDifferences differences) {
        LiquibaseComparisonItem item = new LiquibaseComparisonItem(sourceObject, targetObject, status, differences);
        synchronized (comparisonItems) {
            LiquibaseComparisonItem existing = comparisonItems.get(item.getKey());
            if (existing != null) return existing;
            comparisonItems.put(item.getKey(), item);
        }
        notifyItemsChanged();
        return item;
    }

    @NotNull
    private <I extends LiquibaseExecutionItem> I ensureItem(
            @NotNull Map<String, I> items,
            @NotNull String key,
            @NotNull Supplier<I> factory) {
        I item;
        boolean created = false;
        synchronized (items) {
            item = items.get(key);
            if (item == null) {
                item = factory.get();
                items.put(key, item);
                created = true;
            }
        }
        if (created) notifyItemsChanged();
        return item;
    }

    @NotNull
    public static String buildDatabaseObjectKey(@NotNull DatabaseObject databaseObject) {
        String schemaName = databaseObject.getSchema() == null ? "" : databaseObject.getSchema().getName();
        return databaseObject.getObjectTypeName() + ':' + schemaName + ':' + databaseObject.getName();
    }

    @NotNull
    public static String buildChangeSetKey(@NotNull ChangeSet changeSet) {
        return changeSet.getFilePath() + ':' + changeSet.getAuthor() + ':' + changeSet.getId();
    }

    public void updateExecutionItem(
            @NotNull LiquibaseSnapshotItem item,
            @NotNull DatabaseObject databaseObject,
            @NotNull LiquibaseExecutionItemStatus status,
            String message) {
        item.update(databaseObject, status, message);
        notifyItemsChanged();
    }

    public void addListener(@NotNull Runnable listener) {
        listeners.add(listener);
    }

    public void notifyItemsChanged() {
        listeners.notify(Runnable::run);
    }

    public LiquibaseExecutionResult(
            @NotNull LiquibaseExecutionInput input) {
        this.input = input;
        this.output = new LogOutputBuffer(input.getProject());
    }

    public void setChangelogPath(@Nullable Path changelogPath) {
        this.changelogPath = changelogPath;
    }

    public void setLiquibaseTableNames(
            @NotNull String databaseChangeLogTableName,
            @NotNull String databaseChangeLogLockTableName) {
        boolean changed = !databaseChangeLogTableName.equals(this.databaseChangeLogTableName) ||
                !databaseChangeLogLockTableName.equals(this.databaseChangeLogLockTableName);
        this.databaseChangeLogTableName = databaseChangeLogTableName;
        this.databaseChangeLogLockTableName = databaseChangeLogLockTableName;
        if (changed) notifyItemsChanged();
    }

    public void notifyStarted() {
        status = TaskStatus.RUNNING;
        timing.start();
        notifyItemsChanged();
    }

    public void notifyFinished(@NotNull TaskStatus status) {
        this.status = status;
        timing.finish();
        notifyItemsChanged();
    }

    public void notifyCancelled() {
        status = TaskStatus.CANCELLED;
        timing.finish();
        notifyItemsChanged();
    }

    @NotNull
    public Duration getExecutionDuration() {
        return timing.getDuration();
    }

    public void appendConsoleOutput(@Nullable @Nls String output) {
        if (output == null) return;
        this.output.appendStdOutput(output);
        notifyItemsChanged();
    }

    public void appendSqlOutput(@Nullable String output) {
        if (output == null || output.isEmpty()) return;
        synchronized (sqlOutput) {
            sqlOutput.append(output);
            if (!output.endsWith("\n")) sqlOutput.append('\n');
        }
        notifyItemsChanged();
    }

    @NotNull
    public String getSqlOutput() {
        synchronized (sqlOutput) {
            return sqlOutput.toString();
        }
    }

    public void appendErrorOutput(@Nullable @Nls String output) {
        if (output == null) return;
        this.output.appendErrOutput(output);
        notifyItemsChanged();
    }

    public void appendInfoOutput(@Nullable @Nls String output) {
        if (output == null) return;
        this.output.appendSysOutput(getConnection(), output);
        notifyItemsChanged();
    }

    @NotNull
    public List<LogOutput> getOutput() {
        return output.getOutput();
    }

    @Nullable
    @Override
    public LiquibaseExecutionResultForm createForm() {
        return new LiquibaseExecutionResultForm(this);
    }

    @NotNull
    @Override
    public String getName() {
        return getConnection().getName() + " - " + input.getRelevantSchema().getName() + " - " + input.getOperation().getName();
    }

    @Override
    public Icon getIcon() {
        return Icons.DB_LIQUIBASE;
    }

    @NotNull
    @Override
    public Project getProject() {
        return getConnection().getProject();
    }

    @Override
    public ConnectionId getConnectionId() {
        return getConnection().getConnectionId();
    }

    @NotNull
    @Override
    public ConnectionHandler getConnection() {
        return input.getRelevantConnection();
    }

    @Override
    public DBLanguagePsiFile createPreviewFile() {
        return null;
    }
}
