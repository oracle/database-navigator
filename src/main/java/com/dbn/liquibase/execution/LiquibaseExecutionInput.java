package com.dbn.liquibase.execution;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.liquibase.model.LiquibaseWorkspace;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/** Input describing a Liquibase operation for one database schema. */
@Getter
public class LiquibaseExecutionInput {
    private final DBObjectRef<DBSchema> schema;
    private final ConnectionRef connection;
    private final LiquibaseOperation operation;
    private final LiquibaseWorkspace workspace;
    private final LiquibaseWorkspacePaths workspacePaths;

    public LiquibaseExecutionInput(
            @NotNull DBSchema schema,
            @NotNull LiquibaseOperation operation,
            @NotNull LiquibaseWorkspace workspace) {
        this.schema = DBObjectRef.of(schema);
        this.connection = schema.getConnection().ref();
        this.operation = operation;
        this.workspace = workspace.clone();
        this.workspacePaths = new LiquibaseWorkspacePaths(this.workspace);
    }

    @NotNull
    public DBSchema getSchema() {
        return DBObjectRef.ensure(schema);
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }
}
