package com.dbn.liquibase.execution;

import com.dbn.common.component.ProjectUnit;
import com.dbn.common.locale.Formatter;
import com.dbn.connection.ConnectionHandler;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.model.LiquibaseWorkspace;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;

import static com.dbn.common.util.Commons.coalesce;
import static com.dbn.liquibase.execution.LiquibaseCommands.ROLLBACK_COUNT;
import static com.dbn.liquibase.execution.LiquibaseCommands.ROLLBACK_DATE;
import static com.dbn.liquibase.execution.LiquibaseCommands.ROLLBACK_TAG;

/** Input describing a Liquibase operation and its optional source and target schemas. */
@Getter
@Setter
public class LiquibaseExecutionInput extends ProjectUnit {
    private final LiquibaseWorkspaceBundle workspaces;
    private final LiquibaseOperation operation;

    private DBObjectRef<DBSchema> sourceSchema;
    private DBObjectRef<DBSchema> targetSchema;
    private LiquibaseRollbackType rollbackType = LiquibaseRollbackType.COUNT;
    private int rollbackCount = 1;
    private String rollbackTag;
    private String rollbackDate;

    private LiquibaseWorkspace workspace;
    private LiquibaseWorkspacePaths workspacePaths;

    public LiquibaseExecutionInput(@NotNull Project project, @NotNull LiquibaseOperation operation) {
        super(project);

        DatabaseLiquibaseManager liquibaseManager = DatabaseLiquibaseManager.getInstance(project);
        this.workspaces = liquibaseManager.getWorkspaces();
        this.operation = operation;
    }

    @Nullable
    public DBSchema getSourceSchema() {
        return DBObjectRef.get(sourceSchema);
    }

    @Nullable
    public DBSchema getTargetSchema() {
        return DBObjectRef.get(targetSchema);
    }

    @Nullable
    public ConnectionHandler getSourceConnection() {
        DBSchema sourceSchema = getSourceSchema();
        return sourceSchema == null ? null : sourceSchema.getConnection();
    }

    @Nullable
    public ConnectionHandler getTargetConnection() {
        DBSchema targetSchema = getTargetSchema();
        return targetSchema == null ? null : targetSchema.getConnection();
    }

    @NotNull
    public ConnectionHandler getRelevantConnection() {
        ConnectionHandler connection = operation.requiresSourceSchema()
                ? coalesce(() -> getSourceConnection(), () -> getTargetConnection())
                : coalesce(() -> getTargetConnection(), () -> getSourceConnection());

        if (connection == null) throw new IllegalStateException("No connection available");
        return connection;
    }

    @NotNull
    public DBSchema getRelevantSchema() {
        DBSchema schema = operation.requiresSourceSchema()
                ? coalesce(() -> getSourceSchema(), () -> getTargetSchema())
                : coalesce(() -> getTargetSchema(), () -> getSourceSchema());

        if (schema == null) throw new IllegalStateException("No schema available");
        return schema;
    }

    public void setSourceSchema(@Nullable DBSchema sourceSchema) {
        this.sourceSchema = DBObjectRef.of(sourceSchema);
    }

    public void setTargetSchema(@Nullable DBSchema targetSchema) {
        this.targetSchema = DBObjectRef.of(targetSchema);
    }

    @NotNull
    public LiquibaseRollbackInstruction getRollbackInstruction() throws ParseException {
        return switch (rollbackType) {
            case COUNT -> new LiquibaseRollbackInstruction(ROLLBACK_COUNT, "count", rollbackCount);
            case TAG -> new LiquibaseRollbackInstruction(ROLLBACK_TAG, "tag", rollbackTag);
            case DATE -> new LiquibaseRollbackInstruction(
                    ROLLBACK_DATE,
                    "date",
                    Formatter.getInstance(getProject()).parseDateTime(rollbackDate));
        };
    }

    public void setWorkspace(@Nullable LiquibaseWorkspace workspace) {
        this.workspace = workspace == null ? null : workspace.clone();
        this.workspacePaths = this.workspace == null ? null : new LiquibaseWorkspacePaths(this.workspace);
    }
}
