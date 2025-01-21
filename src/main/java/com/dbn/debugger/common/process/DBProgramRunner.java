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

package com.dbn.debugger.common.process;

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.notification.NotificationSupport;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.config.ConnectionDebuggerSettings;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.debugger.DatabaseDebuggerManager;
import com.dbn.debugger.common.config.DBRunConfig;
import com.dbn.debugger.common.config.ui.CompileDebugDependenciesDialog;
import com.dbn.editor.DBContentType;
import com.dbn.execution.ExecutionInput;
import com.dbn.execution.compiler.CompileManagerListener;
import com.dbn.execution.compiler.CompileType;
import com.dbn.execution.compiler.CompilerAction;
import com.dbn.execution.compiler.CompilerActionSource;
import com.dbn.execution.compiler.DatabaseCompilerManager;
import com.dbn.nls.NlsSupport;
import com.dbn.object.DBMethod;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.history.LocalHistory;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.dbn.common.notification.NotificationGroup.DEBUGGER;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Messages.options;
import static com.dbn.common.util.Messages.showWarningDialog;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.intellij.openapi.ui.DialogWrapper.OK_EXIT_CODE;

public abstract class DBProgramRunner<T extends ExecutionInput> extends GenericProgramRunner implements NlsSupport {
    public static final String INVALID_RUNNER_ID = "DBNInvalidRunner";

    public abstract DBDebuggerType getDebuggerType();

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        if (!Objects.equals(executorId, DefaultDebugExecutor.EXECUTOR_ID)) return false;

        if (profile instanceof DBRunConfig) {
            DBRunConfig<?> config = (DBRunConfig<?>) profile;
            if (!Objects.equals(
                    this.getDebuggerType(),
                    config.getDebuggerType())) return false;

            return config.canRun();
        }
        return false;
    }

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment) {
        Project project = environment.getProject();
        DBRunConfig runProfile = (DBRunConfig) environment.getRunProfile();
        ConnectionHandler connection = runProfile.getConnection();
        if (connection == null) return null;

        ConnectionAction.invoke(txt("msg.debugger.title.DebugExecution"), false, connection,
                action -> Progress.prompt(project, connection, true,
                        txt("prc.debugger.title.CheckingPrivileges"),
                        txt("prc.debugger.text.CheckingPrivileges",connection.getUserName()),
                        progress -> performPrivilegeCheck(
                                project,
                                cast(runProfile.getExecutionInput()),
                                environment,
                                null)),
                null,
                action -> {
                    DatabaseDebuggerManager databaseDebuggerManager = DatabaseDebuggerManager.getInstance(project);
                    return databaseDebuggerManager.checkForbiddenOperation(connection,
                            txt("msg.debugger.error.ActiveDebuggerSession"));
                });
        return null;
    }

    private void performPrivilegeCheck(
            Project project,
            T executionInput,
            ExecutionEnvironment environment,
            Callback callback) {
        DBRunConfig runProfile = (DBRunConfig) environment.getRunProfile();
        ConnectionHandler connection = runProfile.getConnection();
        if (connection == null) return;

        DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
        List<String> missingPrivileges = debuggerManager.getMissingDebugPrivileges(connection);
        if (missingPrivileges.isEmpty()) {
            performInitialization(
                    connection,
                    executionInput,
                    environment,
                    callback);
        } else {
            String privilegeList = missingPrivileges.stream().map(p -> " - " + p + "\n").collect(Collectors.joining());

            showWarningDialog(
                    project,
                    txt("msg.debugger.title.InsufficientPrivileges"),
                    txt("msg.debugger.error.InsufficientPrivileges", connection.getUserName(), privilegeList),
                    options(
                        txt("msg.debugger.button.ContinueAnyway"),
                        txt("msg.shared.button.Cancel")), 0,
                    option -> when(option == 0, () ->
                            performInitialization(
                                    connection,
                                    executionInput,
                                    environment,
                                    callback)));
        }
    }

    private void performInitialization(
            @NotNull ConnectionHandler connection,
            @NotNull T executionInput,
            @NotNull ExecutionEnvironment environment,
            @Nullable Callback callback) {

        ConnectionDebuggerSettings debuggerSettings = connection.getSettings().getDebuggerSettings();
        if (!debuggerSettings.isCompileDependencies()) return;

        Project project = connection.getProject();
        Progress.prompt(project, connection, true,
                txt("prc.debugger.title.InitializingEnvironment"),
                txt("prc.debugger.text.LoadingMethodDependencies"),
                progress -> {
                    if (progress.isCanceled()) return;
                    DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);

                    DBRunConfig<?> runProfile = (DBRunConfig) environment.getRunProfile();
                    List<DBMethod> methods = runProfile.getMethods();
                    List<DBSchemaObject> dependencies = debuggerManager.loadCompileDependencies(methods);
                    if (progress.isCanceled()) return;

                    if (dependencies.isEmpty()) {
                        performExecution(
                                executionInput,
                                environment,
                                callback);
                    } else {
                        performCompile(
                                connection,
                                executionInput,
                                environment,
                                callback,
                                dependencies);
                    }
                });
    }

    private void performCompile(
            @NotNull ConnectionHandler connection,
            @NotNull T executionInput,
            @NotNull ExecutionEnvironment environment,
            @Nullable Callback callback,
            List<DBSchemaObject> dependencies) {

        Dispatch.run(() -> {
            Project project = connection.getProject();
            DBRunConfig runConfiguration = (DBRunConfig) environment.getRunProfile();
            CompileDebugDependenciesDialog dependenciesDialog = new CompileDebugDependenciesDialog(runConfiguration, dependencies);
            dependenciesDialog.show();

            DBObjectRef<DBSchemaObject>[] selectedDependencies =  dependenciesDialog.getSelection();
            if (dependenciesDialog.getExitCode() != OK_EXIT_CODE) return;

            if (selectedDependencies.length > 0) {
                Progress.prompt(project, connection, true,
                        txt("prc.debugger.title.CompilingDependencies"),
                        txt("prc.debugger.text.CompilingDependencies"),
                        progress -> {
                    DatabaseCompilerManager compilerManager = DatabaseCompilerManager.getInstance(project);
                    for (DBObjectRef<DBSchemaObject> objectRef : selectedDependencies) {
                        DBSchemaObject schemaObject = objectRef.ensure();
                        progress.checkCanceled();

                        progress.setText2(txt("prc.debugger.text.CompilingDependency", objectRef.getQualifiedNameWithType()));
                        DBContentType contentType = schemaObject.getContentType();
                        CompilerAction compilerAction = new CompilerAction(CompilerActionSource.BULK_COMPILE, contentType);
                        compilerManager.compileObject(schemaObject, CompileType.DEBUG, compilerAction);
                    }
                    ProjectEvents.notify(project,
                            CompileManagerListener.TOPIC,
                            (listener) -> listener.compileFinished(connection, null));
                            progress.checkCanceled();

                    performExecution(
                            executionInput,
                            environment,
                            callback);
                });
            } else {
                performExecution(
                        executionInput,
                        environment,
                        callback);
            }
        });
    }

    private void performExecution(
            T executionInput,
            ExecutionEnvironment environment,
            Callback callback) {
        Dispatch.run(() ->
                promptExecutionDialog(executionInput, () ->
                        triggerExecution(executionInput, environment, callback)));
    }

    private void triggerExecution(T executionInput, ExecutionEnvironment environment, Callback callback) {
        ConnectionHandler connection = executionInput.getConnection();
        Project project = environment.getProject();

        DBDebugProcessStarter processStarter = createProcessStarter(connection);
        try {
            XDebuggerManager debuggerManager = XDebuggerManager.getInstance(project);
            XDebugSession session = debuggerManager.startSession(environment, processStarter);

            RunContentDescriptor descriptor = session.getRunContentDescriptor();

            if (callback != null) callback.processStarted(descriptor);
            Executor executor = environment.getExecutor();
            if (true /*LocalHistoryConfiguration.getInstance().ADD_LABEL_ON_RUNNING*/) {
                RunProfile runProfile = environment.getRunProfile();
                LocalHistory.getInstance().putSystemLabel(project, executor.getId() + " " + runProfile.getName());
            }

            ExecutionManager executionManager = ExecutionManager.getInstance(project);
            RunContentManager contentManager = executionManager.getContentManager();
            contentManager.showRunContent(executor, descriptor);

            ProcessHandler processHandler = descriptor.getProcessHandler();
            if (processHandler == null) return;
            if (!processHandler.isStartNotified()) processHandler.startNotify();

            ExecutionConsole executionConsole = descriptor.getExecutionConsole();
            if (executionConsole instanceof ConsoleView) {
                ConsoleView consoleView = (ConsoleView) executionConsole;
                consoleView.attachToProcess(processHandler);
            }

        } catch (ExecutionException e) {
            conditionallyLog(e);
            NotificationSupport.sendErrorNotification(project, DEBUGGER,
                    txt("ntf.debugger.error.ErrorInitializingEnvironment", e));
        }
    }
/*
    public static  boolean isCloudDatabaseDefaultValue(ConnectionHandler conn)  {
        try{
            boolean isCloud = databaseHost.matches(CLOUD_DATABASE_PATTERN);
            if(!isCloud && hostnames != null && !hostnames.isEmpty()){
                String [] hostnamesArray = hostnames.split(",");
                isCloud = Arrays.asList(hostnamesArray).contains(databaseHost);
            }
            if ( !isCloud &&( databaseHost.equals("localhost") || databaseHost.equals("127.0.0.1") ||databaseHost.equals( InetAddress.getLocalHost().getHostAddress()))) {
                return isCloud = false;
            }
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }

        return  true;
    }
*/

    protected abstract DBDebugProcessStarter createProcessStarter(ConnectionHandler connection);

    protected abstract void promptExecutionDialog(T executionInput, Runnable callback);
}
