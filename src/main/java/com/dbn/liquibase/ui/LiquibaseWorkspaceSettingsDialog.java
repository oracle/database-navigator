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

package com.dbn.liquibase.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.liquibase.model.LiquibaseWorkspace;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

@Getter
public class LiquibaseWorkspaceSettingsDialog extends DBNDialog<LiquibaseWorkspaceSettingsForm> {
    private final LiquibaseWorkspaceBundle workspaces;
    private final LiquibaseWorkspace workspace;
    private final boolean newWorkspace;

    public LiquibaseWorkspaceSettingsDialog(
            LiquibaseWorkspaceBundle workspaces,
            LiquibaseWorkspace workspace,
            boolean newWorkspace) {
        super(workspaces.getProject(), txt("msg.liquibase.title.WorkspaceSettings"), true);
        this.workspaces = workspaces;
        this.workspace = workspace.clone();
        this.newWorkspace = newWorkspace;
        setModal(true);
        init();
    }

    @NotNull
    @Override
    protected LiquibaseWorkspaceSettingsForm createForm() {
        return new LiquibaseWorkspaceSettingsForm(this);
    }

    @Override
    @NotNull
    protected final Action[] initializeActions() {
        renameAction(getOKAction(), txt(newWorkspace ? "msg.liquibase.button.Attach" : "msg.liquibase.button.Update"));
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
