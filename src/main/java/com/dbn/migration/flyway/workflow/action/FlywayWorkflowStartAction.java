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

package com.dbn.migration.flyway.workflow.action;

import com.dbn.migration.flyway.action.FlywaySchemaAction;
import com.dbn.migration.flyway.workflow.FlywayWorkflow;
import com.dbn.object.DBSchema;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point for one Flyway workflow scoped to a database schema.
 */
public class FlywayWorkflowStartAction extends FlywaySchemaAction {
    private final FlywayWorkflow workflow;

    public FlywayWorkflowStartAction(
            @NotNull DBSchema schema,
            @NotNull FlywayWorkflow workflow) {
        super(schema);
        this.workflow = workflow;
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent event, @NotNull Project project) {
        startWorkflow(event, project, workflow);
    }

    @Override
    protected void update(@NotNull AnActionEvent event, @NotNull Project project) {
        Presentation presentation = event.getPresentation();
        presentation.setText(workflow.getDashboardName());
        presentation.setDescription(workflow.getDescription());
    }
}
