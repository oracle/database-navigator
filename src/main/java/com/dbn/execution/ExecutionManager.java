/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.execution;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.latent.Latent;
import com.dbn.common.navigation.NavigationInstructions;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionId;
import com.dbn.execution.common.options.ExecutionEngineSettings;
import com.dbn.execution.common.result.ui.ExecutionResultForm;
import com.dbn.execution.common.ui.ExecutionConsoleForm;
import com.dbn.execution.compiler.CompilerResult;
import com.dbn.execution.explain.result.ExplainPlanResult;
import com.dbn.execution.java.result.JavaExecutionResult;
import com.dbn.execution.logging.LogOutput;
import com.dbn.execution.logging.LogOutputContext;
import com.dbn.execution.method.result.MethodExecutionResult;
import com.dbn.execution.statement.options.StatementExecutionSettings;
import com.dbn.execution.statement.result.StatementExecutionResult;
import com.dbn.ml.result.MLExecutionResult;
import com.dbn.vector.model.VectorEmbeddingExecutionResult;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.navigation.NavigationInstruction.FOCUS;
import static com.dbn.common.navigation.NavigationInstruction.SCROLL;
import static com.dbn.common.navigation.NavigationInstruction.SELECT;
import static com.dbn.common.options.setting.Settings.getBoolean;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.setBoolean;
import static com.dbn.common.util.Modality.nonModal;

@State(
    name = ExecutionManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
@Getter
@Setter
public class ExecutionManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.ExecutionManager";
    public static final String TOOL_WINDOW_ID = "DB Execution Console";

    private boolean retainStickyNames = false;

    private final Latent<ExecutionConsoleForm> executionConsoleForm =
            Latent.basic(() -> {
                ExecutionConsoleForm form = new ExecutionConsoleForm(this, getProject());
                Disposer.register(this, form);
                return form;
            });

    private ExecutionManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static ExecutionManager getInstance(@NotNull Project project) {
        return projectService(project, ExecutionManager.class);
    }

    private void showExecutionConsole(Consumer<ExecutionConsoleForm> initializer) {
        Dispatch.run(nonModal(), () -> {
            ToolWindow toolWindow = initExecutionConsole();
            toolWindow.show(null);
        });

        ExecutionConsoleForm consoleForm = getExecutionConsoleForm(); // init form outside EDT
        Dispatch.run(nonModal(), () -> initializer.accept(consoleForm));
    }

    public void hideExecutionConsole() {
        ToolWindow toolWindow = getExecutionConsoleWindow();
        toolWindow.getContentManager().removeAllContents(false);
        toolWindow.setAvailable(false, null);
    }

    public ToolWindow getExecutionConsoleWindow() {
        Project project = getProject();
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        return toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
    }

    private ToolWindow initExecutionConsole() {
        ToolWindow toolWindow = getExecutionConsoleWindow();
        ContentManager contentManager = toolWindow.getContentManager();

        if (contentManager.getContents().length == 0) {
            ExecutionConsoleForm executionConsoleForm = getExecutionConsoleForm();
            ContentFactory contentFactory = contentManager.getFactory();
            Content content = contentFactory.createContent(executionConsoleForm.getComponent(), null, true);
            contentManager.addContent(content);
            toolWindow.setAvailable(true, null);
        }
        return toolWindow;
    }

    @Nullable
    <T extends ExecutionResultForm> T getExecutionResultForm(ExecutionResult executionResult) {
        return getExecutionConsoleForm().getExecutionResultForm(executionResult);
    }

    public void addCompilerResult(@NotNull CompilerResult compilerResult) {
        showExecutionConsole(c -> c.addCompilerResult(compilerResult));
    }

    public void addExplainPlanResult(@NotNull ExplainPlanResult explainPlanResult) {
        NavigationInstructions instructions = NavigationInstructions.create(SELECT, FOCUS);
        showExecutionConsole(c -> c.addResult(explainPlanResult, instructions));
    }

    public void writeLogOutput(@NotNull LogOutputContext context, LogOutput output) {
        if (context.isClosed()) return;
        showExecutionConsole(c -> c.displayLogOutput(context, output));
    }

    public void addExecutionResult(@NotNull StatementExecutionResult executionResult, NavigationInstructions instructions) {
        showExecutionConsole(c -> {
            if (executionResult.isLoggingActive()) {
                LogOutputContext context = new LogOutputContext(executionResult.getConnection());
                context.setHideEmptyLines(false);
                String loggingOutput = executionResult.getLoggingOutput();

                c.displayLogOutput(
                        context, LogOutput.createSysOutput(context,
                                executionResult.getExecutionContext().getExecutionTimestamp(),
                                " - Statement execution started", false));

                if (Strings.isNotEmptyOrSpaces(loggingOutput)) {
                    c.displayLogOutput(context,
                            LogOutput.createStdOutput(loggingOutput));
                }

                c.displayLogOutput(context,
                        LogOutput.createSysOutput(context, " - Statement execution finished\n", false));
            }

            c.addResult(executionResult, instructions);
            if (!executionResult.isBulkExecution() && !executionResult.hasCompilerResult() && !focusOnExecution()) {
                executionResult.navigateToEditor(NavigationInstructions.create(FOCUS, SCROLL, SELECT));
            }
        });
    }

    private boolean focusOnExecution() {
        Project project = getProject();
        ExecutionEngineSettings executionEngineSettings = ExecutionEngineSettings.getInstance(project);
        StatementExecutionSettings statementExecutionSettings = executionEngineSettings.getStatementExecutionSettings();
        return statementExecutionSettings.isFocusResult();
    }


    public void addExecutionResult(MethodExecutionResult executionResult) {
        showExecutionConsole(c -> c.addResult(executionResult));
    }

    public void addExecutionResult(VectorEmbeddingExecutionResult executionResult) {
        showExecutionConsole(c -> c.addResult(executionResult));
    }

    public void addExecutionResult(MLExecutionResult executionResult) {
        showExecutionConsole(c -> c.addResult(executionResult));
    }

    public void addExecutionResult(JavaExecutionResult executionResult) {
        showExecutionConsole(c -> c.addResult(executionResult));
    }

    public void selectExecutionResult(StatementExecutionResult executionResult) {
        NavigationInstructions instructions = NavigationInstructions.create(FOCUS, SCROLL, SELECT);
        showExecutionConsole(c -> c.selectResult(executionResult, instructions));
    }

    public void removeMessagesTab() {
        Dispatch.run(nonModal(), () -> getExecutionConsoleForm().removeMessagesTab());
    }

    public void removeResultTab(ExecutionResult executionResult) {
        Dispatch.run(nonModal(), () -> getExecutionConsoleForm().removeResultTab(executionResult));
    }

    public void selectResultTab(ExecutionResult executionResult) {
        showExecutionConsole(f -> f.selectResultTab(executionResult));
    }

    @NotNull
    public ExecutionConsoleForm getExecutionConsoleForm() {
        return executionConsoleForm.get();
    }

    public void closeExecutionResults(List<ConnectionId> connectionIds){
        ExecutionConsoleForm executionConsoleForm = getExecutionConsoleForm();
        executionConsoleForm.closeExecutionResults(connectionIds);
    }

    @Nullable
    public ExecutionResult getSelectedExecutionResult() {
        if (!executionConsoleForm.loaded()) return null;
        return Dispatch.call(true, () ->
                getExecutionConsoleForm().getSelectedExecutionResult());
    }

    public Set<String> getExecutionResultNames(Class<? extends ExecutionResult<?>> resultType) {
        ExecutionConsoleForm executionConsoleForm = getExecutionConsoleForm();
        return executionConsoleForm.getExecutionResultNames(resultType);
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    public Element getComponentState() {
        Element element = newStateElement();
        setBoolean(element, "retain-sticky-names", retainStickyNames);
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        retainStickyNames = getBoolean(element, "retain-sticky-names", retainStickyNames);
    }

}
