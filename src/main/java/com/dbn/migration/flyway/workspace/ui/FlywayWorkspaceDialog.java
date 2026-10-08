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

package com.dbn.migration.flyway.workspace.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.DatabaseType;
import com.dbn.migration.flyway.workspace.FlywayWorkspace;
import com.dbn.migration.flyway.workspace.FlywayWorkspaceBundle;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

import static com.dbn.migration.shared.engine.DatabaseMigrationEngineType.FLYWAY;
import static com.dbn.nls.NlsResources.txt;

@Getter
public class FlywayWorkspaceDialog extends DBNDialog<FlywayWorkspaceForm> {
    private final FlywayWorkspaceBundle workspaces;
    private final FlywayWorkspace workspace;
    private final boolean newWorkspace;
    private final DatabaseType databaseType;

    public FlywayWorkspaceDialog(
            FlywayWorkspaceBundle workspaces,
            FlywayWorkspace workspace,
            @Nullable DatabaseType databaseType,
            boolean newWorkspace) {
        super(workspaces.getProject(), FLYWAY.getWorkspaceTitle(), true);
        this.workspaces = workspaces;
        this.workspace = workspace.clone();
        this.newWorkspace = newWorkspace;
        this.databaseType = databaseType;
        setModal(true);
        init();
    }

    @NotNull
    @Override
    protected FlywayWorkspaceForm createForm() {
        return new FlywayWorkspaceForm(this);
    }

    @Override
    @NotNull
    protected final Action[] initializeActions() {
        String caption = txt(newWorkspace ? "msg.shared.button.Create" : "msg.shared.button.Update");
        renameAction(getOKAction(), caption);
        return actions(getOKAction(), getCancelAction());
    }

    @Override
    public void doCancelAction() {
        if (newWorkspace) {
            workspaces.removeWorkspace(workspace.getId());
        }
        super.doCancelAction();
    }

    @Override
    protected void doOKAction() {
        getForm().applyFormChanges();
        workspaces.replaceWorkspace(workspace);
        super.doOKAction();
    }
}
