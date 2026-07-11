package com.dbn.liquibase.execution;

import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.execution.ExecutionResultBase;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.liquibase.execution.ui.LiquibaseExecutionResultForm;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;

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
    private final List<LiquibaseProcessedItem> processedItems = new ArrayList<>();

    public void addProcessedItem(@NotNull LiquibaseProcessedItem item) {
        processedItems.add(item);
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
