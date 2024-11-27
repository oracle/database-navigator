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

package com.dbn.execution.method;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseFeature;
import com.dbn.database.common.execution.MethodExecutionProcessor;
import com.dbn.database.interfaces.DatabaseExecutionInterface;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.ExecutionManager;
import com.dbn.execution.ExecutionStatus;
import com.dbn.execution.method.browser.MethodBrowserSettings;
import com.dbn.execution.method.browser.ui.MethodExecutionBrowserDialog;
import com.dbn.execution.method.history.ui.MethodExecutionHistoryDialog;
import com.dbn.execution.method.ui.MethodExecutionHistory;
import com.dbn.execution.method.ui.MethodExecutionInputDialog;
import com.dbn.object.DBMethod;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ui.ObjectTreeModel;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import lombok.Getter;
import lombok.val;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@State(
    name = MethodExecutionManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
@Getter
public class MethodExecutionManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.MethodExecutionManager";

    private final MethodBrowserSettings browserSettings = new MethodBrowserSettings();
    private final MethodExecutionHistory executionHistory = new MethodExecutionHistory(getProject());
    private final MethodExecutionArgumentValueHistory argumentValuesHistory = new MethodExecutionArgumentValueHistory();

    private MethodExecutionManager(Project project) {
        super(project, COMPONENT_NAME);
        ProjectEvents.subscribe(project, this, ConnectionConfigListener.TOPIC, connectionConfigListener());
    }

    @NotNull
    private ConnectionConfigListener connectionConfigListener() {
        return new ConnectionConfigListener() {
            @Override
            public void connectionRemoved(ConnectionId connectionId) {
                browserSettings.connectionRemoved(connectionId);
                executionHistory.connectionRemoved(connectionId);
                argumentValuesHistory.connectionRemoved(connectionId);
            }
        };
    }

    public static MethodExecutionManager getInstance(@NotNull Project project) {
        return projectService(project, MethodExecutionManager.class);
    }

    public MethodExecutionInput getExecutionInput(DBMethod method) {
        return executionHistory.getExecutionInput(method);
    }

    @NotNull
    public MethodExecutionInput getExecutionInput(DBObjectRef<DBMethod> methodRef) {
        return executionHistory.getExecutionInput(methodRef, true);
    }

    public void startMethodExecution(@NotNull MethodExecutionInput executionInput, @NotNull DBDebuggerType debuggerType) {
        promptExecutionDialog(executionInput, debuggerType, () -> MethodExecutionManager.this.execute(executionInput));
    }

    public void startMethodExecution(@NotNull DBMethod method, @NotNull DBDebuggerType debuggerType) {
        promptExecutionDialog(method, debuggerType, () -> MethodExecutionManager.this.execute(method));
    }

    private void promptExecutionDialog(@NotNull DBMethod method, @NotNull DBDebuggerType debuggerType, Runnable callback) {
        MethodExecutionInput executionInput = getExecutionInput(method);
        promptExecutionDialog(executionInput, debuggerType, callback);
    }

    public void promptExecutionDialog(MethodExecutionInput executionInput, @NotNull DBDebuggerType debuggerType, Runnable callback) {
        Project project = executionInput.getProject();
        DBObjectRef<DBMethod> methodRef = executionInput.getMethodRef();

        ConnectionAction.invoke("the method execution", false, executionInput,
                action -> Progress.prompt(project, action, true,
                        "Loading method details",
                        "Loading details of " + methodRef.getQualifiedNameWithType(),
                        progress -> {
                            ConnectionHandler connection = action.getConnection();
                            String methodIdentifier = methodRef.getPath();
                            if (connection.isValid()) {
                                DBMethod method = executionInput.getMethod();
                                if (method == null) {
                                    String message = "Can not execute method " + methodIdentifier + ".\nMethod not found!";
                                    Messages.showErrorDialog(project, message);
                                } else {
                                    // load the arguments while in background
                                    executionInput.getMethod().getArguments();
                                    showInputDialog(executionInput, debuggerType, callback);
                                }
                            } else {
                                String message =
                                        "Can not execute method " + methodIdentifier + ".\n" +
                                                "No connectivity to '" + connection.getName() + "'. " +
                                                "Please check your connection settings and try again.";
                                Messages.showErrorDialog(project, message);
                            }
                        }));
    }

    private void showInputDialog(@NotNull MethodExecutionInput executionInput, @NotNull DBDebuggerType debuggerType, @NotNull Runnable executor) {
        MethodExecutionInputDialog.open(executionInput, debuggerType, executor);
    }


    public void showExecutionHistoryDialog(
            MethodExecutionInput selection,
            boolean editable,
            boolean modal,
            boolean debug,
            Consumer<MethodExecutionInput> callback) {

        Project project = getProject();
        Progress.prompt(project, selection, true,
                "Loading data dictionary",
                "Loading method execution history",
                progress -> {
                    MethodExecutionInput selectedInput = Commons.nvln(selection, executionHistory.getLastSelection());
                    if (selectedInput != null) {
                        // initialize method arguments while in background
                        DBMethod method = selectedInput.getMethod();
                        if (isValid(method)) {
                            method.getArguments();
                        }
                    }

                    if (!progress.isCanceled()) {
                        Dialogs.show(
                                () -> new MethodExecutionHistoryDialog(project, selectedInput, editable, modal, debug),
                                (dialog, exitCode) -> {
                                    if (exitCode != DialogWrapper.OK_EXIT_CODE) return;

                                    MethodExecutionInput newlySelected = dialog.getSelectedExecutionInput();
                                    if (newlySelected == null) return;
                                    if (callback == null) return;

                                    callback.accept(newlySelected);
                                });
                    }
                });
    }

    public void execute(DBMethod method) {
        MethodExecutionInput executionInput = getExecutionInput(method);
        execute(executionInput);
    }

    public void execute(MethodExecutionInput input) {
        cacheArgumentValues(input);
        executionHistory.setSelection(input.getMethodRef());
        DBMethod method = input.getMethod();
        MethodExecutionContext context = input.getExecutionContext();
        context.set(ExecutionStatus.EXECUTING, true);

        if (method == null) {
            DBObjectRef<DBMethod> methodRef = input.getMethodRef();
            Messages.showErrorDialog(getProject(), "Could not resolve " + methodRef.getQualifiedNameWithType() + "\".");
        } else {
            Project project = method.getProject();
            ConnectionHandler connection = Failsafe.nn(method.getConnection());
            DatabaseExecutionInterface executionInterface = connection.getInterfaces().getExecutionInterface();
            MethodExecutionProcessor executionProcessor = executionInterface.createExecutionProcessor(method);

            Progress.prompt(project, method, true,
                    "Executing method",
                    "Executing " + method.getQualifiedNameWithType(),
                    progress -> {
                try {
                    executionProcessor.execute(input, DBDebuggerType.NONE);
                    if (context.isNot(ExecutionStatus.CANCELLED)) {
                        ExecutionManager executionManager = ExecutionManager.getInstance(project);
                        executionManager.addExecutionResult(input.getExecutionResult());
                        context.set(ExecutionStatus.EXECUTING, false);
                    }

                    context.set(ExecutionStatus.CANCELLED, false);
                } catch (SQLException e) {
                    conditionallyLog(e);
                    context.set(ExecutionStatus.EXECUTING, false);
                    if (context.isNot(ExecutionStatus.CANCELLED)) {
                        Messages.showErrorDialog(project,
                                "Method execution error",
                                "Error executing " + method.getQualifiedNameWithType() + ".\n" + e.getMessage().trim(),
                                new String[]{"Try Again", "Cancel"}, 0,
                                option -> when(option == 0, () ->
                                        startMethodExecution(input, DBDebuggerType.NONE)));
                    }
                }
            });
        }
    }

    private void cacheArgumentValues(MethodExecutionInput input) {
        ConnectionHandler connection = input.getExecutionContext().getTargetConnection();
        if (connection == null) return;

        for (val entry : input.getArgumentValueHistory().entrySet()) {
            MethodExecutionArgumentValue argumentValue = entry.getValue();

            argumentValuesHistory.cacheVariable(
                    connection.getConnectionId(),
                    argumentValue.getName(),
                    argumentValue.getValue());
        }
    }

    public void debugExecute(
            @NotNull MethodExecutionInput input,
            @NotNull DBNConnection conn,
            DBDebuggerType debuggerType) throws SQLException {

        DBMethod method = input.getMethod();
        if (method == null) return;

        ConnectionHandler connection = method.getConnection();
        DatabaseExecutionInterface executionInterface = connection.getInterfaces().getExecutionInterface();
        MethodExecutionProcessor executionProcessor = debuggerType == DBDebuggerType.JDWP ?
                executionInterface.createExecutionProcessor(method) :
                executionInterface.createDebugExecutionProcessor(method);

        executionProcessor.execute(input, conn, debuggerType);
        MethodExecutionContext context = input.getExecutionContext();
        if (context.isNot(ExecutionStatus.CANCELLED)) {
            ExecutionManager executionManager = ExecutionManager.getInstance(method.getProject());
            executionManager.addExecutionResult(input.getExecutionResult());
        }
        context.set(ExecutionStatus.CANCELLED, false);
    }

    public void promptMethodBrowserDialog(
            @Nullable MethodExecutionInput executionInput, boolean debug,
            @Nullable Consumer<MethodExecutionInput> callback) {

        Progress.prompt(getProject(), executionInput, true,
                "Loading data dictionary",
                "Loading executable elements",
                progress -> {
                    Project project = getProject();
                    MethodExecutionManager executionManager = MethodExecutionManager.getInstance(project);
                    MethodBrowserSettings settings = executionManager.getBrowserSettings();
                    DBMethod currentMethod = executionInput == null ? null : executionInput.getMethod();
                    if (currentMethod != null) {
                        currentMethod.getArguments();
                        settings.setSelectedConnection(currentMethod.getConnection());
                        settings.setSelectedSchema(currentMethod.getSchema());
                        settings.setSelectedMethod(currentMethod);
                    }

                    DBSchema schema = settings.getSelectedSchema();
                    ObjectTreeModel objectTreeModel = !debug || DatabaseFeature.DEBUGGING.isSupported(schema) ?
                            new ObjectTreeModel(schema, settings.getVisibleObjectTypes(), settings.getSelectedMethod()) :
                            new ObjectTreeModel(null, settings.getVisibleObjectTypes(), null);


                    Dialogs.show(() -> new MethodExecutionBrowserDialog(project, objectTreeModel, true), (dialog, exitCode) -> {
                        if (exitCode != DialogWrapper.OK_EXIT_CODE) return;

                        DBMethod method = dialog.getSelectedMethod();
                        MethodExecutionInput methodExecutionInput = executionManager.getExecutionInput(method);
                        if (callback != null && methodExecutionInput != null) {
                            callback.accept(methodExecutionInput);
                        }
                    });
                });
    }


    public void setExecutionInputs(List<MethodExecutionInput> executionInputs) {
        executionHistory.setExecutionInputs(executionInputs);
    }

    public void cleanupExecutionHistory(List<ConnectionId> connectionIds) {
        executionHistory.cleanupHistory(connectionIds);
    }

    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        Element browserSettingsElement = newElement(element, "method-browser");
        browserSettings.writeConfiguration(browserSettingsElement);

        executionHistory.writeState(element);
        argumentValuesHistory.writeState(element);
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element browserSettingsElement = element.getChild("method-browser");
        if (browserSettingsElement != null) {
            browserSettings.readConfiguration(browserSettingsElement);
        }

        executionHistory.readState(element);
        argumentValuesHistory.readState(element);
    }




    @Override
    public void disposeInner() {
        Disposer.dispose(executionHistory);
        super.disposeInner();
    }
}
