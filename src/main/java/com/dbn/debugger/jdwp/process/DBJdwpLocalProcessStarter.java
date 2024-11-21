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
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.config.ConnectionDebuggerSettings;
import com.intellij.debugger.DebugEnvironment;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.DefaultDebugEnvironment;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.util.Range;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
public abstract class DBJdwpLocalProcessStarter extends DBJdwpProcessStarter {
    DBJdwpLocalProcessStarter(ConnectionHandler connection) {
        super(connection);
    }

    /**
     * local database start's implementation: set up the ip host and port in  intellij
     * debugger framework , also setting up that we want to use listen connector and make the
     * debugger listen till the database connect using sockets
     * @param session session to be passed to {@link XDebugProcess#XDebugProcess} constructor
     */
    @NotNull
    @Override
    public final XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
        Executor executor = DefaultDebugExecutor.getDebugExecutorInstance();
        RunProfile runProfile = session.getRunProfile();
        runProfile = assertNotNull(runProfile, "Invalid run profile");


        ExecutionEnvironment environment = ExecutionEnvironmentBuilder.create(session.getProject(), executor, runProfile).build();
        ConnectionDebuggerSettings debuggerSettings = getConnection().getSettings().getDebuggerSettings();
        Range<Integer> portRange = debuggerSettings.getTcpPortRange();
        String tcpHost = resolveTcpHost(debuggerSettings.getTcpHostAddress());
        int tcpPort = findFreePort(tcpHost, portRange.getFrom(), portRange.getTo());
        DBJdwpTcpConfig tcpConfig = new DBJdwpTcpConfig(tcpHost, tcpPort);

        RemoteConnection remoteConnection = new RemoteConnection(true, tcpHost, Integer.toString(tcpPort), true);

        RunProfileState state = Failsafe.nn(runProfile.getState(executor, environment));

        DebugEnvironment debugEnvironment = new DefaultDebugEnvironment(environment, state, remoteConnection, true);
        DebuggerManagerEx debuggerManagerEx = DebuggerManagerEx.getInstanceEx(session.getProject());
        DebuggerSession debuggerSession = debuggerManagerEx.attachVirtualMachine(debugEnvironment);
        assertNotNull(debuggerSession, "Could not initialize JDWP listener");

        return createDebugProcess(session, debuggerSession, tcpConfig);

    }

    private static int findFreePort(String host, int minPortNumber, int maxPortNumber) throws ExecutionException {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new ExecutionException("Failed to resolve host '" + host + "'", e);
        }

        for (int portNumber = minPortNumber; portNumber < maxPortNumber; portNumber++) {
            try (ServerSocket ignored = new ServerSocket(portNumber, 50, inetAddress)) {
                return portNumber;
            } catch (Exception e) {
                conditionallyLog(e);
            }
        }
        throw new ExecutionException("Could not find any free port on host '" + host + "' in the range " + minPortNumber + " - " + maxPortNumber);
    }

    private static String resolveTcpHost(String tcpHost) {
        try {
            tcpHost = Strings.isEmptyOrSpaces(tcpHost) ?
                    Inet4Address.getLocalHost().getHostAddress() :
                    InetAddress.getAllByName(tcpHost)[0].getHostAddress();

        } catch (UnknownHostException e) {
            conditionallyLog(e);
            // TODO log to the debugger console instead
            log.warn("Failed to resolve TCP host address '{}'. Using 'localhost'", tcpHost, e);
            tcpHost =  "localhost";

        }
        return tcpHost;
    }


}
