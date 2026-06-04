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
import com.dbn.common.network.NetworkAddress;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.ThreadPropertyGate;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.Resources;
import com.dbn.connection.SchemaId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.ssh.SshTunnelConfig;
import com.dbn.connection.ssh.SshTunnelConnector;
import com.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.dbn.debugger.DBDebugConsoleLogger;
import com.dbn.debugger.DBDebugOperation;
import com.dbn.debugger.DBDebugUtil;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.debugger.DatabaseDebuggerManager;
import com.dbn.debugger.JDWPTunnelType;
import com.dbn.debugger.common.breakpoint.DBBreakpointHandler;
import com.dbn.debugger.common.breakpoint.DBBreakpointUtil;
import com.dbn.debugger.common.config.DBRunConfig;
import com.dbn.debugger.common.process.DBDebugProcess;
import com.dbn.debugger.common.process.DBDebugProcessStatus;
import com.dbn.debugger.common.process.DBDebugProcessStatusHolder;
import com.dbn.debugger.jdwp.DBJdwpBreakpointHandler;
import com.dbn.debugger.jdwp.DBJdwpSourcePath;
import com.dbn.debugger.jdwp.DBJdwpSourcePathCache;
import com.dbn.debugger.jdwp.ManagedThreadCommand;
import com.dbn.debugger.jdwp.frame.DBJdwpDebugExecutionStack;
import com.dbn.debugger.jdwp.frame.DBJdwpDebugStackFrame;
import com.dbn.debugger.jdwp.frame.DBJdwpDebugSuspendContext;
import com.dbn.execution.ExecutionContext;
import com.dbn.execution.ExecutionInput;
import com.dbn.object.DBMethod;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.debugger.DebuggerManager;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.DebugProcessListener;
import com.intellij.debugger.engine.JavaDebugProcess;
import com.intellij.debugger.engine.JavaStackFrame;
import com.intellij.debugger.engine.SuspendContext;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.impl.DebuggerContextListener;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.debugger.impl.DebuggerStateManager;
import com.intellij.debugger.impl.PrioritizedTask;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl;
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
import com.sun.jdi.Location;
import com.sun.jdi.StackFrame;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.thread.ThreadProperty.DEBUGGER_NAVIGATION;
import static com.dbn.common.util.Classes.simpleClassName;
import static com.dbn.common.util.Modality.nonModal;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.debugger.JDWPTunnelType.SSH_REVERSE_TUNNEL;
import static com.dbn.debugger.JDWPTunnelType.TCP_DRIVER_TUNNEL;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;
import static com.intellij.debugger.impl.PrioritizedTask.Priority.LOWEST;
import static com.intellij.debugger.impl.PrioritizedTask.Priority.NORMAL;

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
    private final DBJdwpSourcePathCache sourcePathCache = new DBJdwpSourcePathCache();


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
        this.declaredBlockIdentifier = debuggerInterface.getJdwpBlockIdentifier().replace(".", "\\");
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
        if (suspendContext == null) return false;
        if (is(DBDebugProcessStatus.TARGET_EXECUTION_TERMINATED)) return false;

        XExecutionStack executionStack = suspendContext.getActiveExecutionStack();
        if (executionStack == null) return true;

        XStackFrame topFrame = executionStack.getTopFrame();
        if (topFrame instanceof DBJdwpDebugStackFrame) return true;

        Location location = getLocation(topFrame);
        VirtualFile file = getVirtualFile(location);
        return file != null;
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

    SchemaId getTargetSchemaId() {
        T input = getExecutionInput();
        return input == null ? null : input.getTargetSchemaId();
    }

    DBRunConfig<T> getRunProfile() {
        return cast(getSession().getRunProfile());
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
        unmuteBreakpoints();

        DebuggerSession debuggerSession = getDebuggerSession();
        DebugProcessImpl process = debuggerSession.getProcess();
        ProcessHandler processHandler = process.getProcessHandler();

        DebuggerManager debuggerManager = getDebuggerManager();
        debuggerManager.addDebugProcessListener(processHandler, createProcessListener());

        XDebugSession session = getSession();
        session.addSessionListener(createSessionListener());

        DebuggerStateManager contextManager = debuggerSession.getContextManager();
        contextManager.addListener(createContextListener());
        process.setXDebugProcess(this);

        DBDebugOperation.run(getProject(), txt("ntf.debugger.constant.Operation_INITIALIZE_ENVIRONMENT"), () -> {
            try {
                T input = getExecutionInput();
                if (input == null) return;

                console.system(txt("log.debugger.info.InitializingDebugEnvironment"));

                ConnectionHandler connection = getConnection();
                SchemaId schemaId = input.getExecutionContext().getTargetSchema();
                targetConnection = connection.getDebugConnection(schemaId);
                targetConnection.setAutoCommit(false);
                targetConnection.beforeClose(() -> releaseSession(targetConnection));

                initializeLocalJdwpSession();

                console.system(txt("log.debugger.info.DebugSessionInitializedJdwp"));
                set(DBDebugProcessStatus.BREAKPOINT_SETTING_ALLOWED, true);

                createExecutionWrappers();
                queueCommand(NORMAL, () -> registerDefaultBreakpoint());
                queueCommand(NORMAL, () -> registerBreakpoints());
                queueCommand(LOWEST, () -> startTargetProgram()); // start program after initialization completion
            } catch (Exception e) {
                conditionallyLog(e);
                set(DBDebugProcessStatus.SESSION_INITIALIZATION_THREW_EXCEPTION, true);
                console.error(txt("log.debugger.error.ErrorInitializingDebugEnvironment", e.getMessage()));
                stop();
            }
        });
    }

    private void initializeLocalJdwpSession(){
        JDWPTunnelType tunnelType = tcpConfig.getTunnelType();
        if (tunnelType == TCP_DRIVER_TUNNEL) return; // no local session initialization for driver tunneling

        try {
            NetworkAddress localAddress = tcpConfig.getLocalAddress();
            console.info(txt("log.debugger.info.InitializingDebugSessionOnAddress", localAddress));

            NetworkAddress jdwpTcpAddress = localAddress.clone();

            //opening reverse ssh tunnel here if required
            if (tunnelType == SSH_REVERSE_TUNNEL) {
                console.info(txt("log.debugger.info.InitializingReverseSshTunnel"));
                SshTunnelConfig sshTunnelConfig = tcpConfig.getSshTunnelConfig();
                SshTunnelConnector sshTunnelConnector = new SshTunnelConnector(sshTunnelConfig, localAddress);
                sshTunnelConnector.setReverseTunnel(true);
                sshTunnelConnector.connect();
                targetConnection.beforeClose(() -> sshTunnelConnector.disconnect());

                SshdSocketAddress boundAddress = sshTunnelConnector.getTracker().getBoundAddress();
                jdwpTcpAddress.setHost(boundAddress.getHostName());
                jdwpTcpAddress.setPort(boundAddress.getPort());

                NetworkAddress proxyAddress = sshTunnelConfig.getProxyAddress();
                console.system(txt("log.debugger.info.ReverseSshTunnelStarted", localAddress, jdwpTcpAddress, proxyAddress));
            }

            DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
            debuggerInterface.initializeJdwpSession(targetConnection,
                    jdwpTcpAddress.getHost(),
                    jdwpTcpAddress.getPortString());
        }
        catch (Exception e) {
            conditionallyLog(e);
            set(DBDebugProcessStatus.SESSION_INITIALIZATION_THREW_EXCEPTION, true);
            console.error(txt("log.debugger.error.ErrorInitializingLocalJdwpSession", e.getMessage()));
            stop();
        }
    }

    private @NotNull DebuggerContextListener createContextListener() {
        return (newContext, event) -> {
            SuspendContextImpl suspendContext = newContext.getSuspendContext();
            overwriteSuspendContext(suspendContext);
        };
    }

    private void unmuteBreakpoints() {
        XDebugSession session = getSession();
        session.setBreakpointMuted(false);
    }

    private @NotNull XDebugSessionListener createSessionListener() {
        XDebugSession session = getSession();
        return new XDebugSessionListener() {
            @Override
            @ThreadPropertyGate(DEBUGGER_NAVIGATION)
            public void sessionPaused() {
                XSuspendContext suspendContext = session.getSuspendContext();
                if (suspendContext == null || !shouldSuspend(suspendContext)) {
                    Dispatch.run(nonModal(), () -> session.stepInto());
                    return;
                }

                XExecutionStack activeExecutionStack = suspendContext.getActiveExecutionStack();

                Location location = getTopFrameLocation(activeExecutionStack);
                VirtualFile virtualFile = getVirtualFile(location);
                DBDebugUtil.openEditor(virtualFile);
            }
        };
    }

    private static @NotNull DebugProcessListener createProcessListener() {
        return new DebugProcessListener() {
            @Override
            public void paused(@NotNull SuspendContext suspendContext) {
                if (suspendContext instanceof XSuspendContext xSuspendContext) {

                    XExecutionStack[] executionStacks = xSuspendContext.getExecutionStacks();
                    for (XExecutionStack executionStack : executionStacks) {
                        //System.out.println();
                    }

                    //underlyingFrame.getDescriptor().getLocation()

                }
            }
        };
    }

    protected void registerDefaultBreakpoint() {
        console.system(txt("log.debugger.info.RegisteringDefaultBreakpoint"));

        DBRunConfig<T> runProfile = getRunProfile();
        List<DBObjectRef<DBMethod>> methods = runProfile.getMethodRefs();
        if (methods.isEmpty()) return;

        var breakpointHandler = getBreakpointHandler();
        breakpointHandler.registerDefaultBreakpoint(methods.get(0));
    }

    private void registerBreakpoints() {
        console.system(txt("log.debugger.info.RegisteringBreakpoints"));

        List<DBObjectRef<DBMethod>> methods = getRunProfile().getMethodRefs();
        List<XLineBreakpoint<XBreakpointProperties>> breakpoints = getDatabaseBreakpoints();

        var breakpointHandler = getBreakpointHandler();
        breakpointHandler.registerBreakpoints(breakpoints, methods);
    }

    private List<XLineBreakpoint<XBreakpointProperties>> getDatabaseBreakpoints() {
        return DBBreakpointUtil.getDatabaseBreakpoints(getConnection(), DBDebuggerType.JDWP);
    }

    private void overwriteSuspendContext(final @Nullable XSuspendContext suspendContext) {
        if (suspendContext == null) return;
        if (suspendContext == lastSuspendContext) return;
        if (suspendContext instanceof DBJdwpDebugSuspendContext) return;

        lastSuspendContext = suspendContext;
        queueCommand(NORMAL, () -> processSuspendContext(suspendContext));
        throw new ProcessDeferredException();
    }

    private void processSuspendContext(@NotNull XSuspendContext suspendContext) {
        XDebugSession session = getSession();
        if (shouldSuspend(suspendContext)) {
            DBJdwpDebugSuspendContext dbSuspendContext = new DBJdwpDebugSuspendContext(DBJdwpDebugProcess.this, suspendContext);
            session.positionReached(dbSuspendContext);
        }
    }

    protected void createExecutionWrappers() throws SQLException {
        // no wrappers for PLSQL debugging
    }

    private void startTargetProgram() {
        T input = getExecutionInput();
        String title = txt("prc.debugger.title.RunningDebuggerTarget");
        String message = input == null ?
                txt("prc.debugger.text.ExecutingTargetProgram") :
                txt("prc.debugger.text.Executing", input.getExecutionContext().getTargetName());

        Progress.background(getProject(), getConnection(), false, title, message,
                progress -> {
                    console.system(txt("log.debugger.info.ExecutingTargetProgram"));
                    if (is(DBDebugProcessStatus.SESSION_INITIALIZATION_THREW_EXCEPTION)) return;
                    try {
                        set(DBDebugProcessStatus.TARGET_EXECUTION_STARTED, true);
                        executeTarget();
                    } catch (SQLException e) {
                        conditionallyLog(e);
                        set(DBDebugProcessStatus.TARGET_EXECUTION_THREW_EXCEPTION, true);
                        if (isNot(DBDebugProcessStatus.DEBUGGER_STOPPING)) {
                            String errorMessage = e.getMessage();
                            console.error(input == null ?
                                    txt("log.debugger.error.ErrorExecutingTargetProgram", errorMessage) :
                                    txt("log.debugger.error.ErrorExecuting", input.getExecutionContext().getTargetName(), errorMessage));
                        }
                    } finally {
                        set(DBDebugProcessStatus.TARGET_EXECUTION_TERMINATED, true);
                        stop();
                    }
                });
    }

    protected abstract void executeTarget() throws SQLException;

    @Override
    public synchronized void stop() {
        if (canStopDebugger()) {
            set(DBDebugProcessStatus.DEBUGGER_STOPPING, true);
            set(DBDebugProcessStatus.BREAKPOINT_SETTING_ALLOWED, false);
            console.system(txt("log.debugger.info.StoppingDebugger"));
            getSession().stop();
            stopDebugger();
            super.stop();
        }
    }

    private boolean canStopDebugger() {
        return isNot(DBDebugProcessStatus.DEBUGGER_STOPPING) && isNot(DBDebugProcessStatus.DEBUGGER_STOPED);
    }

    private void stopDebugger() {
        Progress.background(getProject(), getConnection(), false,
                txt("prc.debugger.title.StoppingDebugger"),
                txt("prc.debugger.text.StoppingDebugSession"),
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
                    console.system(txt("log.debugger.info.DebuggerStopped"));
                    set(DBDebugProcessStatus.DEBUGGER_STOPED, false);
                    set(DBDebugProcessStatus.DEBUGGER_STOPPING, false);
                });
    }

    private void releaseSession(DBNConnection targetConnection) {
        try {
            console.system(txt("log.debugger.info.ReleasingDebugSession"));
            DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
            debuggerInterface.disconnectJdwpSession(targetConnection);

        } catch (Throwable e) {
            conditionallyLog(e);
            console.error(txt("log.debugger.error.ErrorReleasingDebugSession", e.getMessage()));
        }
    }


    protected void releaseTargetConnection() {
        console.system(txt("log.debugger.info.ReleasingTargetConnection"));
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
            ConnectionHandler connection = getConnection();
            SchemaId schemaId = getTargetSchemaId();

            return sourcePathCache.getSourceFile(sourcePath, connection, schemaId);

        } catch (Exception e) {
            conditionallyLog(e);
            String errorMessage = Commons.nvl(e.getMessage(), simpleClassName(e));
            console.warning(txt("log.debugger.error.ErrorEvaluatingSuspendPosition", sourceUrl, errorMessage));
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
    public static Location getTopFrameLocation(@Nullable XExecutionStack executionStack) {
        if (executionStack == null) return null;

        if (executionStack instanceof DBJdwpDebugExecutionStack dbExecutionStack) {
            return dbExecutionStack.getTopFrameLocation();
        } else {
            XStackFrame topFrame = executionStack.getTopFrame();
            return getLocation(topFrame);
        }
    }


    @Nullable
    @SneakyThrows
    public static Location getLocation(@Nullable XStackFrame stackFrame) {
        if (stackFrame instanceof JavaStackFrame javaStackFrame) {
            StackFrameDescriptorImpl frameDescriptor = javaStackFrame.getDescriptor();
            Location location = frameDescriptor.getLocation();
            if (location != null) return location;

            // unwrap frame proxy
            StackFrameProxyImpl frameProxy = frameDescriptor.getFrameProxy();
            StackFrame proxyStackFrame = frameProxy.getStackFrame();
            location = proxyStackFrame.location();
            return location;

        }
        return null;
    }


    private DebuggerManager getDebuggerManager() {
        return DebuggerManager.getInstance(getProject());
    }

    @Override
    public void queueCommand(PrioritizedTask.Priority priority, Runnable command) {
        DebugProcessImpl debugProcess = getDebuggerSession().getProcess();
        ManagedThreadCommand.schedule(debugProcess, priority, command);
    }


}
