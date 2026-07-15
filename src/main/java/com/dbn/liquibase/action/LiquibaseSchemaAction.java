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

package com.dbn.liquibase.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.ui.LiquibaseExecutionInputDialog;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Dialogs.show;
import static com.dbn.common.util.Dialogs.whenOk;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.nls.NlsResources.txt;

/** Base action for Liquibase operations scoped to one database schema. */
public abstract class LiquibaseSchemaAction extends ProjectAction {
    private final DBObjectRef<DBSchema> schema;

    protected LiquibaseSchemaAction(@NotNull DBSchema schema) {
        this.schema = DBObjectRef.of(schema);
    }

    @NotNull
    protected DBSchema getSchema() {
        return DBObjectRef.ensure(schema);
    }

    @NotNull
    protected ConnectionHandler getConnection() {
        return getSchema().getConnection();
    }

    @NotNull
    protected DatabaseLiquibaseManager getManager(@NotNull Project project) {
        return DatabaseLiquibaseManager.getInstance(project);
    }

    protected void executeOperation(
            @NotNull Project project,
            @NotNull LiquibaseOperation operation) {

        DBSchema schema = getSchema();
        DatabaseLiquibaseManager manager = getManager(project);
        LiquibaseWorkspaceBundle workspaces = manager.getWorkspaces();

        if (!operation.getSupport().supportsWorkspaceCreation() &&
                workspaces.getWorkspaces(getConnection().getDatabaseType()).isEmpty()) {
            showErrorDialog(
                    project,
                    txt("msg.liquibase.title.WorkspaceRequired"),
                    txt("msg.liquibase.message.NoWorkspacesAvailable", getConnection().getDatabaseType().getName()),
                    new String[]{
                            txt("msg.liquibase.button.OpenWorkspaces"),
                            txt("msg.shared.button.Cancel")},
                    0,
                    option -> {
                        if (option == 0) manager.openWorkspaceSettings();
                    });
            return;
        }

        show(() -> new LiquibaseExecutionInputDialog(schema, operation, workspaces),
                whenOk(dialog -> {
                    LiquibaseExecutionInput input = dialog.getExecutionInput();
                    manager.executeOperation(input, null);
                }));
    }
}
