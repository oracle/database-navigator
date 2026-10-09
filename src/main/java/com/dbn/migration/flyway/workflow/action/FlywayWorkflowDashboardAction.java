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

import com.dbn.common.action.ProjectAction;
import com.dbn.migration.flyway.workflow.ui.FlywayWorkflowDashboardDialog;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Dialogs.show;
import static com.dbn.nls.NlsResources.txt;

/**
 * Opens the Flyway workflow dashboard from a database schema context.
 */
public class FlywayWorkflowDashboardAction extends ProjectAction {
    private final DBObjectRef<DBSchema> schema;

    public FlywayWorkflowDashboardAction(@NotNull DBSchema schema) {
        super(txt("app.flyway.action.WorkflowDashboard"), null, AllIcons.Actions.ListFiles);
        this.schema = DBObjectRef.of(schema);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        show(() -> new FlywayWorkflowDashboardDialog(DBObjectRef.ensure(schema)));
    }
}
