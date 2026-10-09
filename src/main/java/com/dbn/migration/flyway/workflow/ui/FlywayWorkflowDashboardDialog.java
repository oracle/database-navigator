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

package com.dbn.migration.flyway.workflow.ui;

import com.dbn.migration.flyway.workflow.FlywayWorkflow;
import com.dbn.migration.flyway.workflow.FlywayWorkflowCategory;
import com.dbn.migration.shared.workflow.ui.DatabaseMigrationWorkflowDashboardDialog;
import com.dbn.object.DBSchema;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * Non-modal dialog displaying the grouped Flyway workflows for a project.
 */
public class FlywayWorkflowDashboardDialog extends DatabaseMigrationWorkflowDashboardDialog<
        FlywayWorkflow,
        FlywayWorkflowCategory> {

    public FlywayWorkflowDashboardDialog(@NotNull Project project) {
        this(project, null);
    }

    public FlywayWorkflowDashboardDialog(@NotNull DBSchema schema) {
        this(schema.getProject(), schema);
    }

    private FlywayWorkflowDashboardDialog(
            @NotNull Project project,
            @Nullable DBSchema initialSchema) {
        super(project, initialSchema, txt("msg.flyway.title.WorkflowDashboard"), 640);
    }

    @NotNull
    @Override
    public List<FlywayWorkflow> getWorkflows() {
        return Arrays.asList(FlywayWorkflow.values());
    }

    @NotNull
    @Override
    public List<FlywayWorkflowCategory> getCategories() {
        return Arrays.asList(FlywayWorkflowCategory.values());
    }

    @NotNull
    @Override
    public String getDashboardHint() {
        return txt("app.flyway.hint.Workflows");
    }

    @NotNull
    @Override
    public String getDocumentationLabel() {
        return txt("app.flyway.link.FlywayDocumentation");
    }

    @NotNull
    @Override
    public String getDocumentationUrl() {
        return txt("app.flyway.url.WorkflowDashboard");
    }

    @Override
    public void startWorkflow(@NotNull FlywayWorkflow workflow) {
        throw new UnsupportedOperationException("Flyway workflow is not implemented: " + workflow.name());
    }
}
