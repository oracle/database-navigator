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

package com.dbn.migration.liquibase.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.migration.liquibase.DatabaseLiquibaseManager;
import com.dbn.migration.liquibase.operation.LiquibaseFeatureSupport;
import com.dbn.migration.liquibase.operation.LiquibaseOperation;
import com.dbn.migration.liquibase.operation.LiquibaseOperationConfirmations;
import com.dbn.migration.liquibase.operation.LiquibaseOperationInput;
import com.dbn.migration.liquibase.operation.ui.LiquibaseOperationInputDialog;
import com.dbn.migration.liquibase.workspace.LiquibaseWorkspaceBundle;
import com.dbn.migration.shared.task.DatabaseMigrationTask;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Dialogs.show;
import static com.dbn.common.util.Dialogs.whenOk;

/**
 * Base action for Liquibase operations scoped to one database schema.
 */
public abstract class LiquibaseSchemaAction extends ProjectAction {
    private final DBObjectRef<DBSchema> schema;

    protected LiquibaseSchemaAction(@NotNull DBSchema schema) {
        this.schema = DBObjectRef.of(schema);
    }

    @NotNull
    protected DBSchema getSchema() {
        return DBObjectRef.ensure(schema);
    }

    @NotNull
    protected DatabaseLiquibaseManager getManager(@NotNull Project project) {
        return DatabaseLiquibaseManager.getInstance(project);
    }

    protected boolean confirmWorkspaceAvailable(
            @NotNull DBSchema schema,
            @NotNull DatabaseMigrationTask task,
            @NotNull LiquibaseFeatureSupport support) {
        return LiquibaseOperationConfirmations.confirmWorkspaceAvailable(
                schema.getConnection(),
                task,
                support);
    }

    protected void executeOperation(
            @NotNull Project project,
            @NotNull LiquibaseOperation operation) {

        DBSchema schema = getSchema();
        if (!confirmWorkspaceAvailable(schema, operation, operation.getSupport())) return;

        DatabaseLiquibaseManager manager = getManager(project);
        LiquibaseWorkspaceBundle workspaces = manager.getWorkspaces();

        show(() -> new LiquibaseOperationInputDialog(schema, operation, workspaces),
                whenOk(dialog -> {
                    LiquibaseOperationInput input = dialog.getExecutionInput();
                    manager.executeOperation(input, null);
                }));
    }

}
