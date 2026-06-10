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

import com.dbn.common.compatibility.Compatibility;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.notification.NotificationSupport;
import com.dbn.common.operation.DatabaseOperation;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Modality;
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
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.dbn.common.Reflection.invokeMethod;
import static com.dbn.common.notification.NotificationCategory.DEBUGGER;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.debugger.DBDebugUtil.isToolwindowSplit;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;
import static com.intellij.openapi.ui.DialogWrapper.OK_EXIT_CODE;

@Getter
public abstract class DBProgramRunner<T extends ExecutionInput> extends GenericProgramRunner  {
    public static final String INVALID_RUNNER_ID = "DBNInvalidRunner";

    private final DBDebuggerType debuggerType;
    private final DatabaseOperation databaseOperation;

    public DBProgramRunner(DBDebuggerType debuggerType, DatabaseOperation databaseOperation) {
        this.debuggerType = debuggerType;
        this.databaseOperation = databaseOperation;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        if (!Objects.equals(executorId, DefaultDebugExecutor.EXECUTOR_ID)) return false;

        if (profile instanceof DBRunConfig<?> config) {
            DBDebuggerType configDebuggerType = config.getDebuggerType();
            if (debuggerType != configDebuggerType) return false;

            return config.canRun();
        }
        return false;
    }

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment) {
        Project project = environment.getProject();
        DBRunConfig<T> runProfile = cast(environment.getRunProfile());
        ConnectionHandler connection = runProfile.getConnection();
        if (connection == null) return null;

        DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
        boolean canContinue = debuggerManager.checkForbiddenOperation(connection,
                txt("msg.debugger.error.ActiveDebuggerSession"));
        if (!canContinue) return null;

        T executionInput = runProfile.getExecutionInput();
        // TODO move to the debug actions (all prerequisite verifications should be invoked in actions)
        databaseOperation.start(connection, () -> performInitialization(executionInput, environment));
        return null;
    }

    protected void performInitialization(
            @NotNull T executionInput,
            @NotNull ExecutionEnvironment environment) {
        ConnectionHandler connection = executionInput.ensureConnection();

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
                                environment);
                    } else {
                        performCompile(
                                connection,
                                executionInput,
                                environment,
                                dependencies);
                    }
                });
    }

    private void performCompile(
            @NotNull ConnectionHandler connection,
            @NotNull T executionInput,
            @NotNull ExecutionEnvironment environment,
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

                    performExecution(executionInput, environment);
                });
            } else {
                performExecution(executionInput, environment);
            }
        });
    }

    protected void performExecution(T executionInput, ExecutionEnvironment environment) {
        Dispatch.run(Modality.nonModal(), () ->
                promptExecutionDialog(executionInput, () ->
                        triggerExecution(executionInput, environment)));
    }

    private void triggerExecution(T executionInput, ExecutionEnvironment environment) {
        ConnectionHandler connection = executionInput.getConnection();
        Project project = environment.getProject();

        DBDebugProcessStarter processStarter = createProcessStarter(connection);
        try {
            RunContentDescriptor descriptor = startSession(project, environment, processStarter);
            if (descriptor == null) return;

            Executor executor = environment.getExecutor();
            // TODO check why this was conditional before (remove "always-true" condition)
            if (true /*LocalHistoryConfiguration.getInstance().ADD_LABEL_ON_RUNNING*/) {
                RunProfile runProfile = environment.getRunProfile();
                LocalHistory.getInstance().putSystemLabel(project, executor.getId() + " " + runProfile.getName());
            }

            ExecutionManager executionManager = ExecutionManager.getInstance(project);
            RunContentManager contentManager = executionManager.getContentManager();
            // Split debugger shows the frontend tab itself; manually showing the returned descriptor exposes the backend mock tab.
            if (!isToolwindowSplit()) {
                contentManager.showRunContent(executor, descriptor);

                ProcessHandler processHandler = descriptor.getProcessHandler();
                if (processHandler == null) return;
                if (!processHandler.isStartNotified()) processHandler.startNotify();

                ExecutionConsole executionConsole = descriptor.getExecutionConsole();
                if (executionConsole instanceof ConsoleView consoleView) {
                    consoleView.attachToProcess(processHandler);
                }
            }

        } catch (ExecutionException e) {
            conditionallyLog(e);
            NotificationSupport.sendErrorNotification(project, DEBUGGER,
                    txt("ntf.debugger.error.ErrorInitializingEnvironment", e));
        }
    }

    @Nullable
    @Compatibility
    @SuppressWarnings("deprecation")
    private RunContentDescriptor startSession(
            Project project,
            ExecutionEnvironment environment,
            DBDebugProcessStarter starter) throws ExecutionException {

        // Split debugger needs the builder/showTab path so XDebugger creates the real frontend descriptor.
        return isToolwindowSplit() ?
                startSplitSession(project, environment, starter) :
                startLegacySession(project, environment, starter);
    }

    private static RunContentDescriptor startSplitSession(
            Project project,
            ExecutionEnvironment environment,
            DBDebugProcessStarter starter) throws ExecutionException {

/*            // workaround for new split-debugger api issue
            XDebugSessionBuilder builder = debuggerManager.newSessionBuilder(processStarter);
            builder.environment(environment);
            builder.sessionName(environment.getRunProfile().getName());
            builder.showTab(true);
            RunContentDescriptor contentToReuse = environment.getContentToReuse();
            if (contentToReuse != null) {
                builder.contentToReuse(contentToReuse1);
            }
            XSessionStartedResult result = builder.startSession();
            return result.getRunContentDescriptor();*/

        Object sessionResult = null;
        try {
            XDebuggerManager debuggerManager = XDebuggerManager.getInstance(project);
            Object sessionBuilder = invokeMethod(debuggerManager, "newSessionBuilder", starter);
            invokeMethod(sessionBuilder, "environment", environment);
            invokeMethod(sessionBuilder, "sessionName", environment.getRunProfile().getName());
            RunContentDescriptor contentToReuse = environment.getContentToReuse();

            if (contentToReuse != null) {
                invokeMethod(sessionBuilder, "contentToReuse", contentToReuse);
            }

            invokeMethod(sessionBuilder, "showTab", true);
            sessionResult = invokeMethod(sessionBuilder, "startSession");
            return invokeMethod(sessionResult, "getRunContentDescriptor");
        } catch (Throwable e) {
            Throwable cause = Exceptions.unwrap(e);
            if (cause instanceof ExecutionException executionException) throw executionException;
            if (sessionResult != null) throw new ExecutionException(cause);

            return startLegacySession(project, environment, starter);
        }

    }

    @SuppressWarnings("deprecation")
    @NotNull
    private static RunContentDescriptor startLegacySession(
            Project project,
            ExecutionEnvironment environment,
            DBDebugProcessStarter starter) throws ExecutionException {

        XDebuggerManager debuggerManager = XDebuggerManager.getInstance(project);
        XDebugSession session = debuggerManager.startSession(environment, starter);
        return session.getRunContentDescriptor();
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
