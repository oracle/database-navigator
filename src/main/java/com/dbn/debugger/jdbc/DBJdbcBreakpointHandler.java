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

import com.dbn.common.icon.Icons;
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
import com.dbn.debugger.common.breakpoint.DBBreakpointUtil;
import com.dbn.debugger.common.process.DBDebugProcess;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.psql.PSQLFile;
import com.dbn.object.DBMethod;
import com.dbn.object.common.DBSchemaObject;
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

import static com.dbn.common.notification.NotificationGroup.DEBUGGER;
import static com.dbn.common.util.Strings.cachedUpperCase;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class DBJdbcBreakpointHandler extends DBBreakpointHandler<DBJdbcDebugProcess> {
    protected BreakpointInfo defaultBreakpointInfo;

    DBJdbcBreakpointHandler(XDebugSession session, DBJdbcDebugProcess debugProcess) {
        super(session, debugProcess);
        //resetBreakpoints();
    }

    @Override
    protected void registerDatabaseBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
        DBDebugProcess debugProcess = getDebugProcess();
        DBDebugConsoleLogger console = debugProcess.getConsole();

        XDebugSession session = getSession();

        VirtualFile virtualFile = DBBreakpointUtil.getVirtualFile(breakpoint);
        Project project = session.getProject();
        if (virtualFile == null) {
            XDebuggerManager debuggerManager = XDebuggerManager.getInstance(project);
            XBreakpointManager breakpointManager = debuggerManager.getBreakpointManager();
            Write.run(project, () -> breakpointManager.removeBreakpoint(breakpoint));
        } else {
            try {
                if (DBBreakpointUtil.getBreakpointId(breakpoint) != null) {
                    enableBreakpoint(breakpoint);

                } else {
                    BreakpointInfo breakpointInfo = addBreakpoint(breakpoint);
                    String error = breakpointInfo.getError();
                    if (error != null) {
                        handleBreakpointError(breakpoint, error);
                    } else {
                        Integer breakpointId = breakpointInfo.getBreakpointId();
                        DBBreakpointUtil.setBreakpointId(breakpoint, breakpointId);

                        if (!breakpoint.isEnabled()) {
                            error = disableBreakpoint(breakpointId);
                            if (error != null) {
                                session.updateBreakpointPresentation( breakpoint,
                                        Icons.DEBUG_INVALID_BREAKPOINT,
                                        "INVALID: " + error);
                            }

                        }
                        String breakpointDesc = DBBreakpointUtil.getBreakpointDesc(breakpoint);
                        console.system("Breakpoint added: " + breakpointDesc);
                    }
                }

            } catch (Exception e) {
                conditionallyLog(e);
                handleBreakpointError(breakpoint, e.getMessage());
            }
        }
    }

    @Override
    protected void unregisterDatabaseBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint, boolean temporary) {
        DBDebugProcess debugProcess = getDebugProcess();

        if (!canSetBreakpoints()) return;

        Integer breakpointId = DBBreakpointUtil.getBreakpointId(breakpoint);
        if (breakpointId != null) {
            DBDebugConsoleLogger console = debugProcess.getConsole();

            VirtualFile virtualFile = DBBreakpointUtil.getVirtualFile(breakpoint);
            if (virtualFile != null) {
                String breakpointDesc = DBBreakpointUtil.getBreakpointDesc(breakpoint);
                try {
                    removeBreakpoint(temporary, breakpointId);
                    console.system("Breakpoint removed: " + breakpointDesc);
                } catch (SQLException e) {
                    conditionallyLog(e);
                    console.error("Error removing breakpoint: " + breakpointDesc + ". " + e.getMessage());
                    sendErrorNotification(DEBUGGER, txt("ntf.debugger.error.ErrorUnregisteringBreakpoints", e));
                } finally {
                    DBBreakpointUtil.setBreakpointId(breakpoint, null);
                }
            }
        }
    }

    @Override
    public void registerDefaultBreakpoint(DBMethod method) {
        DBEditableObjectVirtualFile mainDatabaseFile = DBDebugUtil.getMainDatabaseFile(method);

        if (mainDatabaseFile == null) return;

        DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) mainDatabaseFile.getMainContentFile();
        PSQLFile psqlFile = (PSQLFile) sourceCodeFile.getPsiFile();
        if (psqlFile == null) return;

        BasePsiElement basePsiElement = psqlFile.lookupObjectDeclaration(method.getObjectType().getGenericType(), method.getName());
        if (basePsiElement == null) return;

        BasePsiElement subject = basePsiElement.findFirstPsiElement(ElementTypeAttribute.SUBJECT);
        int offset = subject.getTextOffset();
        Document document = Documents.getDocument(psqlFile);
        if (document == null) return;

        int line = document.getLineNumber(offset);
        DBSchemaObject schemaObject = DBDebugUtil.getMainDatabaseObject(method);
        if (schemaObject == null) return;

        try {
            defaultBreakpointInfo = getDebuggerInterface().addProgramBreakpoint(
                    method.getSchema().getName(),
                    schemaObject.getName(),
                    cachedUpperCase(schemaObject.getObjectType().getName()),
                    line,
                    getDebugConnection());
        } catch (SQLException e) {
            conditionallyLog(e);
        }
    }

    @Override
    public void unregisterDefaultBreakpoint() {
        try {
            if (defaultBreakpointInfo != null && defaultBreakpointInfo.getBreakpointId() != null) {
                getDebuggerInterface().removeBreakpoint(defaultBreakpointInfo.getBreakpointId(), getDebugConnection());
            }
        } catch (SQLException e) {
            conditionallyLog(e);
        }
    }

    private DBNConnection getDebugConnection() {
        DBJdbcDebugProcess debugProcess = getDebugProcess();
        return debugProcess.getDebuggerConnection();
    }

    private BreakpointInfo addBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) throws Exception {
        ConnectionHandler connection = getConnection();
        DatabaseDebuggerInterface debuggerInterface = connection.getDebuggerInterface();
        DBNConnection debugConnection = getDebugConnection();
        DBSchemaObject object = DBBreakpointUtil.getDatabaseObject(breakpoint);
        return object == null ?
                debuggerInterface.addSourceBreakpoint(
                        breakpoint.getLine(),
                        debugConnection) :
                debuggerInterface.addProgramBreakpoint(
                        object.getSchema().getName(),
                        object.getName(),
                        cachedUpperCase(object.getObjectType().getName()),
                        breakpoint.getLine(),
                        debugConnection);
    }

    private void removeBreakpoint(boolean temporary, Integer breakpointId) throws SQLException {
        ConnectionHandler connection = getConnection();
        DBNConnection debugConnection = getDebugConnection();
        DatabaseDebuggerInterface debuggerInterface = connection.getDebuggerInterface();
        if (temporary) {
            debuggerInterface.disableBreakpoint(breakpointId, debugConnection);
        } else {
            debuggerInterface.removeBreakpoint(breakpointId, debugConnection);
        }
    }

    private void enableBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) throws Exception {
        Integer breakpointId = DBBreakpointUtil.getBreakpointId(breakpoint);
        if (breakpointId != null) {
            ConnectionHandler connection = getConnection();
            DBNConnection debugConnection = getDebugConnection();

            DatabaseDebuggerInterface debuggerInterface = connection.getDebuggerInterface();
            BreakpointOperationInfo breakpointOperationInfo = debuggerInterface.enableBreakpoint(breakpointId, debugConnection);
            String error = breakpointOperationInfo.getError();
            if (error != null) {
                getSession().updateBreakpointPresentation(breakpoint,
                        Icons.DEBUG_INVALID_BREAKPOINT,
                        "INVALID: " + error);
            }

        }
    }

    private String disableBreakpoint(Integer breakpointId) throws SQLException {
        ConnectionHandler connection = getConnection();
        DatabaseDebuggerInterface debuggerInterface = connection.getDebuggerInterface();
        DBNConnection debugConnection = getDebugConnection();
        BreakpointOperationInfo breakpointOperationInfo = debuggerInterface.disableBreakpoint(breakpointId, debugConnection);
        return breakpointOperationInfo.getError();
    }

    private void resetBreakpoints() {
        Project project = getSession().getProject();

        XBreakpointManager breakpointManager = XDebuggerManager.getInstance(project).getBreakpointManager();
        XBreakpoint<?>[] breakpoints = breakpointManager.getAllBreakpoints();

        for (XBreakpoint breakpoint : breakpoints) {
            if (breakpoint.getType() instanceof DBBreakpointType) {
                XLineBreakpoint lineBreakpoint = (XLineBreakpoint) breakpoint;
                VirtualFile virtualFile = DBBreakpointUtil.getVirtualFile(lineBreakpoint);
                if (virtualFile != null) {
                    FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
                    ConnectionHandler connection = contextManager.getConnection(virtualFile);

                    if (connection == getDebugProcess().getConnection()) {
                        DBBreakpointUtil.setBreakpointId(lineBreakpoint, null);
                    }
                }
            }
        }
    }
}
