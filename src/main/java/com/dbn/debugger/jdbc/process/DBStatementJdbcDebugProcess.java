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

package com.dbn.debugger.jdbc.process;

import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.debug.DebuggerRuntimeInfo;
import com.dbn.debugger.DBDebugUtil;
import com.dbn.debugger.common.breakpoint.DBBreakpointUtil;
import com.dbn.debugger.jdbc.DBJdbcDebugProcess;
import com.dbn.execution.ExecutionTarget;
import com.dbn.execution.statement.StatementExecutionInput;
import com.dbn.execution.statement.StatementExecutionManager;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.object.common.DBObject;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;
import java.util.Objects;

import static com.dbn.common.util.Strings.countNewLines;
import static com.dbn.debugger.DBDebugUtil.guardStatementExecution;

public class DBStatementJdbcDebugProcess extends DBJdbcDebugProcess<StatementExecutionInput> {
    DBStatementJdbcDebugProcess(@NotNull XDebugSession session, ConnectionHandler connection) {
        super(session, connection);
    }

    @Override
    public void sessionInitialized() {
        guardStatementExecution(getSession(), getExecutionInput());
        super.sessionInitialized();
    }

    @Override
    protected void executeTarget() throws SQLException {
        StatementExecutionManager statementExecutionManager = StatementExecutionManager.getInstance(getProject());
        statementExecutionManager.debugExecute(getExecutionProcessor(), getTargetConnection());

    }

    @Override
    protected void registerDefaultBreakpoint() {
        getBreakpointHandler().registerDefaultSourceBreakpoint(0);
    }

    @Nullable
    @Override
    public Integer resolveBreakpointLine(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
        Integer line = super.resolveBreakpointLine(breakpoint);
        if (line != null) return line;

        VirtualFile breakpointFile = DBBreakpointUtil.getBreakpointFile(breakpoint);
        VirtualFile executionFile = getExecutionProcessor().getVirtualFile();
        if (!Objects.equals(breakpointFile, executionFile)) return null;

        StatementExecutionInput executionInput = getExecutionInput();
        int sourceLine = breakpoint.getLine() - executionInput.getExecutableLineNumber();
        int lineCount = countNewLines(executionInput.getStatementText()) + 1;
        return sourceLine >= 0 && sourceLine < lineCount ? sourceLine : null;
    }

    @Nullable
    @Override
    public VirtualFile getRuntimeInfoFile(DebuggerRuntimeInfo runtimeInfo) {
        DBObject object = getDatabaseObject(runtimeInfo);
        return object == null ?
            getExecutionProcessor().getVirtualFile() :
            object.getVirtualFile();
    }
    private StatementExecutionProcessor getExecutionProcessor() {
        return getExecutionInput().getExecutionProcessor();
    }

    @NotNull
    @Override
    public String getName() {
        return getExecutionProcessor().getName();
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return getExecutionProcessor().getIcon();
    }

    @Override
    public ExecutionTarget getExecutionTarget() {
        return ExecutionTarget.STATEMENT;
    }
}
