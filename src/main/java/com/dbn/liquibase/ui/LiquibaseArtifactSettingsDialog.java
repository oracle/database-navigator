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
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.model.LiquibaseArtifact;
import com.dbn.liquibase.model.LiquibaseWorkspace;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

@Getter
public class LiquibaseArtifactSettingsDialog extends DBNDialog<LiquibaseArtifactSettingsForm> {
    private final LiquibaseWorkspace workspace;
    private final LiquibaseArtifact artifact;
    private final ConnectionRef connection;
    private final boolean newArtifact;

    public LiquibaseArtifactSettingsDialog(LiquibaseWorkspace workspace, LiquibaseArtifact artifact, ConnectionHandler connection, boolean newArtifact) {
        super(connection.getProject(), txt("msg.liquibase.title.WorkspaceSettings"), true);
        this.workspace = workspace;
        this.artifact = artifact;
        this.connection = connection.ref();
        this.newArtifact = newArtifact;
        setModal(true);
        init();
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @NotNull
    @Override
    protected LiquibaseArtifactSettingsForm createForm() {
        return new LiquibaseArtifactSettingsForm(this);
    }

    @Override
    @NotNull
    protected final Action[] initializeActions() {
        renameAction(getOKAction(), txt(newArtifact ? "msg.liquibase.button.Attach" : "msg.liquibase.button.Update"));
        return actions(getOKAction(), getCancelAction());
    }

    @Override
    public void doCancelAction() {
        if (newArtifact) {
            DatabaseLiquibaseManager.getInstance(getProject()).detachWorkspace(getConnection());
        }
        close(0);
    }

    @Override
    protected void doOKAction() {
        getForm().applyFormChanges();
        super.doOKAction();
    }
}
