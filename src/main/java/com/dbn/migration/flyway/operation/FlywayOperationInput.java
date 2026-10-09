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

package com.dbn.migration.flyway.operation;

import com.dbn.connection.ConnectionHandler;
import com.dbn.migration.flyway.DatabaseFlywayManager;
import com.dbn.migration.flyway.task.FlywayTaskInput;
import com.dbn.migration.flyway.workspace.FlywayWorkspace;
import com.dbn.migration.flyway.workspace.FlywayWorkspaceBundle;
import com.dbn.migration.flyway.workspace.FlywayWorkspacePaths;
import com.dbn.migration.shared.operation.DatabaseMigrationOperationInput;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Input for a Flyway operation against one selected database schema.
 */
@Getter
public class FlywayOperationInput extends FlywayTaskInput implements DatabaseMigrationOperationInput<FlywayOperation> {
    private final FlywayWorkspaceBundle workspaces;
    private final FlywayOperation operation;
    private DBObjectRef<DBSchema> schema;
    private FlywayWorkspace workspace;
    private FlywayWorkspacePaths workspacePaths;

    public FlywayOperationInput(@NotNull Project project, @NotNull FlywayOperation operation) {
        super(project);
        this.operation = operation;
        workspaces = DatabaseFlywayManager.getInstance(project).getWorkspaces();
    }

    @Nullable
    public DBSchema getSchema() {
        return DBObjectRef.get(schema);
    }

    public void setSchema(@Nullable DBSchema schema) {
        this.schema = DBObjectRef.of(schema);
    }

    @NotNull
    public FlywayFeatureSupport getSupport() {
        return operation.getSupport();
    }

    @NotNull
    @Override
    public ConnectionHandler getRelevantConnection() {
        DBSchema schema = getRelevantSchema();
        return schema.getConnection();
    }

    @NotNull
    @Override
    public DBSchema getRelevantSchema() {
        DBSchema schema = getSchema();
        if (schema == null) throw new IllegalStateException("No schema available");
        return schema;
    }

    public void setWorkspace(@Nullable FlywayWorkspace workspace) {
        this.workspace = workspace == null ? null : workspace.clone();
        this.workspacePaths = this.workspace == null ? null : new FlywayWorkspacePaths(this.workspace);
    }

    public FlywayOperationInput copyFrom(@NotNull FlywayOperationInput input) {
        setSchema(input.getSchema());
        setWorkspace(input.getWorkspace());
        return this;
    }
}
