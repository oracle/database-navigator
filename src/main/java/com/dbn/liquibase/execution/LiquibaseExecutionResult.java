package com.dbn.liquibase.execution;

import com.dbn.common.icon.Icons;
import com.dbn.common.task.TaskStatus;
import com.dbn.common.ui.util.Listeners;
import com.dbn.common.util.ExecutionTiming;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.execution.ExecutionResultBase;
import com.dbn.execution.logging.LogOutput;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.liquibase.execution.logging.LogOutputBuffer;
import com.dbn.liquibase.execution.ui.LiquibaseExecutionResultForm;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import liquibase.structure.DatabaseObject;
import lombok.Getter;
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

/** Execution-console result for a Liquibase operation and its console output. */
@Getter
public class LiquibaseExecutionResult extends ExecutionResultBase<LiquibaseExecutionResultForm> {
    private final DBObjectRef<DBSchema> schema;
    private final ConnectionRef connection;
    private final LiquibaseOperation operation;
    @Nullable
    private Path changelogPath;
    private final Listeners<Runnable> listeners = Listeners.create(this);
    private final LogOutputBuffer output;
    private final ExecutionTiming timing = new ExecutionTiming();
    private final Map<String, LiquibaseExecutionItem> executionItems = new LinkedHashMap<>();
    private volatile TaskStatus status = TaskStatus.NEW;

    @NotNull
    public List<LiquibaseExecutionItem> getExecutionItems() {
        synchronized (executionItems) {
            return new ArrayList<>(executionItems.values());
        }
    }

    @NotNull
    public LiquibaseExecutionItem ensureExecutionItem(@NotNull DatabaseObject databaseObject) {
        String key = getDatabaseObjectKey(databaseObject);
        LiquibaseExecutionItem item;
        boolean created = false;
        synchronized (executionItems) {
            item = executionItems.get(key);
            if (item == null) {
                item = new LiquibaseExecutionItem(databaseObject);
                executionItems.put(key, item);
                created = true;
            }
        }
        if (created) notifListeners();
        return item;
    }

    @NotNull
    private String getDatabaseObjectKey(@NotNull DatabaseObject databaseObject) {
        String schemaName = databaseObject.getSchema() == null ? "" : databaseObject.getSchema().getName();
        return databaseObject.getObjectTypeName() + ':' + schemaName + ':' + databaseObject.getName();
    }

    public void updateExecutionItem(
            @NotNull LiquibaseExecutionItem item,
            @NotNull DatabaseObject databaseObject,
            @NotNull String status,
            String message) {
        item.update(databaseObject, status, message);
        notifListeners();
    }

    public void addListener(@NotNull Runnable listener) {
        listeners.add(listener);
    }

    private void notifListeners() {
        listeners.notify(Runnable::run);
    }

    public LiquibaseExecutionResult(
            @NotNull DBSchema schema,
            @NotNull LiquibaseOperation operation) {
        this.schema = DBObjectRef.of(schema);
        this.connection = schema.getConnection().ref();
        this.operation = operation;
        this.output = new LogOutputBuffer(schema.getProject());
    }

    public void setChangelogPath(@Nullable Path changelogPath) {
        this.changelogPath = changelogPath;
    }

    @NotNull
    public DBSchema getSchema() {
        return DBObjectRef.ensure(schema);
    }

    public void notifyStarted() {
        status = TaskStatus.RUNNING;
        timing.start();
        notifListeners();
    }

    public void notifyFinished(@NotNull TaskStatus status) {
        this.status = status;
        timing.finish();
        notifListeners();
    }

    public void notifyCancelled() {
        status = TaskStatus.CANCELLED;
        timing.finish();
        notifListeners();
    }

    @NotNull
    public Duration getExecutionDuration() {
        return timing.getDuration();
    }

    public void appendConsoleOutput(@Nullable @Nls String output) {
        if (output == null) return;
        this.output.appendStdOutput(output);
        notifListeners();
    }

    public void appendErrorOutput(@Nullable @Nls String output) {
        if (output == null) return;
        this.output.appendErrOutput(output);
        notifListeners();
    }

    public void appendInfoOutput(@Nullable @Nls String output) {
        if (output == null) return;
        this.output.appendSysOutput(getConnection(), output);
        notifListeners();
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
        return getConnection().getName() + " - " + getSchema().getName() + " - " + operation.name();
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
        return connection.ensure();
    }

    @Override
    public DBLanguagePsiFile createPreviewFile() {
        return null;
    }
}
