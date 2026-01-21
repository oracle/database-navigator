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

package com.dbn.debugger;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.routine.Consumer;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.database.common.debug.DebuggerVersionInfo;
import com.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.debugger.common.breakpoint.DBBreakpointUpdaterFileEditorListener;
import com.dbn.debugger.jdbc.process.DBMethodJdbcRunner;
import com.dbn.debugger.jdbc.process.DBStatementJdbcRunner;
import com.dbn.debugger.jdwp.process.DBJavaJdwpRunner;
import com.dbn.debugger.jdwp.process.DBMethodJdwpRunner;
import com.dbn.debugger.jdwp.process.DBStatementJdwpRunner;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBMethod;
import com.dbn.object.DBProgram;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.vfs.DBConsoleType;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.load.ProgressMonitor.setProgressDetail;
import static com.dbn.common.notification.NotificationCategory.DEBUGGER;
import static com.dbn.common.util.Commons.array;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.database.DatabaseFeature.DEBUGGING;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

@State(
    name = DatabaseDebuggerManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseDebuggerManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DebuggerManager";

    private final Set<ConnectionRef> activeDebugSessions = new HashSet<>();

    private DatabaseDebuggerManager(Project project) {
        super(project, COMPONENT_NAME);

        ProjectEvents.subscribe(project, this, FileEditorManagerListener.FILE_EDITOR_MANAGER, new DBBreakpointUpdaterFileEditorListener());
    }

    public static DatabaseDebuggerManager getInstance(@NotNull Project project) {
        return projectService(project, DatabaseDebuggerManager.class);
    }

    public void registerDebugSession(ConnectionHandler connection) {
        activeDebugSessions.add(connection.ref());
    }

    public void unregisterDebugSession(ConnectionHandler connection) {
        activeDebugSessions.remove(connection.ref());
    }

    public boolean checkForbiddenOperation(ConnectionHandler connection) {
        return checkForbiddenOperation(connection, null);
    }

    public boolean checkForbiddenOperation(DatabaseContext connection) {
        return checkForbiddenOperation(connection.getConnection());
    }


    public boolean checkForbiddenOperation(ConnectionHandler connection, String message) {
        // TODO add flag on connection handler instead of this
        if (activeDebugSessions.contains(connection.ref())) {
            Messages.showErrorDialog(getProject(), message == null ? "Operation not supported during active debug session." : message);
            return false;
        }
        return true;
    }

    public static boolean isDebugConsole(VirtualFile virtualFile) {
        if (virtualFile instanceof DBConsoleVirtualFile consoleVirtualFile) {
            return consoleVirtualFile.getType() == DBConsoleType.DEBUG;
        }
        return false;
    }

    public static void verifyJdwpSupport(boolean jdbcFallback) throws RuntimeConfigurationError {
        if (DBDebuggerType.JDWP.isSupported()) return;

        ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
        String versionName = applicationInfo.getVersionName();
        String fullVersion = applicationInfo.getFullVersion();

        String message = "JDWP debugging is not supported in \"" + versionName + " " + fullVersion + "\".";
        String messageFallbackHint = jdbcFallback ? "" : " Please use Classic debugger over JDBC instead.";
        throw new RuntimeConfigurationError(message + messageFallbackHint);
    }

    public void startMethodDebugger(@NotNull DBMethod method) {
        startDebugger(method.getConnection(), (debuggerType) -> {
            ExecutionConfigManager configManager = getConfigManager();
            RunnerAndConfigurationSettings settings = configManager.createConfiguration(method, debuggerType);

            String runnerId =
                    debuggerType == DBDebuggerType.JDBC ? DBMethodJdbcRunner.RUNNER_ID :
                    debuggerType == DBDebuggerType.JDWP ? DBMethodJdwpRunner.RUNNER_ID : null;

            try {
                startDebugger(runnerId, settings);
            } catch (ExecutionException e) {
                conditionallyLog(e);
                Messages.showErrorDialog(getProject(), "Could not start debugger for " + method.getQualifiedName() + ".", e);
            }
        });
    }

    public void startStatementDebugger(@NotNull StatementExecutionProcessor executionProcessor) {
        ConnectionHandler connection = executionProcessor.getConnection();
        if (isNotValid(connection)) return;

        startDebugger(connection, debuggerType -> {
            ExecutionConfigManager configManager = getConfigManager();
            RunnerAndConfigurationSettings settings = configManager.createConfiguration(executionProcessor, debuggerType);

            String runnerId =
                    debuggerType == DBDebuggerType.JDBC ? DBStatementJdbcRunner.RUNNER_ID :
                    debuggerType == DBDebuggerType.JDWP ? DBStatementJdwpRunner.RUNNER_ID :
                                    null;

            try {
                startDebugger(runnerId, settings);
            } catch (ExecutionException e) {
                conditionallyLog(e);
                Messages.showErrorDialog(getProject(), "Could not start statement debugger",  e);
            }
        });
    }

    public void startJavaDebugger(@NotNull DBJavaMethod method) {
        ConnectionHandler connection = method.getConnection();
        if (isNotValid(connection)) return;

        startJdwpDebugger(() -> {
            ExecutionConfigManager configManager = getConfigManager();
            RunnerAndConfigurationSettings settings = configManager.createConfiguration(method, DBDebuggerType.JDWP);

            try {
                startDebugger(DBJavaJdwpRunner.RUNNER_ID, settings);
            } catch (ExecutionException e) {
                conditionallyLog(e);
                Messages.showErrorDialog(getProject(), "Could not start debugger for " + method.getQualifiedName() + ").", e);
            }
        });
    }

    private void startDebugger(String runnerId, RunnerAndConfigurationSettings settings) throws ExecutionException {
        if (runnerId == null) return;

        ProgramRunner programRunner = ProgramRunner.findRunnerById(runnerId);
        if (programRunner == null) return;

        Executor executorInstance = DefaultDebugExecutor.getDebugExecutorInstance();
        if (executorInstance == null) {
            throw new ExecutionException("Could not resolve debug executor");
        }

        Project project = getProject();
        ExecutionEnvironment executionEnvironment = new ExecutionEnvironment(executorInstance, programRunner, settings, project);
        programRunner.execute(executionEnvironment);
    }

    private void startDebugger(@NotNull ConnectionHandler connection, @NotNull Consumer<DBDebuggerType> debuggerStarter) {
        var debuggerTypeOption = connection.getSettings().getDebuggerSettings().getDebuggerType();
        Project project = getProject();
        debuggerTypeOption.resolve(project, array(), option -> {
            DBDebuggerType debuggerType = option.getDebuggerType();
            if (debuggerType == null) return;

            if (debuggerType.isSupported()) {
                debuggerStarter.accept(debuggerType);
            } else {
                ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
                Messages.showErrorDialog(
                        project,
                        txt("msg.debugger.title.UnsupportedDebugger"),
                        txt("msg.debugger.error.UnsupportedDebuggerUseAlternative",
                                debuggerType.getName(),
                                applicationInfo.getVersionName(),
                                applicationInfo.getFullVersion()),
                        new String[]{
                                txt("msg.debugger.button.UseDebugger",DBDebuggerType.JDBC.getName()),
                                txt("msg.shared.button.Cancel")}, 0,
                        o -> when(o == 0, () -> debuggerStarter.accept(DBDebuggerType.JDBC)));
            }
        });
    }

    private void startJdwpDebugger(@NotNull Runnable debuggerStarter) {
        DBDebuggerType debuggerType = DBDebuggerType.JDWP;
        if (debuggerType.isSupported()) {
            debuggerStarter.run();
        } else {
            ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
            Messages.showErrorDialog(
                    getProject(),
                    txt("msg.debugger.title.UnsupportedDebugger"),
                    txt("msg.debugger.error.UnsupportedDebugger",
                            debuggerType.getName(),
                            applicationInfo.getVersionName(),
                            applicationInfo.getFullVersion()),
                    new String[]{txt("msg.shared.button.Close")}, 0,
                    o -> {});

        }
    }

    public List<DBSchemaObject> loadCompileDependencies(List<DBMethod> methods) {
        // TODO improve this logic (currently only drilling one level down in the dependencies)
        List<DBSchemaObject> compileList = new ArrayList<>();
        for (DBMethod method : methods) {
            DBProgram program = method.getProgram();
            DBSchemaObject executable = program == null ? method : program;
            SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(getProject());
            sourceCodeManager.ensureSourcesLoaded(executable, true);

            addToCompileList(compileList, executable);

            for (DBObject object : executable.getReferencedObjects()) {
                if (object instanceof DBSchemaObject schemaObject && object != executable) {
                    if (!ProgressMonitor.isProgressCancelled()) {
                        boolean added = addToCompileList(compileList, schemaObject);
                        if (added) {
                            String objectName = schemaObject.getQualifiedNameWithType();
                            setProgressDetail(txt("prc.debugger.text.LoadingDependencies", objectName));
                            schemaObject.getReferencedObjects();
                        }
                    }
                }
            }
        }

        compileList.sort(DEPENDENCY_COMPARATOR);
        return compileList;
    }

    private boolean addToCompileList(List<DBSchemaObject> compileList, DBSchemaObject schemaObject) {
        DBSchema schema = schemaObject.getSchema();
        DBObjectStatusHolder objectStatus = schemaObject.getStatus();
        if (!schema.isPublicSchema() && !schema.isSystemSchema() && schemaObject.is(DBObjectProperty.DEBUGABLE) && !objectStatus.is(DBObjectStatus.DEBUG)) {
            if (!compileList.contains(schemaObject)) {
                compileList.add(schemaObject);
            }

            return true;
        }
        return false;
    }

    private static final Comparator<DBSchemaObject> DEPENDENCY_COMPARATOR = (schemaObject1, schemaObject2) -> {
        if (schemaObject1.getReferencedObjects().contains(schemaObject2)) return 1;
        if (schemaObject2.getReferencedObjects().contains(schemaObject1)) return -1;
        return 0;
    };

    public String getDebuggerVersion(@NotNull ConnectionHandler connection) {
        return loadDebuggerVersion(connection);
    }

    private String loadDebuggerVersion(@NotNull ConnectionHandler connection) {
        if (!DEBUGGING.isSupported(connection)) return txt("app.shared.label.Unknown");

        try {
            return DatabaseInterfaceInvoker.load(HIGHEST,
                    "Loading metadata",
                    "Loading debugger version",
                    connection.getProject(),
                    connection.getConnectionId(),
                    conn -> {
                        DatabaseDebuggerInterface debuggerInterface = connection.getDebuggerInterface();
                        DebuggerVersionInfo debuggerVersion = debuggerInterface.getDebuggerVersion(conn);
                        return debuggerVersion.getVersion();
                    });
        } catch (SQLException e) {
            conditionallyLog(e);
            sendErrorNotification(DEBUGGER, txt("ntf.debugger.error.FailedToLoadVersion", e));

            return txt("app.shared.label.Unknown");
        }
    }

    private ExecutionConfigManager getConfigManager() {
        return ExecutionConfigManager.getInstance(getProject());
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {

    }
}