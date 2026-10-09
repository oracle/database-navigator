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

package com.dbn.migration.liquibase.ui;

import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.context.DatabaseContextSelection;
import com.dbn.migration.liquibase.DatabaseLiquibaseManager;
import com.dbn.migration.liquibase.operation.LiquibaseFeatureSupport;
import com.dbn.migration.liquibase.operation.LiquibaseOperation;
import com.dbn.migration.liquibase.operation.ui.LiquibaseOperationInputDialog;
import com.dbn.migration.liquibase.workflow.LiquibaseWorkflow;
import com.dbn.migration.liquibase.workflow.ui.LiquibaseWorkflowInputDialog;
import com.dbn.migration.liquibase.workspace.LiquibaseWorkspaceBundle;
import com.dbn.migration.shared.operation.DatabaseMigrationOperation;
import com.dbn.migration.shared.task.DatabaseMigrationTask;
import com.dbn.migration.shared.task.ui.DatabaseMigrationTaskStarter;
import com.dbn.migration.shared.workflow.DatabaseMigrationWorkflow;
import com.dbn.object.DBSchema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Dialogs.whenOk;
import static com.dbn.migration.liquibase.operation.LiquibaseFeature.WORKSPACE;
import static com.dbn.migration.liquibase.operation.LiquibaseFeature.WORKSPACE_CREATION;
import static com.dbn.nls.NlsResources.txt;

/**
 * Starts Liquibase operations and workflows after their database context is selected.
 */
public class LiquibaseTaskStarter extends DatabaseMigrationTaskStarter {
    private final DatabaseLiquibaseManager manager;

    public LiquibaseTaskStarter(@NotNull DatabaseLiquibaseManager manager) {
        super(manager.getProject());
        this.manager = manager;
    }

    @Override
    protected void startOperation(
            @NotNull DatabaseMigrationOperation task,
            @NotNull DBSchema schema) {
        LiquibaseOperation operation = ensureOperation(task);
        Dialogs.show(
                () -> new LiquibaseOperationInputDialog(schema, operation, manager.getWorkspaces()),
                whenOk(dialog -> manager.executeOperation(dialog.getExecutionInput(), null)));
    }

    @Override
    protected void startWorkflow(
            @NotNull DatabaseMigrationWorkflow task,
            @NotNull DBSchema schema) {
        LiquibaseWorkflow workflow = ensureWorkflow(task);
        Dialogs.show(
                () -> new LiquibaseWorkflowInputDialog(schema, workflow),
                whenOk(dialog -> manager.executeWorkflow(dialog.getWorkflowInput(), null)));
    }

    @NotNull
    @Override
    protected String getContextTitle(@NotNull DatabaseMigrationTask task) {
        return task instanceof LiquibaseWorkflow
                ? txt("msg.liquibase.title.WorkflowContext")
                : txt("msg.liquibase.title.OperationContext");
    }

    @Nullable
    @Override
    protected String verifyContext(
            @NotNull DatabaseMigrationTask task,
            @NotNull DatabaseContextSelection selection) {
        LiquibaseFeatureSupport support = getSupport(task);
        if (support == null) return null;
        if (!support.requires(WORKSPACE)) return null;
        if (support.supports(WORKSPACE_CREATION)) return null;

        ConnectionId connectionId = selection.connectionId();
        ConnectionHandler connection = getConnection(connectionId);
        if (connection == null) return null;

        DatabaseType databaseType = connection.getDatabaseType();
        LiquibaseWorkspaceBundle workspaces = manager.getWorkspaces();
        if (workspaces.containsWorkspaces(databaseType)) return null;

        String key = task instanceof LiquibaseWorkflow
                ? "msg.liquibase.error.NoWorkspaceAvailableForWorkflow"
                : "msg.liquibase.error.NoWorkspaceAvailableForOperation";
        return txt(key, task.getName());
    }

    @Nullable
    private static LiquibaseFeatureSupport getSupport(@NotNull DatabaseMigrationTask task) {
        if (task instanceof LiquibaseOperation operation) return operation.getSupport();
        if (task instanceof LiquibaseWorkflow workflow) return workflow.getSupport();
        return null;
    }

    @NotNull
    private static LiquibaseOperation ensureOperation(@NotNull DatabaseMigrationOperation task) {
        if (task instanceof LiquibaseOperation operation) return operation;
        throw new IllegalArgumentException("Unsupported Liquibase operation: " + task);
    }

    @NotNull
    private static LiquibaseWorkflow ensureWorkflow(@NotNull DatabaseMigrationWorkflow task) {
        if (task instanceof LiquibaseWorkflow workflow) return workflow;
        throw new IllegalArgumentException("Unsupported Liquibase workflow: " + task);
    }
}
