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

package com.dbn.migration.liquibase.operation.ui;

import com.dbn.migration.liquibase.operation.LiquibaseOperation;
import com.dbn.migration.liquibase.operation.LiquibaseOperationCategory;
import com.dbn.migration.shared.engine.DatabaseMigrationEngineType;
import com.dbn.migration.shared.operation.ui.DatabaseMigrationOperationDashboardDialog;
import com.dbn.object.DBSchema;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * Non-modal dialog displaying the grouped Liquibase operations for a project.
 */
public class LiquibaseOperationDashboardDialog extends DatabaseMigrationOperationDashboardDialog<
        LiquibaseOperation,
        LiquibaseOperationCategory> {
    public LiquibaseOperationDashboardDialog(@NotNull Project project) {
        this(project, null);
    }

    public LiquibaseOperationDashboardDialog(@NotNull DBSchema schema) {
        this(schema.getProject(), schema);
    }

    private LiquibaseOperationDashboardDialog(@NotNull Project project, @Nullable DBSchema schema) {
        super(project, schema,
                DatabaseMigrationEngineType.LIQUIBASE,
                txt("msg.liquibase.title.OperationDashboard"), 860);
    }

    @NotNull
    @Override
    public List<LiquibaseOperation> getOperations() {
        return Arrays.asList(LiquibaseOperation.values());
    }

    @NotNull
    @Override
    public List<LiquibaseOperationCategory> getCategories() {
        return Arrays.asList(LiquibaseOperationCategory.values());
    }

    @NotNull
    @Override
    public String getDashboardHint() {
        return txt("app.liquibase.hint.OperationDashboard");
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
