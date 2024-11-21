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

package com.dbn.debugger.jdwp.process;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.exception.ProcessDeferredException;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.ThreadMonitor;
import com.dbn.common.thread.ThreadProperty;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.Resources;
import com.dbn.connection.SchemaId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.dbn.debugger.DBDebugConsoleLogger;
import com.dbn.debugger.DBDebugOperation;
import com.dbn.debugger.DBDebugUtil;
import com.dbn.debugger.DatabaseDebuggerManager;
import com.dbn.debugger.common.breakpoint.DBBreakpointHandler;
import com.dbn.debugger.common.breakpoint.DBBreakpointUtil;
import com.dbn.debugger.common.config.DBRunConfig;
import com.dbn.debugger.common.process.DBDebugProcess;
import com.dbn.debugger.common.process.DBDebugProcessStatus;
import com.dbn.debugger.common.process.DBDebugProcessStatusHolder;
import com.dbn.debugger.jdwp.DBJdwpBreakpointHandler;
import com.dbn.debugger.jdwp.DBJdwpSourcePath;
import com.dbn.debugger.jdwp.ManagedThreadCommand;
import com.dbn.debugger.jdwp.frame.DBJdwpDebugStackFrame;
import com.dbn.debugger.jdwp.frame.DBJdwpDebugSuspendContext;
import com.dbn.editor.DBContentType;
import com.dbn.execution.ExecutionContext;
import com.dbn.execution.ExecutionInput;
import com.dbn.object.DBMethod;
import com.dbn.object.DBProgram;
import com.dbn.object.DBSchema;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.debugger.DebuggerManager;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.DebugProcessListener;
import com.intellij.debugger.engine.JavaDebugProcess;
import com.intellij.debugger.engine.JavaStackFrame;
import com.intellij.debugger.engine.SuspendContext;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.sun.jdi.Location;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.intellij.debugger.impl.PrioritizedTask.Priority.LOW;

@Slf4j
public abstract class DBJdwpDebugProcess<T extends ExecutionInput>
        extends JavaDebugProcess
        implements DBDebugProcess {

    public static final Key<DBJdwpDebugProcess> KEY = new Key<>("DBNavigator.JdwpDebugProcess");
    private final ConnectionRef connection;
    private final DBDebugProcessStatusHolder status = new DBDebugProcessStatusHolder();
    private final DBBreakpointHandler<DBJdwpDebugProcess>[] breakpointHandlers;
    private final DBDebugConsoleLogger console;
    private final String declaredBlockIdentifier;
    private final DBJdwpTcpConfig tcpConfig;


    protected DBNConnection targetConnection;
    private transient XSuspendContext lastSuspendContext;

    protected DBJdwpDebugProcess(@NotNull XDebugSession session, DebuggerSession debuggerSession, ConnectionHandler connection, DBJdwpTcpConfig tcpConfig) {
        super(session, debuggerSession);
        this.console = new DBDebugConsoleLogger(session);
        this.connection = ConnectionRef.of(connection);
        this.tcpConfig = tcpConfig;

        Project project = session.getProject();
        DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(project);
        debuggerManager.registerDebugSession(connection);

        DBJdwpBreakpointHandler breakpointHandler = new DBJdwpBreakpointHandler(session, this);
        this.breakpointHandlers = new DBBreakpointHandler[]{breakpointHandler};
        debuggerSession.getProcess().putUserData(KEY, this);

        DatabaseDebuggerInterface debuggerInterface = connection.getDebuggerInterface();
        declaredBlockIdentifier = debuggerInterface.getJdwpBlockIdentifier().replace(".", "\\");
    }

    @Override
    public boolean set(DBDebugProcessStatus status, boolean value) {
        return this.status.set(status, value);
    }

    @Override
    public boolean is(DBDebugProcessStatus status) {
        return this.status.is(status);
    }

    protected boolean shouldSuspend(XSuspendContext suspendContext) {
        if (is(DBDebugProcessStatus.TARGET_EXECUTION_TERMINATED)) {
            return false;
        } else {
            XExecutionStack executionStack = suspendContext.getActiveExecutionStack();
            if (executionStack != null) {
                XStackFrame topFrame = executionStack.getTopFrame();
                if (topFrame instanceof DBJdwpDebugStackFrame) {
                    return true;
                }
                Location location = getLocation(topFrame);
                VirtualFile virtualFile = getVirtualFile(location);
                return virtualFile != null;
            }
        }
        return true;
    }

    @Override
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Nullable
    public T getExecutionInput() {
        DBRunConfig<T> runProfile = getRunProfile();
        return runProfile == null ? null : runProfile.getExecutionInput();
    }

    DBRunConfig<T> getRunProfile() {
        return (DBRunConfig<T>) getSession().getRunProfile();
    }

    @Override
    @NotNull
    public Project getProject() {
        return getSession().getProject();
    }

    @Override
    public DatabaseDebuggerInterface getDebuggerInterface() {
        return getConnection().getDebuggerInterface();
    }

    @NotNull
    public DBNConnection getTargetConnection() {
        return Failsafe.nn(targetConnection);
    }

    @NotNull
    @Override
    public DBBreakpointHandler<DBJdwpDebugProcess>[] getBreakpointHandlers() {
        return breakpointHandlers;
    }

    public DBBreakpointHandler<DBJdwpDebugProcess> getBreakpointHandler() {
        return breakpointHandlers[0];
    }

    @Override
    public boolean checkCanInitBreakpoints() {
        return is(DBDebugProcessStatus.BREAKPOINT_SETTING_ALLOWED);
    }

    @Override
    public DBDebugConsoleLogger getConsole() {
        return console;
    }

    @Override
    public void sessionInitialized() {
        XDebugSession session = getSession();
        if (session instanceof XDebugSessionImpl) {
            XDebugSessionImpl sessionImpl = (XDebugSessionImpl) session;
            sessionImpl.getSessionData().setBreakpointsMuted(false);
        }
        DBRunConfig<T> runProfile = getRunProfile();
        List<DBMethod> methods = runProfile.getMethods();
        if (!methods.isEmpty()) {
            getBreakpointHandler().registerDefaultBreakpoint(methods.get(0));
        }

        DebuggerSession debuggerSession = getDebuggerSession();
        final Project project = getProject();
        DebuggerManager debuggerManager = DebuggerManager.getInstance(project);
        ProcessHandler processHandler = debuggerSession.getProcess().getProcessHandler();
        debuggerManager.addDebugProcessListener(processHandler, new DebugProcessListener(){
            @Override
            public void paused(@NotNull SuspendContext suspendContext) {
                if (suspendContext instanceof XSuspendContext) {
                    XSuspendContext xSuspendContext = (XSuspendContext) suspendContext;

                    XExecutionStack[] executionStacks = xSuspendContext.getExecutionStacks();
                    for (XExecutionStack executionStack : executionStacks) {
                        //System.out.println();
                    }

                    //underlyingFrame.getDescriptor().getLocation()

                }
            }
        });

        session.addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionPaused() {
                XSuspendContext suspendContext = session.getSuspendContext();
                if (!shouldSuspend(suspendContext)) {
                    Dispatch.run(() -> session.resume());
                } else {
                    XExecutionStack activeExecutionStack = suspendContext.getActiveExecutionStack();
                    if (activeExecutionStack != null) {
                        XStackFrame topFrame = activeExecutionStack.getTopFrame();
                        if (topFrame instanceof JavaStackFrame) {
                            Location location = getLocation(topFrame);
                            VirtualFile virtualFile = getVirtualFile(location);
                            ThreadMonitor.surround(project, ThreadProperty.DEBUGGER_NAVIGATION, () -> DBDebugUtil.openEditor(virtualFile));
                        }
                    }
                }
            }
        });

        debuggerSession.getContextManager().addListener((newContext, event) -> {
            SuspendContextImpl suspendContext = newContext.getSuspendContext();
            overwriteSuspendContext(suspendContext);
        });

        getDebuggerSession().getProcess().setXDebugProcess(this);

        DBDebugOperation.run(project, "initialize debug environment", () -> {
            try {
                console.system("Initializing debug environment");
                T input = getExecutionInput();
                if (input != null) {
                    ConnectionHandler connection = getConnection();
                    SchemaId schemaId = input.getExecutionContext().getTargetSchema();
                    targetConnection = connection.getDebugConnection(schemaId);
                    targetConnection.setAutoCommit(false);
                    targetConnection.beforeClose(() -> releaseSession(targetConnection));


                    if (tcpConfig.isLocal()) {
                        String tcpHost = tcpConfig.getHost();
                        int tcpPort = tcpConfig.getPort();
                        console.info("Initializing debug session on address " + tcpHost + ":" + tcpPort);

                        DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
                        debuggerInterface.initializeJdwpSession(targetConnection, tcpHost, String.valueOf(tcpPort));
                    }
                    console.system("Debug session initialized (JDWP)");
                    set(DBDebugProcessStatus.BREAKPOINT_SETTING_ALLOWED, true);

                    initializeBreakpoints();
                    startTargetProgram();
                }
            } catch (Exception e) {
                conditionallyLog(e);
                set(DBDebugProcessStatus.SESSION_INITIALIZATION_THREW_EXCEPTION, true);
                console.error("Error initializing debug environment\n" + e.getMessage());
                stop();
            }
        });
    }

    private void initializeBreakpoints() {
        console.system("Registering breakpoints...");
        List<DBMethod> methods = getRunProfile().getMethods();
        List<XLineBreakpoint<XBreakpointProperties>> breakpoints = DBBreakpointUtil.getDatabaseBreakpoints(getConnection());
        getBreakpointHandler().registerBreakpoints(breakpoints, methods);
    }

    private void overwriteSuspendContext(final @Nullable XSuspendContext suspendContext) {
        if (suspendContext != null && suspendContext != lastSuspendContext && !(suspendContext instanceof DBJdwpDebugSuspendContext)) {
            DebugProcessImpl debugProcess = getDebuggerSession().getProcess();
            ManagedThreadCommand.schedule(debugProcess, LOW, () -> {
                lastSuspendContext = suspendContext;
                XDebugSession session = getSession();
                if (shouldSuspend(suspendContext)) {
                    DBJdwpDebugSuspendContext dbSuspendContext = new DBJdwpDebugSuspendContext(DBJdwpDebugProcess.this, suspendContext);
                    session.positionReached(dbSuspendContext);
                }
            });
            throw new ProcessDeferredException();
        }
    }

    private void startTargetProgram() {
        // trigger in managed thread
        DebugProcessImpl debugProcess = getDebuggerSession().getProcess();
        T input = getExecutionInput();
        ManagedThreadCommand.schedule(debugProcess, LOW, () -> {
            Progress.background(getProject(), getConnection(), false,
                    "Running debugger target program",
                    "Executing " + (input == null ? " target program" : input.getExecutionContext().getTargetName()),
                    progress -> {
                        console.system("Executing target program...");
                        if (is(DBDebugProcessStatus.SESSION_INITIALIZATION_THREW_EXCEPTION)) return;
                        try {
                            set(DBDebugProcessStatus.TARGET_EXECUTION_STARTED, true);
                            executeTarget();
                        } catch (SQLException e) {
                            conditionallyLog(e);
                            set(DBDebugProcessStatus.TARGET_EXECUTION_THREW_EXCEPTION, true);
                            if (isNot(DBDebugProcessStatus.DEBUGGER_STOPPING)) {
                                String message = input == null ? "Error executing target program" : "Error executing " + input.getExecutionContext().getTargetName();
                                console.error(message + ": " + e.getMessage());
                            }
                        } finally {
                            set(DBDebugProcessStatus.TARGET_EXECUTION_TERMINATED, true);
                            stop();
                        }
                    });
        });
    }

    protected abstract void executeTarget() throws SQLException;

    @Override
    public void stop() {
        if (canStopDebugger()) {
            synchronized (this) {
                if (canStopDebugger()) {
                    set(DBDebugProcessStatus.DEBUGGER_STOPPING, true);
                    set(DBDebugProcessStatus.BREAKPOINT_SETTING_ALLOWED, false);
                    console.system("Stopping debugger...");
                    getSession().stop();
                    stopDebugger();
                    super.stop();
                }
            }
        }
    }

    private boolean canStopDebugger() {
        return isNot(DBDebugProcessStatus.DEBUGGER_STOPPING) && isNot(DBDebugProcessStatus.DEBUGGER_STOPED);
    }

    private void stopDebugger() {
        Progress.background(getProject(), getConnection(), false,
                "Stopping debugger",
                "Stopping debugger session",
                progress -> {
                    T input = getExecutionInput();
                    if (input != null && isNot(DBDebugProcessStatus.TARGET_EXECUTION_TERMINATED)) {
                        ExecutionContext<?> context = input.getExecutionContext();
                        Resources.cancel(context.getStatement());
                    }

                    ConnectionHandler connection = getConnection();

                    DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(getProject());
                    debuggerManager.unregisterDebugSession(connection);
                    releaseTargetConnection();
                    console.system("Debugger stopped");
                    set(DBDebugProcessStatus.DEBUGGER_STOPED, false);
                    set(DBDebugProcessStatus.DEBUGGER_STOPPING, false);
                });
    }

    private void releaseSession(DBNConnection targetConnection) {
        try {
            console.system("Releasing debug session...");
            DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
            debuggerInterface.disconnectJdwpSession(targetConnection);

        } catch (Throwable e) {
            conditionallyLog(e);
            console.error("Error releasing debug session: " + e.getMessage());
        }
    }



    protected void releaseTargetConnection() {
        console.system("Releasing target connection...");
        Resources.close(targetConnection);
        targetConnection = null;
    }

    @Nullable
    public VirtualFile getVirtualFile(Location location) {
        if (location == null) return null;

        String sourceUrl = "<NULL>";
        try {
            sourceUrl = location.sourcePath();
            DBJdwpSourcePath sourcePath = DBJdwpSourcePath.from(sourceUrl);
            String programType = sourcePath.getProgramType();
            if (!Objects.equals(programType, "Block")) {
                String schemaName = sourcePath.getProgramOwner();
                String programName = sourcePath.getProgramName();
                DBSchema schema = getConnection().getObjectBundle().getSchema(schemaName);
                if (schema != null) {
                    DBProgram program = schema.getProgram(programName);
                    if (program != null) {
                        DBEditableObjectVirtualFile editableVirtualFile = program.getEditableVirtualFile();
                        DBContentType contentType = Objects.equals(programType, "PackageBody") ? DBContentType.CODE_BODY : DBContentType.CODE_SPEC;
                        return editableVirtualFile.getContentFile(contentType);
                    } else {
                        DBMethod method = schema.getMethod(programName, (short) 0);
                        if (method != null) {
                            return method.getEditableVirtualFile().getContentFile(DBContentType.CODE);
                        }
                    }
                }
            }
        } catch (Exception e) {
            conditionallyLog(e);
            getConsole().warning("Error evaluating suspend position '" + sourceUrl + "': " + Commons.nvl(e.getMessage(), e.getClass().getSimpleName()));
        }
        return null;
    }

    public boolean isDeclaredBlock(@Nullable Location location) {
        if (location == null) return false;
        if (Strings.isEmptyOrSpaces(declaredBlockIdentifier)) return false;

        try {
            String sourcePath = location.sourcePath();
            return sourcePath.startsWith(declaredBlockIdentifier);
        } catch (Exception e) {
            conditionallyLog(e);
            log.warn("Failed to evaluate declared block", e);
        }

        return false;
    }



    @Nullable
    private Location getLocation(@Nullable XStackFrame stackFrame) {
        if (stackFrame instanceof JavaStackFrame) {
            return ((JavaStackFrame) stackFrame).getDescriptor().getLocation();
        }
        return null;
    }
}
