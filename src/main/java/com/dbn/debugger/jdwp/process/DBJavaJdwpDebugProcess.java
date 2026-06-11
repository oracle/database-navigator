/*
 * Copyright 2025 Oracle and/or its affiliates
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

import com.dbn.connection.ConnectionHandler;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.debugger.common.breakpoint.DBBreakpointHandler;
import com.dbn.execution.ExecutionTarget;
import com.dbn.execution.java.JavaExecutionContext;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.JavaExecutionManager;
import com.dbn.execution.java.wrapper.WrapperModel;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.common.DBSchemaObject;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.xdebugger.XDebugSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;

import static com.dbn.nls.NlsResources.txt;

public class DBJavaJdwpDebugProcess extends DBJdwpDebugProcess<JavaExecutionInput> {
    DBJavaJdwpDebugProcess(@NotNull XDebugSession session, @NotNull DebuggerSession debuggerSession, ConnectionHandler connection, DBJdwpTcpConfig tcpConfig) {
        super(session, debuggerSession, connection, tcpConfig);
    }

    @NotNull
    @Override
    public String getName() {
        JavaExecutionInput executionInput = getExecutionInput();
        if (executionInput != null) {
            DBJavaMethod method = executionInput.getMethod();
            DBSchemaObject object = getMainDatabaseObject(method);
            if (object != null) {
                return object.getQualifiedName();
            }
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
        JavaExecutionInput executionInput = getExecutionInput();
        if (executionInput != null) {
            DBJavaMethod method = executionInput.getMethod();
            DBSchemaObject object = getMainDatabaseObject(method);
            if (object != null) {
                return object.getIcon();
            }
        }
        return null;
    }

    @Nullable
    protected DBSchemaObject getMainDatabaseObject(DBJavaMethod method) {
        return method.getOwnerClass();
    }

    @Override
    protected void createExecutionWrappers() throws SQLException {
        getConsole().system(txt("log.debugger.info.CreatingExecutionWrappers"));
        JavaExecutionInput executionInput = getExecutionInput();
        if (executionInput == null) return;

        JavaExecutionContext executionContext = executionInput.getExecutionContext();
        executionContext.createExecutionWrappers();
    }

    @Override
    protected void registerDefaultBreakpoint() {
        JavaExecutionInput executionInput = getExecutionInput();
        if (executionInput == null) return;

        JavaExecutionContext executionContext = executionInput.getExecutionContext();
        WrapperModel wrapperModel = executionContext.getWrapperModel();
        if (wrapperModel == null) return;

        DBBreakpointHandler<DBJdwpDebugProcess> breakpointHandler = getBreakpointHandler();
        breakpointHandler.registerWrapperBreakpoint(wrapperModel.getSqlWrapperMethod());
    }

    @Override
    protected void executeTarget() throws SQLException {
        JavaExecutionInput executionInput = getExecutionInput();
        if (executionInput == null) return;

        JavaExecutionManager javaExecutionManager = JavaExecutionManager.getInstance(getProject());
        javaExecutionManager.debugExecute(executionInput, getTargetConnection(), DBDebuggerType.JDWP);
    }

    @Override
    protected void releaseTargetConnection() {
        // method execution processor is responsible for closing
        // the connection after the result is read
        targetConnection = null;
    }

    @Override
    public ExecutionTarget getExecutionTarget() {
        return ExecutionTarget.JAVA;
    }
}
