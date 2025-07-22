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
import com.dbn.connection.config.ConnectionDebuggerSettings;
import com.dbn.debugger.JDWPTunnelType;
import com.dbn.debugger.common.process.DBDebugProcessStarter;
import com.dbn.debugger.common.process.DBProgramRunner;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.JavaExecutionManager;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.operation.DatabaseOperation.DEBUG_JAVA_CODE;
import static com.dbn.debugger.DBDebuggerType.JDWP;

public class DBJavaJdwpRunner extends DBProgramRunner<JavaExecutionInput> {
    public static final String RUNNER_ID = "DBNJavaMethodJdwpRunner";

    public DBJavaJdwpRunner() {
        super(JDWP, DEBUG_JAVA_CODE);
    }

    @Override
    @NotNull
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    protected DBDebugProcessStarter createProcessStarter(ConnectionHandler connection) {
        ConnectionDebuggerSettings debuggerSettings = connection.getSettings().getDebuggerSettings();
        if(connection.isCloudDatabase() || debuggerSettings.getJdwpTunnelType() == JDWPTunnelType.TCP_DRIVER_TUNNEL){
            return new DBJavaJdwpCloudProcessStarter(connection);
        }
        return new DBJavaJdwpLocalProcessStarter(connection);
    }

    @Override
    protected void performInitialization(
            @NotNull ConnectionHandler connection,
            @NotNull JavaExecutionInput executionInput,
            @NotNull ExecutionEnvironment environment) {

        // no initialization required for java debugging (as of now)
        // (wrapper method is expected to be already compiled in debug mode)

        performExecution(
                executionInput,
                environment);
    }

    @Override
    protected void promptExecutionDialog(JavaExecutionInput executionInput, Runnable callback) {
        Project project = executionInput.getProject();
        JavaExecutionManager executionManager = JavaExecutionManager.getInstance(project);
        executionManager.promptExecutionDialog(executionInput, JDWP, callback);
    }
}
