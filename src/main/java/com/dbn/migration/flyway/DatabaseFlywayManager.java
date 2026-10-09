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

package com.dbn.migration.flyway;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.Components;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.state.StateCategory;
import com.dbn.common.state.StateContainer;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.DatabaseType;
import com.dbn.migration.flyway.ui.FlywayTaskStarter;
import com.dbn.migration.flyway.workspace.FlywayWorkspace;
import com.dbn.migration.flyway.workspace.FlywayWorkspaceBundle;
import com.dbn.migration.flyway.workspace.ui.FlywayWorkspaceDialog;
import com.dbn.migration.flyway.workspace.ui.FlywayWorkspaceForm;
import com.dbn.migration.shared.engine.DatabaseMigrationEngineType;
import com.dbn.migration.shared.workspace.ui.DatabaseMigrationWorkspacesDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.util.Dialogs.whenOk;

@State(
        name = DatabaseFlywayManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseFlywayManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseFlywayManager";

    private final StateContainer states = new StateContainer();
    private final FlywayWorkspaceBundle workspaces;
    private final FlywayTaskStarter taskStarter;

    private DatabaseFlywayManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        workspaces = new FlywayWorkspaceBundle(project);
        taskStarter = new FlywayTaskStarter(project);
    }

    @NotNull
    public static DatabaseFlywayManager getInstance(@NotNull Project project) {
        return Components.projectService(project, DatabaseFlywayManager.class);
    }

    @NotNull
    public FlywayWorkspaceBundle getWorkspaces() {
        return workspaces;
    }

    @NotNull
    public FlywayTaskStarter getTaskStarter() {
        return taskStarter;
    }

    @NotNull
    public StateAttributes getState(@NonNls @NotNull String category) {
        return states.ensureAttributes(StateCategory.get(category));
    }

    public void openWorkspaceSettings() {
        Dialogs.show(() -> new DatabaseMigrationWorkspacesDialog<>(
                        workspaces,
                        DatabaseMigrationEngineType.FLYWAY,
                        workspaces::clone,
                        FlywayWorkspaceForm::new),
                whenOk(dialog -> workspaces.replaceWorkspaces(dialog.getWorkspaces())));
    }

    public void openWorkspaceCreationDialog(
            @Nullable DatabaseType databaseType,
            @Nullable Consumer<FlywayWorkspace> consumer) {
        FlywayWorkspace workspace = new FlywayWorkspace();
        workspace.setDatabaseType(databaseType);

        Dialogs.show(
                () -> new FlywayWorkspaceDialog(
                        workspaces,
                        workspace,
                        databaseType,
                        true),
                whenOk(dialog -> {
                    if (consumer != null) consumer.accept(dialog.getWorkspace());
                }));
    }

    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        states.writeState(element);
        workspaces.writeState(element, "workspaces");
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        states.readState(element);
        workspaces.readState(element, "workspaces");
    }
}
