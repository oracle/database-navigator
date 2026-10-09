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

package com.dbn.migration.shared.task.ui;

import com.dbn.common.component.ProjectUnit;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.context.DatabaseContextSelection;
import com.dbn.connection.context.ui.DatabaseContextSelector;
import com.dbn.connection.context.ui.DatabaseContextSelectorInput;
import com.dbn.migration.shared.operation.DatabaseMigrationOperation;
import com.dbn.migration.shared.task.DatabaseMigrationTask;
import com.dbn.migration.shared.workflow.DatabaseMigrationWorkflow;
import com.dbn.object.DBSchema;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.function.Consumer;

/**
 * Starts database migration operations and workflows after selecting their context.
 *
 * <p>Engine implementations supply task-specific captions, validation, and input
 * dialogs. The common starter owns context selection and returns control to the
 * dashboard only after a valid context has been selected.</p>
 */
public abstract class DatabaseMigrationTaskStarter extends ProjectUnit {
    protected DatabaseMigrationTaskStarter(@NotNull Project project) {
        super(project);
    }

    public final void startOperation(
            @NotNull DatabaseMigrationOperation operation,
            @Nullable DBSchema initialSchema,
            @NotNull JComponent aroundComponent,
            @NotNull Runnable onContextSelected) {
        startTask(operation, initialSchema, aroundComponent, schema -> {
            onContextSelected.run();
            startOperation(operation, schema);
        });
    }

    public final void startWorkflow(
            @NotNull DatabaseMigrationWorkflow workflow,
            @Nullable DBSchema initialSchema,
            @NotNull JComponent aroundComponent,
            @NotNull Runnable onContextSelected) {
        startTask(workflow, initialSchema, aroundComponent, schema -> {
            onContextSelected.run();
            startWorkflow(workflow, schema);
        });
    }

    protected abstract void startOperation(
            @NotNull DatabaseMigrationOperation operation,
            @NotNull DBSchema schema);

    protected abstract void startWorkflow(
            @NotNull DatabaseMigrationWorkflow workflow,
            @NotNull DBSchema schema);

    @NotNull
    protected abstract String getContextTitle(@NotNull DatabaseMigrationTask task);

    @Nullable
    protected abstract String verifyContext(
            @NotNull DatabaseMigrationTask task,
            @NotNull DatabaseContextSelection selection);

    private void startTask(
            @NotNull DatabaseMigrationTask task,
            @Nullable DBSchema initialSchema,
            @NotNull JComponent aroundComponent,
            @NotNull Consumer<DBSchema> callback) {
        DatabaseContextSelectorInput input = new DatabaseContextSelectorInput()
                .withTitle(getContextTitle(task))
                .withInitialSelection(toSelection(initialSchema))
                .withVerification(selection -> verifyContext(task, selection))
                .withContinueCallback(selection -> {
                    DBSchema schema = resolveSchema(selection);
                    if (schema != null) callback.accept(schema);
                });

        DatabaseContextSelector.create(getProject(), input).showPopup(aroundComponent);
    }

    @Nullable
    private DatabaseContextSelection toSelection(@Nullable DBSchema schema) {
        return schema == null ? null : new DatabaseContextSelection(
                schema.getConnectionId(),
                schema.getSchemaId(),
                null);
    }

    @Nullable
    private DBSchema resolveSchema(@NotNull DatabaseContextSelection selection) {
        ConnectionHandler connection = ConnectionManager.getInstance(getProject())
                .getConnection(selection.connectionId());
        return connection == null ? null : connection.getSchema(selection.schemaId());
    }
}
