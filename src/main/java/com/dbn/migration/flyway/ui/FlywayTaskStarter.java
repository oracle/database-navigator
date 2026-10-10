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

package com.dbn.migration.flyway.ui;

import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.context.DatabaseContextSelection;
import com.dbn.migration.flyway.DatabaseFlywayManager;
import com.dbn.migration.flyway.execution.processor.FlywayExecutionProcessors;
import com.dbn.migration.flyway.operation.FlywayFeatureSupport;
import com.dbn.migration.flyway.operation.FlywayOperation;
import com.dbn.migration.flyway.operation.ui.FlywayOperationInputDialog;
import com.dbn.migration.flyway.workflow.FlywayWorkflow;
import com.dbn.migration.flyway.workspace.FlywayWorkspaceBundle;
import com.dbn.migration.shared.operation.DatabaseMigrationOperation;
import com.dbn.migration.shared.task.DatabaseMigrationTask;
import com.dbn.migration.shared.task.ui.DatabaseMigrationTaskStarter;
import com.dbn.migration.shared.workflow.DatabaseMigrationWorkflow;
import com.dbn.object.DBSchema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Dialogs.whenOk;
import static com.dbn.migration.flyway.operation.FlywayFeature.WORKSPACE;
import static com.dbn.migration.flyway.operation.FlywayFeature.WORKSPACE_CREATION;
import static com.dbn.nls.NlsResources.txt;

/**
 * Starts Flyway operations and workflows after their database context is selected.
 */
public class FlywayTaskStarter extends DatabaseMigrationTaskStarter {
    private final DatabaseFlywayManager manager;

    public FlywayTaskStarter(@NotNull DatabaseFlywayManager manager) {
        super(manager.getProject());
        this.manager = manager;
    }

    @Override
    protected void startOperation(
            @NotNull DatabaseMigrationOperation task,
            @NotNull DBSchema schema) {
        FlywayOperation operation = ensureOperation(task);
        FlywayExecutionProcessors.get(operation);

        Dialogs.show(
                () -> new FlywayOperationInputDialog(schema, operation, manager.getWorkspaces()),
                whenOk(dialog -> manager.executeOperation(dialog.getExecutionInput(), null)));
    }

    @Override
    protected void startWorkflow(
            @NotNull DatabaseMigrationWorkflow task,
            @NotNull DBSchema schema) {
        FlywayWorkflow workflow = ensureWorkflow(task);
        throw new UnsupportedOperationException("Flyway workflow is not implemented: " + workflow.name());
    }

    @NotNull
    @Override
    protected String getContextTitle(@NotNull DatabaseMigrationTask task) {
        return task instanceof FlywayWorkflow
                ? txt("msg.flyway.title.WorkflowContext")
                : txt("msg.flyway.title.OperationContext");
    }

    @Nullable
    @Override
    protected String verifyContext(
            @NotNull DatabaseMigrationTask task,
            @NotNull DatabaseContextSelection selection) {
        FlywayFeatureSupport support = getSupport(task);
        if (support == null) return null;
        if (!support.requires(WORKSPACE)) return null;
        if (support.supports(WORKSPACE_CREATION)) return null;

        ConnectionHandler connection = getConnection(selection.connectionId());
        if (connection == null) return null;

        DatabaseType databaseType = connection.getDatabaseType();
        FlywayWorkspaceBundle workspaces = manager.getWorkspaces();
        if (workspaces.containsWorkspaces(databaseType)) return null;

        return txt("msg.flyway.error.NoWorkspaceAvailableForOperation", task.getName());
    }

    @NotNull
    private static FlywayOperation ensureOperation(@NotNull DatabaseMigrationOperation task) {
        if (task instanceof FlywayOperation operation) return operation;
        throw new IllegalArgumentException("Unsupported Flyway operation: " + task);
    }

    @NotNull
    private static FlywayWorkflow ensureWorkflow(@NotNull DatabaseMigrationWorkflow task) {
        if (task instanceof FlywayWorkflow workflow) return workflow;
        throw new IllegalArgumentException("Unsupported Flyway workflow: " + task);
    }

    @Nullable
    private static FlywayFeatureSupport getSupport(@NotNull DatabaseMigrationTask task) {
        if (task instanceof FlywayOperation operation) return operation.getSupport();
        return null;
    }
}
