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
import com.dbn.common.network.NetworkAddress;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.config.ConnectionDebuggerSettings;
import com.dbn.connection.config.ReverseSshTunnelConfiguration;
import com.dbn.connection.ssh.SshTunnelConfig;
import com.dbn.debugger.JDWPTunnelType;
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

import static com.dbn.debugger.JDWPTunnelType.SSH_REVERSE_TUNNEL;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

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
        runProfile = assertNotNull(runProfile, txt("msg.debugger.error.InvalidRunProfile"));


        ExecutionEnvironment environment = ExecutionEnvironmentBuilder.create(session.getProject(), executor, runProfile).build();
        DBJdwpTcpConfig tcpConfig = initializeJdwpTcpConfig();
        NetworkAddress localAddress = tcpConfig.getLocalAddress();

        RemoteConnection remoteConnection = new RemoteConnection(true, localAddress.getHost(), localAddress.getPortString() , true);
        RunProfileState state = Failsafe.nn(runProfile.getState(executor, environment));

        DebugEnvironment debugEnvironment = new DefaultDebugEnvironment(environment, state, remoteConnection, true);
        DebuggerManagerEx debuggerManagerEx = DebuggerManagerEx.getInstanceEx(session.getProject());
        DebuggerSession debuggerSession = debuggerManagerEx.attachVirtualMachine(debugEnvironment);
        assertNotNull(debuggerSession, txt("msg.debugger.error.CouldNotInitializeJdwpListener"));

        return createDebugProcess(session, debuggerSession, tcpConfig);

    }

    private DBJdwpTcpConfig initializeJdwpTcpConfig() throws ExecutionException {
        ConnectionDebuggerSettings debuggerSettings = getConnection().getSettings().getDebuggerSettings();
        NetworkAddress localAddress = resolveLocalAddress(debuggerSettings);

        JDWPTunnelType tunnelType = debuggerSettings.getJdwpTunnelType();
        if (tunnelType == SSH_REVERSE_TUNNEL) {
            SshTunnelConfig sshTunnelConfig = createSshTunnelConfig(debuggerSettings);
            return new DBJdwpTcpConfig(localAddress, tunnelType, sshTunnelConfig);
        }

        return new DBJdwpTcpConfig(localAddress, JDWPTunnelType.NONE);
    }

    public SshTunnelConfig createSshTunnelConfig(ConnectionDebuggerSettings debuggerSettings) {
        ReverseSshTunnelConfiguration config = debuggerSettings.getReverseSshTunnelConfig();
        NetworkAddress proxyAddress = new NetworkAddress(
                config.getHost(),
                config.getPort());

        NetworkAddress remoteAddress = new NetworkAddress(
                config.getBindHost(),
                config.getBindPort());

        return new SshTunnelConfig(
                proxyAddress,
                remoteAddress,
                config.getAuthType(),
                config.getUser(),
                config.getPassword(), config.getKeyFile(),
                config.getKeyPassphrase()
        );
    }

    private NetworkAddress resolveLocalAddress(ConnectionDebuggerSettings debuggerSettings) throws ExecutionException {
        Range<Integer> portRange = debuggerSettings.getTcpPortRange();
        String localHost = resolveTcpHost(debuggerSettings.getTcpHostAddress());
        int localPort = findFreePort(localHost, portRange.getFrom(), portRange.getTo());

        return new NetworkAddress(localHost, localPort);
    }

    private static int findFreePort(String host, int minPortNumber, int maxPortNumber) throws ExecutionException {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new ExecutionException(txt("msg.debugger.error.FailedToResolveHost"), e);
        }

        for (int portNumber = minPortNumber; portNumber <= maxPortNumber; portNumber++) {
            try (ServerSocket ignored = new ServerSocket(portNumber, 50, inetAddress)) {
                return portNumber;
            } catch (Exception e) {
                conditionallyLog(e);
            }
        }
        throw new ExecutionException(txt("msg.debugger.error.CouldNotFindFreePortForRange"));
    }

    private static String resolveTcpHost(String tcpHost) {
        try {
            tcpHost = Strings.isEmptyOrSpaces(tcpHost) ?
                    Inet4Address.getLocalHost().getHostAddress() :
                    InetAddress.getAllByName(tcpHost)[0].getHostAddress();

        } catch (UnknownHostException e) {
            conditionallyLog(e);
            // TODO log to the debugger console instead
            log.warn("Failed to resolve provided TCP host address Using 'localhost'", e);
            tcpHost =  "localhost";

        }
        return tcpHost;
    }


}
