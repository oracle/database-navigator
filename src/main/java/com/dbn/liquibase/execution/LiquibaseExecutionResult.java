package com.dbn.liquibase.execution;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.util.Listeners;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.execution.ExecutionResultBase;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.liquibase.execution.ui.LiquibaseExecutionResultForm;
import com.intellij.openapi.project.Project;
import liquibase.structure.DatabaseObject;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Execution-console result for a Liquibase operation and its console output. */
@Getter
public class LiquibaseExecutionResult extends ExecutionResultBase<LiquibaseExecutionResultForm> {
    private final ConnectionRef connection;
    private final LiquibaseOperation operation;
    private String consoleOutput = "";
    private String errorOutput = "";
    private boolean successful;
    private long startTime;
    private long endTime;
    private final Map<String, LiquibaseProcessedItem> processedItems = new LinkedHashMap<>();
    private final Listeners<Runnable> listeners = Listeners.create(this);

    @NotNull
    public List<LiquibaseProcessedItem> getProcessedItems() {
        synchronized (processedItems) {
            return new ArrayList<>(processedItems.values());
        }
    }

    @NotNull
    public LiquibaseProcessedItem ensureProcessedItem(@NotNull DatabaseObject databaseObject) {
        String key = getDatabaseObjectKey(databaseObject);
        LiquibaseProcessedItem item;
        boolean created = false;
        synchronized (processedItems) {
            item = processedItems.get(key);
            if (item == null) {
                item = new LiquibaseProcessedItem(databaseObject);
                processedItems.put(key, item);
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

    public void updateProcessedItem(
            @NotNull LiquibaseProcessedItem item,
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

    public LiquibaseExecutionResult(@NotNull ConnectionHandler connection, @NotNull LiquibaseOperation operation) {
        this.connection = connection.ref();
        this.operation = operation;
    }

    public void start() {
        startTime = System.currentTimeMillis();
    }

    public void finish(boolean successful) {
        this.successful = successful;
        endTime = System.currentTimeMillis();
    }

    public void appendConsoleOutput(@Nullable String output) {
        if (output != null) consoleOutput += output;
    }

    public void appendErrorOutput(@Nullable String output) {
        if (output != null) errorOutput += output;
    }

    @Nullable
    @Override
    public LiquibaseExecutionResultForm createForm() {
        return new LiquibaseExecutionResultForm(this);
    }

    @NotNull
    @Override
    public String getName() {
        return getConnection().getName() + " - " + operation.name();
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
