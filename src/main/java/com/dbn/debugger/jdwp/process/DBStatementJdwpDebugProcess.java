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

import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.debugger.DBDebugUtil;
import com.dbn.debugger.common.breakpoint.DBBreakpointUtil;
import com.dbn.debugger.jdwp.DBJdwpBreakpointHandler;
import com.dbn.debugger.jdwp.DBJdwpSourcePath;
import com.dbn.execution.ExecutionTarget;
import com.dbn.execution.statement.StatementExecutionInput;
import com.dbn.execution.statement.StatementExecutionManager;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.vfs.DBVirtualFileBase;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.sun.jdi.Location;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;
import java.util.Objects;

import static com.dbn.common.util.Classes.simpleClassName;
import static com.dbn.common.util.Strings.countNewLines;
import static com.dbn.debugger.DBDebugUtil.guardStatementExecution;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
public class DBStatementJdwpDebugProcess extends DBJdwpDebugProcess<StatementExecutionInput> {
    DBStatementJdwpDebugProcess(@NotNull XDebugSession session, @NotNull DebuggerSession debuggerSession, ConnectionHandler connection, DBJdwpTcpConfig tcpConfig) {
        super(session, debuggerSession, connection, tcpConfig);
    }

    @Override
    public void sessionInitialized() {
        guardStatementExecution(getSession(), getExecutionInput());
        super.sessionInitialized();
    }

    @Override
    protected void executeTarget() throws SQLException {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor();
        if (executionProcessor != null) {
            StatementExecutionManager statementExecutionManager = StatementExecutionManager.getInstance(getProject());
            statementExecutionManager.debugExecute(executionProcessor, getTargetConnection());
        }
    }

    @Override
    protected void registerDefaultBreakpoint() {
        StatementExecutionInput executionInput = getExecutionInput();
        if (executionInput == null) {
            log.warn("Cannot register the default JDWP breakpoint: statement execution input is unavailable");
            return;
        }

        StatementExecutionProcessor executionProcessor = getExecutionProcessor();
        if (executionProcessor == null) {
            log.warn("Cannot register the default JDWP breakpoint: statement execution processor is unavailable");
            return;
        }

        VirtualFile virtualFile = executionProcessor.getVirtualFile();
        if (virtualFile == null) {
            log.warn("Cannot register the default JDWP breakpoint: statement source file is unavailable");
            return;
        }

        if (!(virtualFile instanceof DBVirtualFileBase sourceFile)) {
            log.warn("Cannot register the default JDWP breakpoint: unsupported source file {}",
                    virtualFile.getClass().getName());
            return;
        }

        DBJdwpBreakpointHandler breakpointHandler = getBreakpointHandler();
        int breakpointLine = executionInput.getExecutableLineNumber();
        breakpointHandler.registerDefaultSourceBreakpoint(sourceFile, breakpointLine);
    }

    @Nullable
    @Override
    public Integer resolveBreakpointLine(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
        Integer line = super.resolveBreakpointLine(breakpoint);
        if (line == null) return null;
        if (DBBreakpointUtil.getDatabaseObject(breakpoint) != null) return line;

        StatementExecutionInput executionInput = getExecutionInput();
        StatementExecutionProcessor executionProcessor = getExecutionProcessor();
        if (executionInput == null || executionProcessor == null) return null;

        VirtualFile executionFile = executionProcessor.getVirtualFile();
        if (executionFile == null || !Objects.equals(breakpoint.getFileUrl(), executionFile.getUrl())) return null;

        int sourceLine = line - executionInput.getExecutableLineNumber();
        int lineCount = countNewLines(executionInput.getStatementText()) + 1;
        return sourceLine >= 0 && sourceLine < lineCount ? sourceLine : null;
    }

    @Override
    @Nullable
    public VirtualFile getVirtualFile(Location location) {

        if (location != null) {
            String sourceUrl = "<NULL>";
            try {
                sourceUrl = location.sourcePath();
                DBJdwpSourcePath sourcePath = DBJdwpSourcePath.from(sourceUrl);
                String programType = sourcePath.getProgramType();
                if (Objects.equals(programType, "Block")) {
                    StatementExecutionProcessor executionProcessor = getExecutionProcessor();
                    if (executionProcessor != null) {
                        return executionProcessor.getVirtualFile();
                    }
                }
            } catch (Exception e) {
                conditionallyLog(e);
                getConsole().warning(txt("log.debugger.error.ErrorEvaluatingSuspendPosition", sourceUrl, Commons.nvl(e.getMessage(), simpleClassName(e))));
            }
        }

        return super.getVirtualFile(location);
    }

    @Nullable
    private StatementExecutionProcessor getExecutionProcessor() {
        StatementExecutionInput executionInput = getExecutionInput();
        return executionInput == null ? null : executionInput.getExecutionProcessor();
    }

    @NotNull
    @Override
    public String getName() {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor();
        if (executionProcessor != null) {
            return executionProcessor.getName();
        }
        return "Debug Process";
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        StatementExecutionProcessor executionProcessor = getExecutionProcessor();
        if (executionProcessor != null) {
            return executionProcessor.getIcon();
        }
        return null;
    }

    @Override
    public ExecutionTarget getExecutionTarget() {
        return ExecutionTarget.STATEMENT;
    }
}
