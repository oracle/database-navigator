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

package com.dbn.migration.shared.workflow.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.migration.shared.engine.DatabaseMigrationEngineType;
import com.dbn.migration.shared.workflow.DatabaseMigrationWorkflow;
import com.dbn.migration.shared.workflow.DatabaseMigrationWorkflowCategory;
import com.dbn.object.DBSchema;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

/**
 * Shared project-level dialog for grouped database migration workflows.
 */
@Getter
public abstract class DatabaseMigrationWorkflowDashboardDialog<
        W extends DatabaseMigrationWorkflow,
        C extends DatabaseMigrationWorkflowCategory>
        extends DBNDialog<DatabaseMigrationWorkflowDashboardForm<W, C>> {
    private final DatabaseMigrationEngineType engineType;
    protected DatabaseMigrationWorkflowDashboardDialog(
            @NotNull Project project,
            @Nullable DBSchema schema,
            @NotNull DatabaseMigrationEngineType engineType,
            @NotNull String title,
            int height) {
        super(project, title, false);
        setContextObject(schema);
        this.engineType = engineType;
        setDefaultSize(640, height);
        setModal(false);
        init();
    }

    @NotNull
    public abstract List<W> getWorkflows();

    @NotNull
    public abstract List<C> getCategories();

    @NotNull
    public abstract String getDashboardHint();

    @NotNull
    public abstract String getDocumentationLabel();

    @NotNull
    public abstract String getDocumentationUrl();

    @NotNull
    @Override
    protected DatabaseMigrationWorkflowDashboardForm<W, C> createForm() {
        return new DatabaseMigrationWorkflowDashboardForm<>(this);
    }

    @Override
    @NotNull
    protected Action[] initializeActions() {
        renameAction(getCancelAction(), txt("msg.shared.button.Close"));
        return actions(getCancelAction());
    }
}
