package com.dbn.liquibase.execution;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.liquibase.model.LiquibaseArtifact;
import com.dbn.liquibase.model.LiquibaseArtifactPaths;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/** Input describing a Liquibase operation for one database connection. */
@Getter
public class LiquibaseExecutionInput {
    private final ConnectionRef connection;
    private final LiquibaseOperation operation;
    private final LiquibaseArtifact artifact;
    private final LiquibaseArtifactPaths artifactPaths;

    public LiquibaseExecutionInput(
            @NotNull ConnectionHandler connection,
            @NotNull LiquibaseOperation operation,
            @NotNull LiquibaseArtifact artifact) {
        this.connection = connection.ref();
        this.operation = operation;
        this.artifact = artifact.clone();
        this.artifactPaths = new LiquibaseArtifactPaths(this.artifact);
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }
}
