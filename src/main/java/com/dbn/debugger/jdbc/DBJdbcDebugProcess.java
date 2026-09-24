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

package com.dbn.debugger.jdbc;

import com.dbn.common.cache.ObjectKey;
import com.dbn.common.dispose.AlreadyDisposedException;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.notification.NotificationSupport;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.ThreadContext;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.Resources;
import com.dbn.connection.SchemaId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.debug.DebuggerIdentifierInfo;
import com.dbn.database.common.debug.DebuggerIdentifierModel;
import com.dbn.database.common.debug.DebuggerRuntimeInfo;
import com.dbn.database.common.debug.DebuggerSessionInfo;
import com.dbn.database.common.debug.ExecutionBacktraceInfo;
import com.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.debugger.DBDebugConsoleLogger;
import com.dbn.debugger.DBDebugOperation;
import com.dbn.debugger.DBDebugTabLayouter;
import com.dbn.debugger.DBDebugUtil;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.debugger.DatabaseDebuggerManager;
import com.dbn.debugger.common.breakpoint.DBBreakpointHandler;
import com.dbn.debugger.common.breakpoint.DBBreakpointUtil;
import com.dbn.debugger.common.config.DBRunConfig;
import com.dbn.debugger.common.process.DBDebugProcess;
import com.dbn.debugger.common.process.DBDebugProcessStatus;
import com.dbn.debugger.common.process.DBDebugProcessStatusHolder;
import com.dbn.debugger.jdbc.evaluation.DBJdbcDebuggerEditorsProvider;
import com.dbn.debugger.jdbc.frame.DBJdbcDebugSuspendContext;
import com.dbn.editor.DBContentType;
import com.dbn.execution.ExecutionContext;
import com.dbn.execution.ExecutionInput;
import com.dbn.object.DBPackage;
import com.dbn.object.DBType;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBObjectVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.debugger.impl.PrioritizedTask.Priority;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.common.notification.NotificationCategory.DEBUGGER;
import static com.dbn.common.thread.ThreadProperty.DEBUGGER_NAVIGATION;
import static com.dbn.common.util.Strings.cachedUpperCase;
import static com.dbn.debugger.common.process.DBDebugProcessStatus.BREAKPOINT_SETTING_ALLOWED;
import static com.dbn.debugger.common.process.DBDebugProcessStatus.PROCESS_STOPPED;
import static com.dbn.debugger.common.process.DBDebugProcessStatus.PROCESS_TERMINATED;
import static com.dbn.debugger.common.process.DBDebugProcessStatus.PROCESS_TERMINATING;
import static com.dbn.debugger.common.process.DBDebugProcessStatus.SESSION_INITIALIZATION_THREW_EXCEPTION;
import static com.dbn.debugger.common.process.DBDebugProcessStatus.TARGET_EXECUTION_STARTED;
import static com.dbn.debugger.common.process.DBDebugProcessStatus.TARGET_EXECUTION_TERMINATED;
import static com.dbn.debugger.common.process.DBDebugProcessStatus.TARGET_EXECUTION_THREW_EXCEPTION;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.execution.ExecutionStatus.CANCELLED;
import static com.dbn.nls.NlsResources.txt;

@Getter
@Setter
public abstract class DBJdbcDebugProcess<T extends ExecutionInput> extends XDebugProcess implements DBDebugProcess, NotificationSupport {
    private static final DebuggerIdentifierModel EMPTY_IDENTIFIER_MODEL = DebuggerIdentifierModel.empty();
    private static final long DEBUGGER_PING_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(30);

    private DBNConnection targetConnection;
    private DBNConnection debuggerConnection;
    private final DBDebugProcessStatusHolder status = new DBDebugProcessStatusHolder();
    private final ConnectionRef connection;
    private final DBBreakpointHandler[] breakpointHandlers;
    private final DBDebugConsoleLogger console;
    private final Map<ObjectKey, DebuggerIdentifierModel> identifierModels = new ConcurrentHashMap<>();

    private transient DebuggerRuntimeInfo runtimeInfo;
    private transient ExecutionBacktraceInfo backtraceInfo;
    private transient Timer debuggerPingTimer;
    private final transient Object debuggerPingLock = new Object();

    public DBJdbcDebugProcess(@NotNull XDebugSession session, ConnectionHandler connection) {
        super(session);
        this.console = new DBDebugConsoleLogger(session);
        this.connection = ConnectionRef.of(connection);
        Project project = session.getProject();
        DatabaseDebuggerManager.getInstance(project).registerDebugSession(connection);

        DBJdbcBreakpointHandler breakpointHandler = new DBJdbcBreakpointHandler(session, this);
        breakpointHandlers = new DBBreakpointHandler[]{breakpointHandler};
    }

    @Override
    public boolean set(DBDebugProcessStatus status, boolean value) {
        return this.status.set(status, value);
    }

    @Override
    public boolean is(DBDebugProcessStatus status) {
        return this.status.is(status);
    }

    @Override
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Override
    @NotNull
    public Project getProject() {
        return getSession().getProject();
    }

    @NotNull
    @Override
    public XDebuggerEditorsProvider getEditorsProvider() {
        return DBJdbcDebuggerEditorsProvider.INSTANCE;
    }

    @NotNull
    public T getExecutionInput() {
        DBRunConfig<T> runProfile = (DBRunConfig) getSession().getRunProfile();
        if (runProfile == null) throw AlreadyDisposedException.INSTANCE;
        return runProfile.getExecutionInput();
    }

    @Override
    public void sessionInitialized() {
        Project project = getProject();
        XDebugSession session = getSession();
        session.setBreakpointMuted(false);

        Progress.background(project, getConnection(), true,
                txt("prc.debugger.title.InitializingDebugEnvironment"),
                txt("prc.debugger.text.StartingDebugger"),
                progress -> {
                    try {
                        T input = getExecutionInput();
                        console.system(txt("log.debugger.info.InitializingEnvironment"));
                        ConnectionHandler connection = getConnection();
                        SchemaId schemaId = input.getExecutionContext().getTargetSchema();

                        ProgressMonitor.setProgressDetail(txt("prc.debugger.text.InitializingTargetConnection"));
                        targetConnection = connection.getDebugConnection(schemaId);
                        targetConnection.setAutoCommit(false);
                        console.system(txt("log.debugger.info.TargetConnectionInitialized"));

                        ProgressMonitor.setProgressDetail(txt("prc.debugger.text.InitializingDebuggerConnection"));
                        debuggerConnection = connection.getDebuggerConnection();
                        console.system(txt("log.debugger.info.DebugConnectionInitialized"));

                        DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();

                        ProgressMonitor.setProgressDetail(txt("prc.debugger.text.InitializingTargetSession"));
                        DebuggerSessionInfo sessionInfo = debuggerInterface.initializeSession(targetConnection);
                        console.system(txt("log.debugger.info.TargetSessionInitialized"));

                        ProgressMonitor.setProgressDetail(txt("prc.debugger.text.EnablingDebuggingOnTargetSession"));
                        debuggerInterface.enableDebugging(targetConnection);
                        console.system(txt("log.debugger.info.DebugEnabled"));


                        ProgressMonitor.setProgressDetail(txt("prc.debugger.text.AttachingDebuggerSession"));
                        debuggerInterface.attachSession(debuggerConnection, sessionInfo.getSessionId());
                        console.system(txt("log.debugger.info.DebugSessionAttached"));

                        synchronizeSession();
                    } catch (SQLException e) {
                        conditionallyLog(e);
                        set(SESSION_INITIALIZATION_THREW_EXCEPTION, true);
                        console.error(txt("log.debugger.error.ErrorInitializingEnvironment", e.getMessage()));
                        session.stop();
                    }
                });
    }

    private void synchronizeSession() {
        Project project = getProject();
        ConnectionHandler connection = getConnection();
        Progress.background(project, connection, false,
                txt("prc.debugger.title.InitializingDebugEnvironment"),
                txt("prc.debugger.text.SynchronizingSessions"),
                progress -> {
                    if (is(PROCESS_TERMINATING) || is(TARGET_EXECUTION_TERMINATED)) {
                        getSession().stop();
                    } else {
                        set(BREAKPOINT_SETTING_ALLOWED, true);
                        progress.setText(txt("prc.debugger.text.RegisteringBreakpoints"));
                        registerBreakpoints(
                                () -> Progress.background(project, connection, false,
                                        txt("prc.debugger.text.StartingDebugger"),
                                        txt("prc.debugger.text.SynchronizingSessions"),
                                        (progress1) -> {
                                            DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
                                            try {
                                                startTargetProgram();
                                                if (isNot(TARGET_EXECUTION_THREW_EXCEPTION) && isNot(TARGET_EXECUTION_TERMINATED)) {
                                                    runtimeInfo = debuggerInterface.synchronizeSession(debuggerConnection);
                                                    runtimeInfo = debuggerInterface.stepOver(debuggerConnection);
                                                    progress.setText(txt("prc.debugger.text.SuspendingSession"));
                                                    console.system(txt("log.debugger.info.DebugSessionSynchronized"));
                                                    suspendSession();
                                                }

                                            } catch (SQLException e) {
                                                conditionallyLog(e);
                                                set(SESSION_INITIALIZATION_THREW_EXCEPTION, true);
                                                console.system(txt("log.debugger.error.ErrorSynchronizingSession", e.getMessage()));
                                                Messages.showErrorDialog(getProject(),
                                                        txt("msg.debugger.error.CouldInitDebugEnvironment",connection.getName()), e);
                                                getSession().stop();
                                            }
                                        }));
                    }
                });
    }

    private void startTargetProgram() {
        Progress.background(getProject(), getConnection(), false,
                txt("prc.debugger.title.RunningDebugger"),
                txt("prc.debugger.text.RunningDebuggerTarget"),
                progress -> {
                    if (is(PROCESS_TERMINATING)) return;
                    if (is(SESSION_INITIALIZATION_THREW_EXCEPTION)) return;
                    T input = getExecutionInput();
                    try {
                        set(TARGET_EXECUTION_STARTED, true);

                        console.system(txt("log.debugger.info.TargetProgramStarted", input.getExecutionContext().getTargetName()));
                        executeTarget();
                        console.system(txt("log.debugger.info.TargetProgramEnded"));
                    } catch (SQLException e) {
                        conditionallyLog(e);
                        set(TARGET_EXECUTION_THREW_EXCEPTION, true);
                        console.error(txt("log.debugger.error.TargetProgramFailed", e.getMessage()));
                        // if the method execution threw exception, the debugger-off statement is not reached,
                        // hence the session will hag as debuggable. To avoid this, disable debugging has
                        // to explicitly be called here

                        // TODO: is this required? the target connection will be dropped anyways
                        //DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
                        //debuggerInterface.disableDebugging(targetConnection);

                        Messages.showErrorDialog(getProject(), txt("msg.debugger.error.TargetExecutionFailed", input.getExecutionContext().getTargetName()), e);
                    } finally {
                        set(TARGET_EXECUTION_TERMINATED, true);
                        getSession().stop();
                    }
                });
    }

    protected abstract void executeTarget() throws SQLException;

    /**
     * breakpoints need to be registered after the database session is started,
     * otherwise they do not get valid ids
     */
    private void registerBreakpoints(Runnable callback) {
        console.system(txt("log.debugger.info.RegisteringBreakpoints"));
        List<XLineBreakpoint<XBreakpointProperties>> breakpoints = getDatabaseBreakpoints();

        getBreakpointHandler().registerBreakpoints(breakpoints, null);
        registerDefaultBreakpoint();
        console.system(txt("log.debugger.info.DoneRegisteringBreakpoints"));
        callback.run();
    }

    protected void registerDefaultBreakpoint() {}

    /**
     * breakpoints need to be unregistered before closing the database session, otherwise they remain resident.
     */
    private void unregisterBreakpoints() {
        Collection<XLineBreakpoint<XBreakpointProperties>> breakpoints = getDatabaseBreakpoints();
        Set<Integer> unregisteredBreakpointIds = new HashSet<>();
        DBBreakpointHandler<?> breakpointHandler = getBreakpointHandler();
        for (XLineBreakpoint<XBreakpointProperties> breakpoint : breakpoints) {
            Integer breakpointId = DBBreakpointUtil.getBreakpointId(breakpoint);
            if (breakpointId == null) continue;

            if (!unregisteredBreakpointIds.contains(breakpointId)) {
                breakpointHandler.unregisterBreakpoint(breakpoint, false);
                unregisteredBreakpointIds.add(breakpointId);
            }
            DBBreakpointUtil.setBreakpointId(breakpoint, null);

        }
        breakpointHandler.unregisterDefaultBreakpoint();
    }

    private List<XLineBreakpoint<XBreakpointProperties>> getDatabaseBreakpoints() {
        return DBBreakpointUtil.getDatabaseBreakpoints(getConnection(), DBDebuggerType.JDBC);
    }

    @Override
    public synchronized void stop() {
        if (canStopDebugger()) {
            set(PROCESS_TERMINATING, true);
            console.system(txt("log.debugger.info.StoppingDebugger"));
            T input = getExecutionInput();
            ExecutionContext<?> context = input.getExecutionContext();
            context.set(CANCELLED, isNot(PROCESS_STOPPED));
            stopDebugger();
        }
    }

    private boolean canStopDebugger() {
        return isNot(PROCESS_TERMINATED) && isNot(PROCESS_TERMINATING);
    }

    private void stopDebugger() {
        stopDebuggerPing();
        Project project = getProject();
        ConnectionHandler connection = getConnection();
        Progress.background(project, connection, false,
                txt("prc.debugger.title.StoppingDebugger"),
                txt("prc.debugger.text.StoppingDebugEnvironment"),
                progress -> {
                    try {
                        unregisterBreakpoints();
                        set(BREAKPOINT_SETTING_ALLOWED, false);
                        rollOutDebugger();

                        DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
                        if (debuggerConnection != null) {
                            if (isNot(TARGET_EXECUTION_TERMINATED)) {
                                runtimeInfo = debuggerInterface.stopExecution(debuggerConnection);
                            }
                            debuggerInterface.detachSession(debuggerConnection);
                        }
                        console.system(txt("log.debugger.info.DebuggerSessionDetached"));
                    } catch (SQLException e) {
                        conditionallyLog(e);
                        console.error(txt("log.debugger.error.ErrorDetachingDebuggerSession", e.getMessage()));
                    } finally {
                        set(PROCESS_TERMINATED, true);
                        releaseDebugConnection();
                        releaseTargetConnection();

                        DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
                        debuggerManager.unregisterDebugSession(connection);
                        console.system(txt("log.debugger.info.DebuggerStopped"));
                    }
                });
    }

    private void releaseDebugConnection() {
        stopDebuggerPing();
        Resources.close(debuggerConnection);
        debuggerConnection = null;
    }

    protected void releaseTargetConnection() {
        Resources.close(targetConnection);
        targetConnection = null;
    }

    @Override
    public void startStepOver(@Nullable XSuspendContext suspendContext) {
        DBDebugOperation.run(getProject(), txt("ntf.debugger.token.Operation_STEP_OVER"), () -> {
            console.system(txt("log.debugger.info.DebuggerSessionResumed"));
            stopDebuggerPing();
            DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
            runtimeInfo = debuggerInterface.stepOver(debuggerConnection);
            suspendSession();
        });
    }

    @Override
    public void startStepInto(@Nullable XSuspendContext suspendContext) {
        DBDebugOperation.run(getProject(), txt("ntf.debugger.token.Operation_STEP_INTO"), () -> {
            console.system(txt("log.debugger.info.DebuggerSessionResumed"));
            stopDebuggerPing();
            DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
            runtimeInfo = debuggerInterface.stepInto(debuggerConnection);
            suspendSession();
        });
    }


    @Override
    public void startStepOut(@Nullable XSuspendContext suspendContext) {
        DBDebugOperation.run(getProject(), txt("ntf.debugger.token.Operation_STEP_OUT"), () -> {
            console.system(txt("log.debugger.info.DebuggerSessionResumed"));
            stopDebuggerPing();
            DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
            runtimeInfo = debuggerInterface.stepOut(debuggerConnection);
            suspendSession();
        });
    }

    @Override
    public void resume(@Nullable XSuspendContext suspendContext) {
        DBDebugOperation.run(getProject(), txt("ntf.debugger.token.Operation_RESUME_EXECUTION"), () -> {
            console.system(txt("log.debugger.info.DebuggerSessionResumed"));
            stopDebuggerPing();
            DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
            runtimeInfo = debuggerInterface.resumeExecution(debuggerConnection);
            suspendSession();
        });
    }

    @Override
    public void runToPosition(@NotNull XSourcePosition position, @Nullable XSuspendContext suspendContext) {
        DBDebugOperation.run(getProject(), txt("ntf.debugger.token.Operation_RUN_TO_POSITION"), () -> {
            console.system(txt("log.debugger.info.DebuggerSessionResumed"));
            stopDebuggerPing();
            DBSchemaObject object = DBDebugUtil.getObject(position);
            if (object != null) {
                DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
                runtimeInfo = debuggerInterface.runToPosition(
                        object.getSchema().getName(),
                        object.getName(),
                        cachedUpperCase(object.getObjectType().getName()),
                        position.getLine(),
                        debuggerConnection);
            }

            suspendSession();
        });
    }

    @NotNull
    @Override
    public XDebugTabLayouter createTabLayouter() {
        return new DBDebugTabLayouter(getSession());
    }

    @Override
    public void startPausing() {
        // NOT SUPPORTED!!!
        DBDebugOperation.run(getProject(), txt("ntf.debugger.token.Operation_RUN_TO_POSITION"), () -> {
            stopDebuggerPing();
            DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
            runtimeInfo = debuggerInterface.synchronizeSession(debuggerConnection);
            suspendSession();
        });
    }

    private void showErrorDialog(SQLException e) {
        Messages.showErrorDialog(getProject(), txt("msg.debugger.error.CouldNotPerformOperation"), e);
    }

    @ThreadContext(DEBUGGER_NAVIGATION)
    private void suspendSession() {
        if (is(PROCESS_TERMINATING) || is(PROCESS_TERMINATED)) return;

        XDebugSession session = getSession();
        DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
        if (isTerminated()) {
            stopDebuggerPing();
            int reasonCode = runtimeInfo.getReason();
            String reason = debuggerInterface.getRuntimeEventReason(reasonCode);
            sendInfoNotification(DEBUGGER, txt("ntf.debugger.info.SessionTerminated", reasonCode, reason));

            set(PROCESS_STOPPED, true);
            session.stop();
        } else {
            String location = getSuspensionLocation(runtimeInfo);
            String lineNumber = getSuspensionLineNumber(runtimeInfo);
            String suspensionReason = debuggerInterface.getRuntimeEventReason(runtimeInfo.getReason());
            console.system(txt("log.debugger.info.DebuggerSessionSuspendedAt", location, lineNumber, suspensionReason));

            startDebuggerPing();
            VirtualFile virtualFile = getRuntimeInfoFile(runtimeInfo);
            DBDebugUtil.openEditor(virtualFile);
            try {
                backtraceInfo = debuggerInterface.getExecutionBacktraceInfo(debuggerConnection);
                List<DebuggerRuntimeInfo> frames = backtraceInfo.getFrames();
                if (!frames.isEmpty()) {
                    DebuggerRuntimeInfo topRuntimeInfo = frames.get(0);
                    if (runtimeInfo.isTerminated()) {
                        int reasonCode = runtimeInfo.getReason();
                        String terminationReason = debuggerInterface.getRuntimeEventReason(reasonCode);
                        sendInfoNotification(DEBUGGER, txt("ntf.debugger.info.SessionTerminated", reasonCode, terminationReason));
                    }
                    if (!runtimeInfo.isSameLocation(topRuntimeInfo)) {
                        runtimeInfo = topRuntimeInfo;
                        resume(null);
                        return;
                    }
                }
            } catch (SQLException e) {
                conditionallyLog(e);
                console.error(txt("log.debugger.error.ErrorSuspendingDebuggerSession", e.getMessage()));
            }

            if (backtraceInfo != null) {
                preloadIdentifierModels(backtraceInfo.getFrames());
            }

            DBJdbcDebugSuspendContext suspendContext = new DBJdbcDebugSuspendContext(this);
            session.positionReached(suspendContext);
            //navigateInEditor(virtualFile, runtimeInfo.getLineNumber());
        }
    }

    protected boolean isTerminated() {
        return runtimeInfo.isTerminated();
    }

    private static String getSuspensionLocation(DebuggerRuntimeInfo runtimeInfo) {
        String ownerName = runtimeInfo.getOwnerName();
        String programName = runtimeInfo.getProgramName();
        if (Strings.isNotEmpty(ownerName) && Strings.isNotEmpty(programName)) {
            return ownerName + "." + programName;
        }
        if (Strings.isNotEmpty(programName)) return programName;
        if (Strings.isNotEmpty(ownerName)) return ownerName;
        return "?";
    }

    private static String getSuspensionLineNumber(DebuggerRuntimeInfo runtimeInfo) {
        Integer lineNumber = runtimeInfo.getLineNumber();
        return lineNumber == null ? "?" : Integer.toString(lineNumber + 1);
    }

    protected DBBreakpointHandler<?> getBreakpointHandler() {
        return getBreakpointHandlers()[0];
    }

    @Nullable
    public VirtualFile getRuntimeInfoFile(DebuggerRuntimeInfo runtimeInfo) {
        DBSchemaObject schemaObject = getDatabaseObject(runtimeInfo);
        if (schemaObject != null) {
            DBObjectVirtualFile virtualFile = schemaObject.getVirtualFile();
            if (virtualFile instanceof DBEditableObjectVirtualFile editableObjectFile) {
                DBContentType contentType = schemaObject.getContentType();
                if (contentType == DBContentType.CODE_SPEC_AND_BODY) {
                    return editableObjectFile.getContentFile(DBContentType.CODE_BODY);
                } else if (contentType.isOneOf(DBContentType.CODE, DBContentType.CODE_AND_DATA)) {
                    return editableObjectFile.getContentFile(DBContentType.CODE);
                }

            }
        }
        return null;
    }

    @Nullable
    protected DBSchemaObject getDatabaseObject(DebuggerRuntimeInfo runtimeInfo) {
        String ownerName = runtimeInfo.getOwnerName();
        String programName = runtimeInfo.getProgramName();

        if (Strings.isNotEmpty(ownerName) && Strings.isNotEmpty(programName)) {
            ConnectionHandler connection = getConnection();
            DBObjectBundle objectBundle = connection.getObjectBundle();
            DBSchema schema = Failsafe.nn(objectBundle.getSchema(ownerName));
            DBSchemaObject schemaObject = schema.getProgram(programName);
            if (schemaObject == null) schemaObject = schema.getMethod(programName, (short) 0); // overload 0 is assuming debug is only supported in oracle (no schema method overloading)
            return schemaObject;
        }
        return null;
    }

    private void rollOutDebugger() {
        stopDebuggerPing();
        try {
            long millis = System.currentTimeMillis();
            while (isNot(TARGET_EXECUTION_THREW_EXCEPTION) && runtimeInfo != null && !runtimeInfo.isTerminated()) {
                runtimeInfo = getDebuggerInterface().stepOut(debuggerConnection);
                // force closing the target connection
                if (System.currentTimeMillis() - millis > 20000) {
                    break;
                }
            }
        } catch (SQLException e) {
            conditionallyLog(e);
            console.error(txt("log.debugger.error.ErrorStoppingDebuggerSession", e.getMessage()));
        }
    }

    private void startDebuggerPing() {
        synchronized (debuggerPingLock) {
            stopDebuggerPing();
            if (debuggerConnection == null) return;
            if (runtimeInfo == null) return;
            if (runtimeInfo.isTerminated()) return;
            if (is(PROCESS_TERMINATING)) return;
            if (is(PROCESS_TERMINATED)) return;

            debuggerPingTimer = new Timer("DBN - Debugger Session Ping", true);
            pingDebuggerSession();
            debuggerPingTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    pingDebuggerSession();
                }
            }, DEBUGGER_PING_INTERVAL_MILLIS, DEBUGGER_PING_INTERVAL_MILLIS);
        }
    }

    private void stopDebuggerPing() {
        synchronized (debuggerPingLock) {
            if (debuggerPingTimer != null) {
                debuggerPingTimer.cancel();
                debuggerPingTimer = null;
            }
        }
    }

    private void pingDebuggerSession() {
        synchronized (debuggerPingLock) {
            if (debuggerPingTimer == null) return;
            if (debuggerConnection == null) return;
            if (is(PROCESS_TERMINATING)) return;
            if (is(PROCESS_TERMINATED)) return;

        long startTimestamp = System.currentTimeMillis();
        try {
            getDebuggerInterface().pingSession(debuggerConnection);
            console.system(txt("log.debugger.info.DebuggerKeepAliveCompleted"));
        } catch (SQLException e) {
                conditionallyLog(e);
                long elapsedMillis = System.currentTimeMillis() - startTimestamp;
                console.error(txt("log.debugger.error.ErrorDebuggerKeepAlive", elapsedMillis, e.getMessage()));
            }
        }
    }

/*    private void navigateInEditor(final VirtualFile virtualFile, final int line) {
        SimpleLaterInvocator.invoke(() -> {
            Project project = getProject();
            LogicalPosition position = new LogicalPosition(line, 0);
            if (virtualFile instanceof DBEditableObjectVirtualFile) {
                DBEditableObjectVirtualFile objectVirtualFile = (DBEditableObjectVirtualFile) virtualFile;
                // todo review this
                SourceCodeEditor sourceCodeEditor = null;
                DBSourceCodeVirtualFile mainContentFile = (DBSourceCodeVirtualFile) objectVirtualFile.getMainContentFile();
                if (objectVirtualFile.getContentFiles().size() > 1) {
                    FileEditorManager editorManager = FileEditorManager.getInstance(project);
                    FileEditor[] fileEditors = editorManager.getEditors(objectVirtualFile);
                    if (fileEditors.length >= runtimeInfo.getNamespace()) {
                        FileEditor fileEditor = fileEditors[runtimeInfo.getNamespace() -1];
                        sourceCodeEditor = (SourceCodeEditor) fileEditor;
                        objectVirtualFile.FAKE_DOCUMENT.set(sourceCodeEditor.getEditor().getDocument());
                    } else {
                        FileEditor fileEditor = EditorUtil.getTextEditor(mainContentFile);
                        if (fileEditor instanceof SourceCodeEditor) {
                            sourceCodeEditor = (SourceCodeEditor) fileEditor;
                        }
                    }
                } else {
                    FileEditor fileEditor = EditorUtil.getTextEditor(mainContentFile);
                    if (fileEditor instanceof SourceCodeEditor) {
                        sourceCodeEditor = (SourceCodeEditor) fileEditor;
                    }
                }
                if (sourceCodeEditor != null) {
                    EditorUtil.selectEditor(project, sourceCodeEditor, objectVirtualFile, sourceCodeEditor.getEditorProviderId(), NavigationInstruction.FOCUS_SCROLL);
                    sourceCodeEditor.getEditor().getScrollingModel().scrollTo(position, ScrollType.CENTER);
                }
            } else if (virtualFile instanceof DBSourceCodeVirtualFile) {
                DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) virtualFile;
                DBEditableObjectVirtualFile objectVirtualFile = sourceCodeFile.getMainDatabaseFile();
                FileEditorManager editorManager = FileEditorManager.getInstance(project);
                FileEditor[] fileEditors = editorManager.getEditors(objectVirtualFile);
                for (FileEditor fileEditor : fileEditors) {
                    VirtualFile editorFile = fileEditor.getFile();
                    if (editorFile != null && editorFile.equals(sourceCodeFile)) {
                        System.out.println();
                        break;
                    }
                }

            }
            else if (virtualFile instanceof DBVirtualFile){
                FileEditorManager editorManager = FileEditorManager.getInstance(project);
                FileEditor[] fileEditors = editorManager.openFile(virtualFile, true);
                for (FileEditor fileEditor : fileEditors) {
                    if (fileEditor instanceof BasicTextEditor) {
                        BasicTextEditor textEditor = (BasicTextEditor) fileEditor;
                        textEditor.getEditor().getScrollingModel().scrollTo(position, ScrollType.CENTER);
                        break;
                    }
                }
            }
        });
    }*/

/*
    TODO remove (old compatibility code)
    @Override public void startStepOver() {startStepOver(null);}
    @Override public void startStepInto() {startStepInto(null);}
    @Override public void startStepOut() {startStepOut(null);}
    @Override public void resume() {resume(null);}
    @Override public void runToPosition(@NotNull XSourcePosition position) {runToPosition(position, null);}
*/

    @Override
    public DatabaseDebuggerInterface getDebuggerInterface() {
        return getConnection().getInterfaces().getDebuggerInterface();
    }

    /**
     * Returns the cached PL/Scope identifier model for an object, shared by all stack frames in this debug run.
     * Identifier models are preloaded before the suspend context is published.
     *
     * @param object the database object whose identifiers should be loaded
     * @param contentType the source content type, such as {@link DBContentType#CODE_BODY}
     */
    public DebuggerIdentifierModel getIdentifierModel(DBSchemaObject object, DBContentType contentType) {
        ObjectKey key = ObjectKey.create(object.getSchemaName(), object.getName(), contentType);
        return identifierModels.getOrDefault(key, EMPTY_IDENTIFIER_MODEL);
    }

    private void preloadIdentifierModels(List<DebuggerRuntimeInfo> frames) {
        for (DebuggerRuntimeInfo frame : frames) {
            DBSchemaObject object = getDatabaseObject(frame);
            if (object == null) continue;

            VirtualFile sourceFile = getRuntimeInfoFile(frame);
            DBContentType contentType = sourceFile instanceof DBSourceCodeVirtualFile sourceCodeFile
                    ? sourceCodeFile.getContentType()
                    : null;
            DebuggerIdentifierModel model = preloadIdentifierModel(object, contentType);
            model.preloadTypes(identifier -> resolveType(object, identifier));
        }
    }

    private DebuggerIdentifierModel preloadIdentifierModel(DBSchemaObject object, DBContentType contentType) {
        ObjectKey key = ObjectKey.create(object.getSchemaName(), object.getName(), contentType);
        return identifierModels.computeIfAbsent(key, ignored -> loadIdentifierModel(object, contentType));
    }

    @Nullable
    private DBType resolveType(DBSchemaObject object, DebuggerIdentifierInfo typeIdentifier) {
        String typeName = typeIdentifier.getTypeName();
        if (typeName == null) return null;

        String typeOwner = typeIdentifier.getDeclaredOwner();
        DBSchema schema = typeOwner == null
                ? object.getSchema()
                : getConnection().getObjectBundle().getSchema(typeOwner);
        if (schema == null) return null;

        String packageName = typeIdentifier.getTypePackageName();
        return packageName == null
                ? schema.getType(typeName)
                : getPackageType(schema, packageName, typeName);
    }

    @Nullable
    private DBType getPackageType(DBSchema schema, String packageName, String typeName) {
        DBPackage packageObject = schema.getPackage(packageName);
        return packageObject == null ? null : packageObject.getType(typeName);
    }

    public String getIdentifierObjectType(DBSchemaObject object, DBContentType contentType) {
        String contentQualifier = contentType == null ? null : contentType.getContentQualifier(object.getObjectType());
        return contentQualifier == null ? cachedUpperCase(object.getObjectType().getName()) : contentQualifier;
    }

    private DebuggerIdentifierModel loadIdentifierModel(DBSchemaObject object, DBContentType contentType) {
        String objectType = getIdentifierObjectType(object, contentType);
        try {
            return DatabaseInterfaceInvoker.load(
                    HIGH,
                    getProject(),
                    getConnection().getConnectionId(),
                    connection -> {
                        DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
                        List<DebuggerIdentifierInfo> identifiers = debuggerInterface.loadObjectIdentifiers(
                                object.getSchemaName(),
                                object.getName(),
                                objectType,
                                connection);
                        return new DebuggerIdentifierModel(identifiers);
                    });
        } catch (SQLException e) {
            conditionallyLog(e);
            return DebuggerIdentifierModel.empty();
        }
    }

    @Nullable
    @Override
    public String getDescription() {
        return "Database Debug Process";
    }

    @Override
    public void queueCommand(Priority priority, Runnable command) {
        throw new UnsupportedOperationException("No managed thread support");
    }
}
