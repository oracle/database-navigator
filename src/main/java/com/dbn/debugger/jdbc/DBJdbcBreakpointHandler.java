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

package com.dbn.debugger.jdbc;

import com.dbn.common.icon.Icons;
import com.dbn.common.routine.ParametricCallable;
import com.dbn.common.thread.Write;
import com.dbn.common.util.Documents;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.database.common.debug.BreakpointInfo;
import com.dbn.database.common.debug.BreakpointOperationInfo;
import com.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.dbn.debugger.DBDebugConsoleLogger;
import com.dbn.debugger.DBDebugUtil;
import com.dbn.debugger.common.breakpoint.DBBreakpointHandler;
import com.dbn.debugger.common.breakpoint.DBBreakpointType;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.psql.PSQLFile;
import com.dbn.object.DBMethod;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.dbn.common.notification.NotificationCategory.DEBUGGER;
import static com.dbn.common.util.Strings.cachedUpperCase;
import static com.dbn.debugger.common.breakpoint.DBBreakpointUtil.getAllBreakpoints;
import static com.dbn.debugger.common.breakpoint.DBBreakpointUtil.getBreakpointDesc;
import static com.dbn.debugger.common.breakpoint.DBBreakpointUtil.getBreakpointFile;
import static com.dbn.debugger.common.breakpoint.DBBreakpointUtil.getBreakpointId;
import static com.dbn.debugger.common.breakpoint.DBBreakpointUtil.getDatabaseObject;
import static com.dbn.debugger.common.breakpoint.DBBreakpointUtil.setBreakpointId;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class DBJdbcBreakpointHandler extends DBBreakpointHandler<DBJdbcDebugProcess<?>> {
    protected BreakpointInfo defaultBreakpointInfo;

    DBJdbcBreakpointHandler(XDebugSession session, DBJdbcDebugProcess<?> debugProcess) {
        super(session, debugProcess);
        //resetBreakpoints();
    }

    @Override
    protected void registerDatabaseBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
        DBJdbcDebugProcess<?> debugProcess = getDebugProcess();
        XDebugSession session = getSession();

        VirtualFile virtualFile = getBreakpointFile(breakpoint);
        Project project = session.getProject();
        if (virtualFile == null) {
            XDebuggerManager debuggerManager = XDebuggerManager.getInstance(project);
            XBreakpointManager breakpointManager = debuggerManager.getBreakpointManager();
            Write.run(project, () -> breakpointManager.removeBreakpoint(breakpoint));
            return;
        }

        try {
            Integer breakpointId = getBreakpointId(breakpoint);
            if (breakpointId == null) {
                addBreakpoint(breakpoint);
            } else {
                Integer breakpointLine = debugProcess.resolveBreakpointLine(breakpoint);
                if (breakpointLine == null) return;

                enableBreakpoint(breakpoint);
            }
        } catch (Exception e) {
            conditionallyLog(e);
            handleBreakpointError(breakpoint, e.getMessage());
        }
    }

    @Override
    protected void unregisterDatabaseBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint, boolean temporary) {
        if (!canSetBreakpoints()) return;

        Integer breakpointId = getBreakpointId(breakpoint);
        if (breakpointId == null) return;

        DBDebugConsoleLogger console = getConsole();
        VirtualFile virtualFile = getBreakpointFile(breakpoint);
        if (virtualFile != null) {
            String breakpointDesc = getBreakpointDesc(breakpoint);
            try {
                removeBreakpoint(temporary, breakpointId);
                console.system(txt("log.debugger.info.BreakpointRemoved", breakpointDesc));
            } catch (SQLException e) {
                conditionallyLog(e);
                console.error(txt("log.debugger.error.ErrorRemovingBreakpoint", breakpointDesc, e.getMessage()));
                sendErrorNotification(DEBUGGER, txt("ntf.debugger.error.ErrorUnregisteringBreakpoints", e));
            } finally {
                setBreakpointId(breakpoint, null);
            }
        }
    }

    @Override
    public void registerDefaultBreakpoint(DBObjectRef<DBMethod> method) {
        DBEditableObjectVirtualFile mainDatabaseFile = DBDebugUtil.getMainDatabaseFile(method);

        if (mainDatabaseFile == null) return;

        DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) mainDatabaseFile.getMainContentFile();
        if (sourceCodeFile == null) return;

        PSQLFile psqlFile = (PSQLFile) sourceCodeFile.getPsiFile();
        if (psqlFile == null) return;

        BasePsiElement basePsiElement = psqlFile.lookupObjectDeclaration(method.getObjectType().getGenericType(), method.getObjectName());
        if (basePsiElement == null) return;

        BasePsiElement subject = basePsiElement.findFirstPsiElement(ElementTypeAttribute.SUBJECT);
        int offset = subject.getTextOffset();
        Document document = Documents.getDocument(psqlFile);
        if (document == null) return;

        int breakpointLine = document.getLineNumber(offset);
        DBObjectRef<DBObject> object = DBDebugUtil.getMainDatabaseObject(method);
        if (object == null) return;

        try {
            BreakpointInfo breakpointInfo = executeDebuggerOperation(
                    d -> d.addProgramBreakpoint(
                            method.getSchemaName(),
                            object.getObjectName(),
                            cachedUpperCase(object.getObjectType().getName()),
                            breakpointLine,
                            getDebugConnection()));
            registerDefaultBreakpoint(breakpointInfo, method.getQualifiedName(), breakpointLine);
        } catch (SQLException e) {
            handleDefaultBreakpointError(e);
        }
    }

    /**
     * Registers a hidden entry breakpoint in the current nonpersistent source unit.
     * The target must already be synchronized so {@code DBMS_DEBUG} can resolve the
     * anonymous program handle.
     *
     * @param line zero-based line in the anonymous source
     */
    public void registerDefaultSourceBreakpoint(int line) {
        try {
            BreakpointInfo breakpointInfo = executeDebuggerOperation(
                    d -> d.addSourceBreakpoint(line, getDebugConnection()));
            String targetName = getDebugProcess().getExecutionInput().getExecutionContext().getTargetName();
            registerDefaultBreakpoint(breakpointInfo, targetName, line);
        } catch (SQLException e) {
            handleDefaultBreakpointError(e);
        }
    }

    private void registerDefaultBreakpoint(BreakpointInfo breakpointInfo, String targetName, int line) {
        defaultBreakpointInfo = breakpointInfo;

        Integer breakpointId = breakpointInfo.getBreakpointId();
        String breakpointDesc = targetName + ":" + (line + 1) + " (id=" + breakpointId + ")";
        String error = breakpointInfo.getError();
        DBDebugConsoleLogger console = getConsole();
        if (error == null) {
            console.system(txt("log.debugger.info.BreakpointAdded", breakpointDesc));
        } else {
            console.error(txt("log.debugger.error.FailedAddingBreakpoint", breakpointDesc, error));
        }
    }

    private DBDebugConsoleLogger getConsole() {
        return getDebugProcess().getConsole();
    }

    private void handleDefaultBreakpointError(SQLException e) {
        conditionallyLog(e);
        DBDebugConsoleLogger console = getConsole();
        console.error(txt("log.debugger.error.FailedRegisteringBreakpoint", e.getMessage()));
    }

    @Override
    public void registerWrapperBreakpoint(DBObjectRef<DBMethod> wrapperMethod) {
        // only for java wrappers - not supported in JDBC debug
    }

    @Override
    public void unregisterDefaultBreakpoint() {
        try {
            if (defaultBreakpointInfo == null) return;
            if (defaultBreakpointInfo.getBreakpointId() == null) return;

            executeDebuggerOperation(
                    d -> d.removeBreakpoint(
                            defaultBreakpointInfo.getBreakpointId(),
                            getDebugConnection()));
        } catch (SQLException e) {
            conditionallyLog(e);
        }
    }

    private DBNConnection getDebugConnection() {
        DBJdbcDebugProcess<?> debugProcess = getDebugProcess();
        return debugProcess.getDebuggerConnection();
    }

    private void addBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) throws Exception {
        DBJdbcDebugProcess<?> debugProcess = getDebugProcess();
        Integer breakpointLine = debugProcess.resolveBreakpointLine(breakpoint);
        if (breakpointLine == null) return;

        DBNConnection debugConnection = getDebugConnection();
        DBObjectRef object = getDatabaseObject(breakpoint);
        BreakpointInfo breakpointInfo;
        if (object == null) {
            breakpointInfo = executeDebuggerOperation(
                    d -> d.addSourceBreakpoint(breakpointLine, debugConnection));
        } else {
            breakpointInfo = executeDebuggerOperation(
                    d -> d.addProgramBreakpoint(
                            object.getSchemaName(),
                            object.getObjectName(),
                            cachedUpperCase(object.getObjectType().getName()),
                            breakpointLine,
                            debugConnection));
        }

        String error = breakpointInfo.getError();
        if (error != null) {
            handleBreakpointError(breakpoint, error);
            return;
        }

        Integer breakpointId = breakpointInfo.getBreakpointId();
        setBreakpointId(breakpoint, breakpointId);

        if (!breakpoint.isEnabled()) {
            error = disableBreakpoint(breakpointId);
            if (error != null) {
                getSession().updateBreakpointPresentation(breakpoint,
                        Icons.DEBUG_INVALID_BREAKPOINT,
                        "INVALID: " + error);
            }
        }

        DBDebugConsoleLogger console = getConsole();
        String breakpointDesc = getBreakpointDesc(breakpoint);
        console.system(txt("log.debugger.info.BreakpointAdded", breakpointDesc));
    }

    private void removeBreakpoint(boolean temporary, Integer breakpointId) throws SQLException {
        DBNConnection debugConnection = getDebugConnection();
        if (temporary) {
            executeDebuggerOperation(d -> d.disableBreakpoint(breakpointId, debugConnection));
        } else {
            executeDebuggerOperation(d -> d.removeBreakpoint(breakpointId, debugConnection));
        }
    }

    private void enableBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) throws Exception {
        Integer breakpointId = getBreakpointId(breakpoint);
        if (breakpointId == null) return;

        DBNConnection debugConnection = getDebugConnection();

        BreakpointOperationInfo breakpointOperationInfo = executeDebuggerOperation(
                d -> d.enableBreakpoint(breakpointId, debugConnection));
        String error = breakpointOperationInfo.getError();
        if (error != null) {
            getSession().updateBreakpointPresentation(breakpoint,
                    Icons.DEBUG_INVALID_BREAKPOINT,
                    "INVALID: " + error);
        }
    }

    private String disableBreakpoint(Integer breakpointId) throws SQLException {
        DBNConnection debugConnection = getDebugConnection();
        BreakpointOperationInfo breakpointOperationInfo = executeDebuggerOperation(
                d -> d.disableBreakpoint(breakpointId, debugConnection));
        return breakpointOperationInfo.getError();
    }

    private <R> R executeDebuggerOperation(ParametricCallable<DatabaseDebuggerInterface, R, SQLException> callable) throws SQLException {
        return getDebugProcess().executeDebuggerOperation(callable);
    }

    private void resetBreakpoints() {
        Project project = getSession().getProject();
        XBreakpoint<?>[] breakpoints = getAllBreakpoints(project);

        for (XBreakpoint breakpoint : breakpoints) {
            if (breakpoint.getType() instanceof DBBreakpointType) {
                XLineBreakpoint lineBreakpoint = (XLineBreakpoint) breakpoint;
                VirtualFile virtualFile = getBreakpointFile(lineBreakpoint);
                if (virtualFile == null) continue;

                FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
                ConnectionHandler connection = contextManager.getConnection(virtualFile);

                if (connection == getDebugProcess().getConnection()) {
                    setBreakpointId(lineBreakpoint, null);
                }
            }
        }
    }
}
