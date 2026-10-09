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

package com.dbn.migration.flyway.operation.ui;

import com.dbn.migration.flyway.operation.FlywayOperation;
import com.dbn.migration.flyway.operation.FlywayOperationCategory;
import com.dbn.migration.shared.operation.ui.DatabaseMigrationOperationDashboardDialog;
import com.dbn.object.DBSchema;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * Non-modal dialog displaying the grouped Flyway operations for a project.
 */
public class FlywayOperationDashboardDialog extends DatabaseMigrationOperationDashboardDialog<
        FlywayOperation,
        FlywayOperationCategory> {

    public FlywayOperationDashboardDialog(@NotNull Project project) {
        this(project, null);
    }

    public FlywayOperationDashboardDialog(@NotNull DBSchema schema) {
        this(schema.getProject(), schema);
    }

    private FlywayOperationDashboardDialog(
            @NotNull Project project,
            @Nullable DBSchema initialSchema) {
        super(project, initialSchema, txt("msg.flyway.title.OperationDashboard"), 640);
    }

    @NotNull
    @Override
    public List<FlywayOperation> getOperations() {
        return Arrays.asList(FlywayOperation.values());
    }

    @NotNull
    @Override
    public List<FlywayOperationCategory> getCategories() {
        return Arrays.asList(FlywayOperationCategory.values());
    }

    @NotNull
    @Override
    public String getDashboardHint() {
        return txt("app.flyway.hint.OperationDashboard");
    }

    @NotNull
    @Override
    public String getDocumentationLabel() {
        return txt("app.flyway.link.FlywayDocumentation");
    }

    @NotNull
    @Override
    public String getDocumentationUrl() {
        return txt("app.flyway.url.OperationDashboard");
    }

    @Override
    public void startOperation(@NotNull FlywayOperation operation) {
        throw new UnsupportedOperationException("Flyway operation is not implemented: " + operation.name());
    }
}
