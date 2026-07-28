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
import com.dbn.common.environment.EnvironmentTypeId;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.state.StateCategory;
import com.dbn.common.state.StateContainer;
import com.dbn.common.thread.Background;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.SchemaId;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.execution.ExecutionManager;
import com.dbn.liquibase.execution.LiquibaseExecutionHistory;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.processor.LiquibaseExecutionProcessors;
import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.liquibase.operation.LiquibaseOperationContext;
import com.dbn.liquibase.operation.LiquibaseOperationInput;
import com.dbn.liquibase.operation.LiquibaseOperationResult;
import com.dbn.liquibase.task.LiquibaseTaskResult;
import com.dbn.liquibase.workflow.LiquibaseWorkflowExecutor;
import com.dbn.liquibase.workflow.LiquibaseWorkflowInput;
import com.dbn.liquibase.workflow.LiquibaseWorkflowResult;
import com.dbn.liquibase.workspace.LiquibaseEnvironmentProfile;
import com.dbn.liquibase.workspace.LiquibaseEnvironmentProfileBundle;
import com.dbn.liquibase.workspace.LiquibaseWorkspace;
import com.dbn.liquibase.workspace.LiquibaseWorkspaceBundle;
import com.dbn.liquibase.workspace.ui.LiquibaseEnvironmentProfileDialog;
import com.dbn.liquibase.workspace.ui.LiquibaseEnvironmentProfilesDialog;
import com.dbn.liquibase.workspace.ui.LiquibaseWorkspaceDialog;
import com.dbn.liquibase.workspace.ui.LiquibaseWorkspacesDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.util.Dialogs.whenOk;
import static com.dbn.connection.config.ConnectionConfigListener.whenRemoved;
import static com.dbn.nls.NlsResources.txt;

@State(
        name = DatabaseLiquibaseManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseLiquibaseManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseLiquibaseManager";

    private final StateContainer states = new StateContainer();
    private final LiquibaseWorkspaceBundle workspaces;
    private final LiquibaseEnvironmentProfileBundle environmentProfiles;
    private final LiquibaseExecutionHistory executionHistory = new LiquibaseExecutionHistory();
    private final Map<LiquibaseOperationResult, LiquibaseOperationContext> executionContexts = new ConcurrentHashMap<>();
    private final Map<LiquibaseWorkflowResult, LiquibaseWorkflowExecutor> workflowExecutors = new ConcurrentHashMap<>();

    private DatabaseLiquibaseManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        workspaces = new LiquibaseWorkspaceBundle(project);
        environmentProfiles = new LiquibaseEnvironmentProfileBundle(project);
        ProjectEvents.subscribe(project, this, ConnectionConfigListener.TOPIC,
                whenRemoved(c -> executionHistory.removeConnection(c)));
    }

    public static DatabaseLiquibaseManager getInstance(@NotNull Project project) {
        return Components.projectService(project, DatabaseLiquibaseManager.class);
    }

    @NotNull
    public LiquibaseWorkspaceBundle getWorkspaces() {
        return workspaces;
    }

    @NotNull
    public LiquibaseEnvironmentProfileBundle getEnvironmentProfiles() {
        return environmentProfiles;
    }

    @NotNull
    public StateAttributes getState(@NonNls @NotNull String category) {
        return states.ensureAttributes(StateCategory.get(category));
    }

    @NotNull
    public List<String> getTags(
            @NotNull ConnectionId connectionId,
            @NotNull SchemaId schemaId) {
        return executionHistory.getTags(connectionId, schemaId);
    }

    public void rememberTag(
            @NotNull ConnectionId connectionId,
            @NotNull SchemaId schemaId,
            @NotNull String tag) {
        executionHistory.rememberTag(connectionId, schemaId, tag);
    }

    public void removeTag(
            @NotNull ConnectionId connectionId,
            @NotNull SchemaId schemaId,
            @NotNull String tag) {
        executionHistory.removeTag(connectionId, schemaId, tag);
    }

    public void openWorkspaceSettings() {
        Dialogs.show(() -> new LiquibaseWorkspacesDialog(workspaces),
                whenOk(d -> workspaces.replaceWorkspaces(d.getWorkspaces())));
    }

    public void openEnvironmentProfiles() {
        Dialogs.show(() -> new LiquibaseEnvironmentProfilesDialog(getProject(), environmentProfiles),
                whenOk(d -> environmentProfiles.replaceProfiles(d.getBundle())));
    }

    public void openEnvironmentProfileCreationDialog(
            @NotNull EnvironmentTypeId environmentTypeId,
            @NotNull Consumer<LiquibaseEnvironmentProfile> consumer) {
        LiquibaseEnvironmentProfile profile = new LiquibaseEnvironmentProfile(
                txt("app.liquibase.placeholder.NewEnvironmentProfile"),
                environmentTypeId);

        Dialogs.show(
                () -> new LiquibaseEnvironmentProfileDialog(environmentProfiles, profile, true),
                whenOk(d -> consumer.accept(d.getProfile())));
    }

    public void cancelTask(@NotNull LiquibaseTaskResult<?, ?, ?> result) {
        if (result instanceof LiquibaseOperationResult operationResult) {
            LiquibaseOperationContext context = executionContexts.get(operationResult);
            if (context != null) context.cancel();
        } else if (result instanceof LiquibaseWorkflowResult workflowResult) {
            LiquibaseWorkflowExecutor executor = workflowExecutors.get(workflowResult);
            if (executor != null) executor.cancel();
        }
    }

    public void registerExecutionContext(
            @NotNull LiquibaseOperationResult result,
            @NotNull LiquibaseOperationContext context) {
        executionContexts.put(result, context);
    }

    public void unregisterExecutionContext(@NotNull LiquibaseOperationResult result) {
        executionContexts.remove(result);
    }

    public void rerunTask(@NotNull LiquibaseTaskResult<?, ?, ?> previousResult) {
        if (previousResult instanceof LiquibaseOperationResult operationResult) {
            executeOperation(operationResult.getInput(), operationResult);
        } else if (previousResult instanceof LiquibaseWorkflowResult workflowResult) {
            executeWorkflow(workflowResult.getInput(), workflowResult);
        }
    }

    public void executeOperation(
            @NotNull LiquibaseOperationInput input,
            @Nullable LiquibaseOperationResult previousResult) {

        LiquibaseOperation operation = input.getOperation();
        LiquibaseExecutionProcessor processor = LiquibaseExecutionProcessors.get(operation);
        LiquibaseOperationContext context = new LiquibaseOperationContext(input);
        LiquibaseOperationResult result = context.prepareExecutionResult();
        result.setPrevious(previousResult);
        registerExecutionContext(result, context);

        ExecutionManager executionManager = ExecutionManager.getInstance(getProject());
        executionManager.addExecutionResult(result);
        Background.run(() -> {
            try {
                processor.execute(context);
            } finally {
                unregisterExecutionContext(result);
            }
        });
    }

    public void executeWorkflow(
            @NotNull LiquibaseWorkflowInput input,
            @Nullable LiquibaseWorkflowResult previousResult) {
        LiquibaseWorkflowResult result = new LiquibaseWorkflowResult(input);
        result.setPrevious(previousResult);
        LiquibaseWorkflowExecutor executor = new LiquibaseWorkflowExecutor(result);
        workflowExecutors.put(result, executor);

        ExecutionManager.getInstance(getProject()).addExecutionResult(result);
        Background.run(() -> {
            try {
                executor.execute();
            } finally {
                workflowExecutors.remove(result);
            }
        });
    }

    public void openWorkspaceCreationDialog(
            @Nullable DatabaseType databaseType,
            @Nullable Consumer<LiquibaseWorkspace> consumer) {
        LiquibaseWorkspace workspace = new LiquibaseWorkspace();
        workspace.setDatabaseType(databaseType);

        Dialogs.show(
                () -> new LiquibaseWorkspaceDialog(
                        workspaces,
                        workspace,
                        databaseType, true),
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
        environmentProfiles.writeState(element, "environment-profiles");
        executionHistory.writeState(element, "execution-history");

        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        states.readState(element);
        workspaces.readState(element, "workspaces");
        environmentProfiles.readState(element, "environment-profiles");
        executionHistory.readState(element, "execution-history");
    }
}
