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

package com.dbn.debugger.jdbc.process;

import com.dbn.connection.ConnectionHandler;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.debugger.common.process.DBDebugProcessStarter;
import com.dbn.debugger.common.process.DBProgramRunner;
import com.dbn.execution.method.MethodExecutionInput;
import com.dbn.execution.method.MethodExecutionManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class DBMethodJdbcRunner extends DBProgramRunner<MethodExecutionInput> {
    public static final String RUNNER_ID = "DBNMethodRunner";

    @Override
    @NotNull
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public DBDebuggerType getDebuggerType() {
        return DBDebuggerType.JDBC;
    }

    @Override
    protected DBDebugProcessStarter createProcessStarter(ConnectionHandler connection) {
        return new DBMethodJdbcProcessStarter(connection);
    }

    @Override
    protected void promptExecutionDialog(MethodExecutionInput executionInput, Runnable callback) {
        Project project = executionInput.getProject();
        MethodExecutionManager executionManager = MethodExecutionManager.getInstance(project);
        executionManager.promptExecutionDialog(executionInput, DBDebuggerType.JDBC, callback);
    }
}

