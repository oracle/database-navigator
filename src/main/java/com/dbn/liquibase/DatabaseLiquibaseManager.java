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
import com.dbn.common.state.StateContainer;
import com.dbn.common.thread.Background;
import com.dbn.common.util.Dialogs;
import com.dbn.execution.ExecutionManager;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.processor.LiquibaseExecutionProcessorFactory;
import com.dbn.liquibase.model.LiquibaseWorkspace;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
import com.dbn.liquibase.ui.LiquibaseWorkspaceBundleSettingsDialog;
import com.dbn.liquibase.ui.LiquibaseWorkspaceSettingsDialog;
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

@State(
        name = DatabaseLiquibaseManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseLiquibaseManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseLiquibaseManager";

    private final StateContainer states = new StateContainer();
    private final LiquibaseWorkspaceBundle workspaces;
    private final Map<LiquibaseExecutionResult, LiquibaseExecutionProcessor> executionProcessors = new ConcurrentHashMap<>();

    private DatabaseLiquibaseManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        workspaces = new LiquibaseWorkspaceBundle(project);
    }

    public static DatabaseLiquibaseManager getInstance(@NotNull Project project) {
        return Components.projectService(project, DatabaseLiquibaseManager.class);
    }

    @NotNull
    public LiquibaseWorkspaceBundle getWorkspaces() {
        return workspaces;
    }

    public void openWorkspaceSettings() {
        Dialogs.show(() -> new LiquibaseWorkspaceBundleSettingsDialog(workspaces),
                whenOk(d -> workspaces.replaceWorkspaces(d.getWorkspaces())));
    }

    public void cancelExecution(@NotNull LiquibaseExecutionResult result) {
        LiquibaseExecutionProcessor processor = executionProcessors.get(result);
        if (processor != null) processor.cancel();
    }

    public void rerunOperation(@NotNull LiquibaseExecutionResult previousResult) {
        executeOperation(previousResult.getInput(), previousResult);
    }

    public void executeOperation(
            @NotNull LiquibaseExecutionInput input,
            @Nullable LiquibaseExecutionResult previousResult) {

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

    public void openWorkspaceCreationDialog(@Nullable Consumer<LiquibaseWorkspace> consumer) {
        Dialogs.show(
                () -> new LiquibaseWorkspaceSettingsDialog(workspaces, new LiquibaseWorkspace(), true),
                whenOk(dialog -> {
                    if (consumer != null) consumer.accept(dialog.getWorkspace());
                }));
    }

    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        states.writeState(element);
        workspaces.writeState(element, "workspace");
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        states.readState(element);
        workspaces.readState(element, "workspace");
    }
}
