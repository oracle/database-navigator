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

package com.dbn.migration.flyway.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.migration.flyway.operation.FlywayOperation;
import com.dbn.migration.flyway.workflow.FlywayWorkflow;
import com.dbn.migration.shared.engine.DatabaseMigrationEngineDescriptors;
import com.dbn.migration.shared.engine.DatabaseMigrationEngineType;
import com.dbn.migration.shared.task.ui.DatabaseMigrationTaskStarter;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

/**
 * Base action for Flyway tasks scoped to one database schema.
 */
public abstract class FlywaySchemaAction extends ProjectAction {
    private final DBObjectRef<DBSchema> schema;

    protected FlywaySchemaAction(@NotNull DBSchema schema) {
        this.schema = DBObjectRef.of(schema);
    }

    @NotNull
    protected final DBSchema getSchema() {
        return DBObjectRef.ensure(schema);
    }

    protected final void startOperation(
            @NotNull AnActionEvent event,
            @NotNull Project project,
            @NotNull FlywayOperation operation) {
        DatabaseMigrationTaskStarter taskStarter = getTaskStarter(project);
        JComponent aroundComponent = getContextComponent(event);
        if (aroundComponent == null) return;

        taskStarter.startOperation(operation, getSchema(), aroundComponent, () -> {
        });
    }

    protected final void startWorkflow(
            @NotNull AnActionEvent event,
            @NotNull Project project,
            @NotNull FlywayWorkflow workflow) {
        DatabaseMigrationTaskStarter taskStarter = getTaskStarter(project);
        JComponent aroundComponent = getContextComponent(event);
        if (aroundComponent == null) return;

        taskStarter.startWorkflow(workflow, getSchema(), aroundComponent, () -> {
        });
    }

    @NotNull
    private static DatabaseMigrationTaskStarter getTaskStarter(@NotNull Project project) {
        return DatabaseMigrationEngineDescriptors
                .get(DatabaseMigrationEngineType.FLYWAY)
                .getTaskStarter(project);
    }

    private static JComponent getContextComponent(@NotNull AnActionEvent event) {
        return event.getData(PlatformDataKeys.CONTEXT_COMPONENT) instanceof JComponent component
                ? component
                : null;
    }
}
