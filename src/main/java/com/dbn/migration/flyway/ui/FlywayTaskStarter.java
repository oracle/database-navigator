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

import com.dbn.connection.context.DatabaseContextSelection;
import com.dbn.migration.flyway.operation.FlywayOperation;
import com.dbn.migration.flyway.workflow.FlywayWorkflow;
import com.dbn.migration.shared.operation.DatabaseMigrationOperation;
import com.dbn.migration.shared.task.DatabaseMigrationTask;
import com.dbn.migration.shared.task.ui.DatabaseMigrationTaskStarter;
import com.dbn.migration.shared.workflow.DatabaseMigrationWorkflow;
import com.dbn.object.DBSchema;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

/**
 * Starts Flyway operations and workflows after their database context is selected.
 */
public class FlywayTaskStarter extends DatabaseMigrationTaskStarter {
    public FlywayTaskStarter(@NotNull Project project) {
        super(project);
    }

    @Override
    protected void startOperation(
            @NotNull DatabaseMigrationOperation task,
            @NotNull DBSchema schema) {
        FlywayOperation operation = ensureOperation(task);
        throw new UnsupportedOperationException("Flyway operation is not implemented: " + operation.name());
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
        return null;
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
}
