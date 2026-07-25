/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.liquibase.operation;

import com.dbn.common.environment.EnvironmentTypeId;
import com.dbn.connection.ConnectionHandler;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.task.LiquibaseTaskInput;
import com.dbn.liquibase.workspace.LiquibaseEnvironmentProfile;
import com.dbn.liquibase.workspace.LiquibaseEnvironmentProfileBundle;
import com.dbn.liquibase.workspace.LiquibaseWorkspace;
import com.dbn.liquibase.workspace.LiquibaseWorkspaceBundle;
import com.dbn.liquibase.workspace.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Commons.coalesce;
import static com.dbn.liquibase.operation.LiquibaseFeature.DISTINCT_SCHEMAS;
import static com.dbn.liquibase.operation.LiquibaseFeature.SOURCE_SCHEMA;

/** Input describing a Liquibase operation and its optional source and target schemas. */
@Getter
@Setter
public class LiquibaseOperationInput extends LiquibaseTaskInput {
    private final LiquibaseWorkspaceBundle workspaces;
    private final LiquibaseEnvironmentProfileBundle environmentProfiles;
    private final LiquibaseOperation operation;

    private DBObjectRef<DBSchema> sourceSchema;
    private DBObjectRef<DBSchema> targetSchema;
    private final LiquibaseRollbackInstruction rollbackInstruction = new LiquibaseRollbackInstruction();
    private final LiquibaseUpdateInstruction updateInstruction = new LiquibaseUpdateInstruction();
    private String changelogAuthor;
    private String databaseTag;
    private String changelogTag;
    private String checkpointTag;
    private boolean confirmed;

    private LiquibaseWorkspace workspace;
    private LiquibaseWorkspacePaths workspacePaths;

    public LiquibaseOperationInput(@NotNull Project project, @NotNull LiquibaseOperation operation) {
        super(project);

        DatabaseLiquibaseManager liquibaseManager = DatabaseLiquibaseManager.getInstance(project);
        this.workspaces = liquibaseManager.getWorkspaces();
        this.environmentProfiles = liquibaseManager.getEnvironmentProfiles();
        this.operation = operation;
    }

    @NotNull
    public LiquibaseFeatureSupport getSupport() {
        return operation.getSupport();
    }

    public boolean containsOperation(@NotNull LiquibaseOperation operation) {
        return this.operation == operation;
    }

    @NotNull
    public String getHint() {
        return operation.getHint();
    }

    @NotNull
    public String getDocumentationUrl() {
        return operation.getDocumentationUrl();
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
    public DBSchema getExcludedTargetSchema() {
        return getSupport().supports(DISTINCT_SCHEMAS) ? getSourceSchema() : null;
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
        ConnectionHandler connection = getOperation().requires(SOURCE_SCHEMA)
                ? coalesce(() -> getSourceConnection(), () -> getTargetConnection())
                : coalesce(() -> getTargetConnection(), () -> getSourceConnection());

        if (connection == null) throw new IllegalStateException("No connection available");
        return connection;
    }

    @NotNull
    public EnvironmentTypeId getEnvironmentTypeId() {
        return getRelevantConnection().getEnvironmentType().getId();
    }

    @NotNull
    public LiquibaseEnvironmentProfile getEnvironmentProfile() {
        return environmentProfiles.getProfile(getEnvironmentTypeId());
    }

    @NotNull
    public DBSchema getRelevantSchema() {
        DBSchema schema = getOperation().requires(SOURCE_SCHEMA)
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

    public void setWorkspace(@Nullable LiquibaseWorkspace workspace) {
        this.workspace = workspace == null ? null : workspace.clone();
        this.workspacePaths = this.workspace == null ? null : new LiquibaseWorkspacePaths(this.workspace);
    }

    public LiquibaseOperationInput copyFrom(@NotNull LiquibaseOperationInput input) {
        setSourceSchema(input.getSourceSchema());
        setTargetSchema(input.getTargetSchema());
        setWorkspace(input.getWorkspace());
        rollbackInstruction.copyFrom(input.getRollbackInstruction());
        updateInstruction.copyFrom(input.getUpdateInstruction());
        changelogAuthor = input.getChangelogAuthor();
        databaseTag = input.getDatabaseTag();
        changelogTag = input.getChangelogTag();
        checkpointTag = input.getCheckpointTag();
        confirmed = input.isConfirmed();
        return this;
    }
}
