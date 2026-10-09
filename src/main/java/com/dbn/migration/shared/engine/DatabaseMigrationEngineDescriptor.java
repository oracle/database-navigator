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

package com.dbn.migration.shared.engine;

import com.dbn.common.extension.ExtensionPoint;
import com.dbn.common.state.StateAttributes;
import com.dbn.migration.shared.operation.DatabaseMigrationOperation;
import com.dbn.migration.shared.task.ui.DatabaseMigrationTaskStarter;
import com.dbn.migration.shared.workflow.DatabaseMigrationWorkflow;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Describes a database migration engine without exposing its execution implementation.
 *
 * <p>The descriptor provides a stable identity for engine-neutral code, presentation
 * metadata for shared dashboards, the ordered operation and workflow catalogues exposed by
 * the engine, the bridge to the engine-specific task starter, and access to manager-backed
 * state used by shared migration screens.</p>
 */
public abstract class DatabaseMigrationEngineDescriptor<
        O extends DatabaseMigrationOperation,
        W extends DatabaseMigrationWorkflow> implements ExtensionPoint {
    public static final ExtensionPointName<DatabaseMigrationEngineDescriptor<?, ?>> EP =
            ExtensionPointName.create("com.dbn.databaseMigrationEngine");

    private final DatabaseMigrationEngineType engineType;
    private final List<O> operations;
    private final List<W> workflows;

    protected DatabaseMigrationEngineDescriptor(
            @NotNull DatabaseMigrationEngineType engineType,
            @NotNull List<O> operations,
            @NotNull List<W> workflows) {
        this.engineType = engineType;
        this.operations = List.copyOf(operations);
        this.workflows = List.copyOf(workflows);
    }

    @NotNull
    public final DatabaseMigrationEngineType getEngineType() {
        return engineType;
    }

    @NotNull
    public abstract DatabaseMigrationTaskStarter getTaskStarter(@NotNull Project project);

    @NotNull
    public abstract StateAttributes getState(
            @NotNull Project project,
            @NotNull String category);

    @NotNull
    public abstract String getName();

    /**
     * Returns the operations exposed by this engine in their preferred presentation order.
     *
     * <p>The returned operation types may be engine-specific. Shared code should rely only
     * on the {@link DatabaseMigrationOperation} contract when it does not know the engine.</p>
     */
    @NotNull
    public final List<O> getOperations() {
        return operations;
    }

    /**
     * Returns the workflows exposed by this engine in their preferred presentation order.
     *
     * <p>The returned workflow types may be engine-specific. Shared code should rely only
     * on the {@link DatabaseMigrationWorkflow} contract when it does not know the engine.</p>
     */
    @NotNull
    public final List<W> getWorkflows() {
        return workflows;
    }

    public boolean supports(@NotNull O operation) {
        return getOperations().contains(operation);
    }
}
