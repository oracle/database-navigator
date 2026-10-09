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

package com.dbn.migration.liquibase.workflow.ui;

import com.dbn.migration.liquibase.workflow.LiquibaseWorkflow;
import com.dbn.migration.liquibase.workflow.LiquibaseWorkflowCategory;
import com.dbn.migration.shared.engine.DatabaseMigrationEngineType;
import com.dbn.migration.shared.workflow.ui.DatabaseMigrationWorkflowDashboardDialog;
import com.dbn.object.DBSchema;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * Non-modal dialog displaying the grouped Liquibase workflows for a project.
 */
public class LiquibaseWorkflowDashboardDialog extends DatabaseMigrationWorkflowDashboardDialog<
        LiquibaseWorkflow,
        LiquibaseWorkflowCategory> {
    public LiquibaseWorkflowDashboardDialog(@NotNull Project project) {
        this(project, null);
    }

    public LiquibaseWorkflowDashboardDialog(@NotNull DBSchema schema) {
        this(schema.getProject(), schema);
    }

    private LiquibaseWorkflowDashboardDialog(@NotNull Project project, @Nullable DBSchema schema) {
        super(project, schema,
                DatabaseMigrationEngineType.LIQUIBASE,
                txt("msg.liquibase.title.WorkflowDashboard"), 720);
    }

    @NotNull
    @Override
    public List<LiquibaseWorkflow> getWorkflows() {
        return Arrays.asList(LiquibaseWorkflow.values());
    }

    @NotNull
    @Override
    public List<LiquibaseWorkflowCategory> getCategories() {
        return Arrays.asList(LiquibaseWorkflowCategory.values());
    }

    @NotNull
    @Override
    public String getDashboardHint() {
        return txt("app.liquibase.hint.Workflows");
    }

    @NotNull
    @Override
    public String getDocumentationLabel() {
        return txt("app.liquibase.link.LiquibaseDocumentation");
    }

    @NotNull
    @Override
    public String getDocumentationUrl() {
        return txt("app.liquibase.url.OperationDashboard");
    }

}
