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

package com.dbn.liquibase;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.Components;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.state.StateContainer;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.liquibase.model.LiquibaseArtifact;
import com.dbn.liquibase.model.LiquibaseWorkspace;
import com.dbn.liquibase.ui.LiquibaseArtifactSettingsDialog;
import com.dbn.liquibase.ui.LiquibaseWorkspaceSettingsDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.util.Dialogs.whenOk;

@State(
        name = DatabaseLiquibaseManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseLiquibaseManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseLiquibaseManager";

    private final StateContainer states = new StateContainer();
    private final LiquibaseWorkspace workspace;

    private DatabaseLiquibaseManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        workspace = new LiquibaseWorkspace(project);
        ProjectEvents.subscribe(project, this, ConnectionConfigListener.TOPIC,
                ConnectionConfigListener.whenRemoved(workspace::removeArtifact));
    }

    public static DatabaseLiquibaseManager getInstance(@NotNull Project project) {
        return Components.projectService(project, DatabaseLiquibaseManager.class);
    }

    @NotNull
    public LiquibaseWorkspace getWorkspace() {
        return workspace;
    }

    public boolean isWorkspaceAttached(@NotNull ConnectionId connectionId) {
        return workspace.hasArtifact(connectionId);
    }

    public void openArtifactSettings(@NotNull ConnectionHandler connection) {
        boolean newArtifact = !workspace.hasArtifact(connection.getConnectionId());
        LiquibaseArtifact artifact = workspace.ensureArtifact(connection.getConnectionId());
        Dialogs.show(() -> new LiquibaseArtifactSettingsDialog(workspace, artifact, connection, newArtifact));
    }

    public void openWorkspaceSettings() {
        Dialogs.show(() -> new LiquibaseWorkspaceSettingsDialog(workspace),
                whenOk(d -> workspace.replaceArtifacts(d.getWorkspace())));
    }

    public void detachWorkspace(@NotNull ConnectionHandler connection) {
        workspace.removeArtifact(connection.getConnectionId());
    }

    public void generateInitialChangelog(@NotNull ConnectionHandler connection) {
        // TODO
    }

    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        states.writeState(element);
        workspace.writeState(element, "workspace");
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        states.readState(element);
        workspace.readState(element, "workspace");
    }
}
