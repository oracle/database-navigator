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
import com.dbn.common.thread.Background;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.execution.ExecutionManager;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.processor.LiquibaseExecutionProcessorFactory;
import com.dbn.liquibase.model.LiquibaseArtifact;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
import com.dbn.liquibase.ui.LiquibaseArtifactSettingsDialog;
import com.dbn.liquibase.ui.LiquibaseWorkspaceSettingsDialog;
import com.dbn.object.DBSchema;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.util.Dialogs.whenOk;
import static com.dbn.liquibase.execution.LiquibaseOperation.INITIALIZE;
import static com.dbn.liquibase.execution.LiquibaseOperation.VALIDATE;

@State(
        name = DatabaseLiquibaseManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseLiquibaseManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseLiquibaseManager";

    private final StateContainer states = new StateContainer();
    private final LiquibaseWorkspaceBundle workspace;
    private final Map<LiquibaseExecutionResult, LiquibaseExecutionProcessor> executionProcessors = new ConcurrentHashMap<>();

    private DatabaseLiquibaseManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        workspace = new LiquibaseWorkspaceBundle(project);
        ProjectEvents.subscribe(project, this, ConnectionConfigListener.TOPIC,
                ConnectionConfigListener.whenRemoved(workspace::removeArtifact));
    }

    public static DatabaseLiquibaseManager getInstance(@NotNull Project project) {
        return Components.projectService(project, DatabaseLiquibaseManager.class);
    }

    @NotNull
    public LiquibaseWorkspaceBundle getWorkspace() {
        return workspace;
    }

    public boolean isWorkspaceAttached(@NotNull ConnectionId connectionId) {
        return workspace.hasArtifact(connectionId);
    }

    public void openArtifactSettings(@NotNull ConnectionHandler connection) {
        LiquibaseArtifact artifact = workspace.getArtifact(connection.getConnectionId());
        if (artifact == null) return;
        Dialogs.show(() -> new LiquibaseArtifactSettingsDialog(workspace, artifact, false));
    }

    public void openWorkspaceSettings() {
        Dialogs.show(() -> new LiquibaseWorkspaceSettingsDialog(workspace),
                whenOk(d -> workspace.replaceArtifacts(d.getWorkspace())));
    }

    public void detachWorkspace(@NotNull ConnectionHandler connection) {
        workspace.removeArtifact(connection.getConnectionId());
    }

    public void cancelExecution(@NotNull LiquibaseExecutionResult result) {
        LiquibaseExecutionProcessor processor = executionProcessors.get(result);
        if (processor != null) processor.cancel();
    }

    public void generateInitialChangelog(
            @NotNull DBSchema schema,
            @NotNull LiquibaseArtifact artifact,
            @Nullable LiquibaseExecutionResult previousResult) {
        execute(schema, INITIALIZE, artifact, previousResult);
    }

    public void validateChangelog(
            @NotNull DBSchema schema,
            @NotNull LiquibaseArtifact artifact,
            @Nullable LiquibaseExecutionResult previousResult) {
        execute(schema, VALIDATE, artifact, previousResult);
    }

    public void rerun(@NotNull LiquibaseExecutionResult previousResult) {
        execute(
                previousResult.getSchema(),
                previousResult.getOperation(),
                null,
                previousResult);
    }

    private void execute(
            @NotNull DBSchema schema,
            @NotNull LiquibaseOperation operation,
            @Nullable LiquibaseArtifact artifact,
            @Nullable LiquibaseExecutionResult previousResult) {
        ConnectionHandler connection = schema.getConnection();
        ConnectionId connectionId = schema.getConnectionId();
        if (artifact == null) artifact = workspace.getArtifact(connectionId);
        if (artifact == null) {
            promptArtifactCreation(connection,
                    createdArtifact -> execute(schema, operation, createdArtifact, previousResult));
            return;
        }

        LiquibaseExecutionInput input = new LiquibaseExecutionInput(schema, operation, artifact);

        LiquibaseExecutionProcessor processor = LiquibaseExecutionProcessorFactory.create(input);
        LiquibaseExecutionResult result = processor.prepareExecutionResult();
        result.setPrevious(previousResult);
        executionProcessors.put(result, processor);

        ExecutionManager executionManager = ExecutionManager.getInstance(getProject());
        executionManager.addExecutionResult(result);
        Background.run(() -> {
            try {
                processor.execute();
            } finally {
                executionProcessors.remove(result);
            }
        });
    }

    public void promptArtifactCreation(
            @NotNull ConnectionHandler connection,
            @Nullable Consumer<LiquibaseArtifact> consumer) {

        LiquibaseArtifact artifact = workspace.createArtifact();
        Dialogs.show(
                () -> new LiquibaseArtifactSettingsDialog(workspace, artifact, true),
                whenOk(dialog -> {
                    workspace.attachArtifact(connection.getConnectionId(), dialog.getArtifact().getId());
                    if (consumer != null) consumer.accept(dialog.getArtifact());
                }));
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
