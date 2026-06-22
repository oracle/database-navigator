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
import com.dbn.common.util.Classes;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionUtil;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionPropertiesSettings;
import com.dbn.debugger.jdwp.process.tunnel.NSTunnelConnectionInitializer;
import com.dbn.debugger.jdwp.process.tunnel.NSTunnelConnectionProxy;
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
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.jdi.SocketTransportService;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.spi.Connection;
import com.sun.jdi.connect.spi.TransportService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Driver;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static com.dbn.common.util.Lists.first;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.debugger.JDWPTunnelType.TCP_DRIVER_TUNNEL;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
public abstract class DBJdwpTunnelProcessStarter extends DBJdwpProcessStarter{

    public static final byte[] HANDSHAKE_SIGNATURE = "JDWP-Handshake".getBytes(StandardCharsets.UTF_8);
    private static final Object CONNECTOR_PATCH_LOCK = new Object();
    private String jdwpHostPort = null;
    private NSTunnelConnectionProxy debugConnection = null;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(JdwpPacketReader.BUFFER_SIZE);
    private final ByteBuffer writeBuffer = ByteBuffer.allocate(JdwpPacketReader.BUFFER_SIZE);
    private final JdwpPacketReader packetReader;

    DBJdwpTunnelProcessStarter(ConnectionHandler connection) {
        super(connection);
        packetReader = new JdwpPacketReader(readBuffer, () -> debugConnection, this::closeDebugConnection);
    }

    void connect() throws ExecutionException {
        closeDebugConnection();

        Properties props = new Properties();
        ConnectionDatabaseSettings databaseSettings = getConnection().getSettings().getDatabaseSettings();
        String URL = databaseSettings.getConnectionUrl();
//        debugConnection = NSTunnelConnection.newInstance(URL, props);

        try {
            Driver driver = ConnectionUtil.resolveDriver(databaseSettings);
            if (driver == null) {
                throw new IOException(txt("msg.debugger.error.JdwpTunnelNsDriverNotFound"));
            }
            ClassLoader classLoader = driver.getClass().getClassLoader();
            if (classLoader == null) {
                throw new IOException(txt("msg.debugger.error.JdwpTunnelNsClassLoaderNotResolved"));
            }
            debugConnection = NSTunnelConnectionInitializer.newInstance(classLoader, URL, props);
            if (debugConnection == null) {
                throw new IOException(txt("msg.debugger.error.JdwpTunnelNsTunnelingObjectNotLoaded"));
            }

            jdwpHostPort = debugConnection.tunnelAddress();
            ConnectionPropertiesSettings connectionSettings = getConnection().getSettings().getPropertiesSettings();
            connectionSettings.getProperties().put("jdwpHostPort", jdwpHostPort);

        } catch (Throwable e) {
            throw new ExecutionException(txt("msg.debugger.error.FailedToConnectDebugger", e.getMessage()), e);
        }
    }

    private boolean isDebugConnectionOpen() {
        if (debugConnection == null) return false;
        try {
            return debugConnection.isOpen();
        } catch (Throwable e) {
            conditionallyLog(e);
            return false;
        }
    }

    private void closeDebugConnection() {
        try {
            if (debugConnection == null) return;
            if (!debugConnection.isOpen()) return;

            debugConnection.close();
        } catch (Throwable e) {
            log.warn("Failed to close existing debug connection", e);
        } finally {
            debugConnection = null;
        }
    }


    /**
     * JDWP tunnel implementation: use the attach connector and reflection to override the attach
     * behavior to use the NSTunnelConnection instead of establishing a new connection between
     * debugger and database.
     * @param session session to be passed to {@link XDebugProcess#XDebugProcess} constructor
     */
    @NotNull
    @Override
    public XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
        connect();

        Executor executor = DefaultDebugExecutor.getDebugExecutorInstance();
        RunProfile runProfile = session.getRunProfile();
        assertNotNull(runProfile, txt("msg.debugger.error.InvalidRunProfile"));


        Project project = session.getProject();
        ExecutionEnvironment environment = ExecutionEnvironmentBuilder.create(project, executor, runProfile).build();
        String host = extractHost(jdwpHostPort);
        String port = extractPort(jdwpHostPort);
        NetworkAddress localAddress = new NetworkAddress(host, port);

        DBJdwpTcpConfig tcpConfig = new DBJdwpTcpConfig(localAddress, TCP_DRIVER_TUNNEL);
        RemoteConnection remoteConnection = new RemoteConnection(true, host, port, false);

        RunProfileState state = Failsafe.nn(runProfile.getState(executor, environment));

        DebugEnvironment debugEnvironment = new DefaultDebugEnvironment(environment, state, remoteConnection, true);
        DebuggerManagerEx debuggerManagerEx = DebuggerManagerEx.getInstanceEx(project);
        DebuggerSession debuggerSession;
        synchronized (CONNECTOR_PATCH_LOCK) {
            try (ConnectorPatch connectorPatch = patchConnector()) {
                debuggerSession = debuggerManagerEx.attachVirtualMachine(debugEnvironment);
            }
        }
        assertNotNull(debuggerSession, txt("msg.debugger.error.CouldNotInitializeJdwpListener"));


        return createDebugProcess(session, debuggerSession, tcpConfig);

    }
    public static String extractHost(@NonNls String input) {
        int hostStartIndex = input.indexOf("host=") + 5;
        int hostEndIndex = input.indexOf(";port=");
        return input.substring(hostStartIndex, hostEndIndex);
    }

    public static String extractPort(@NonNls String input) {
        int portStartIndex = input.indexOf("port=") + 5;
        return input.substring(portStartIndex);
    }

    private ConnectorPatch patchConnector() throws ExecutionException {
        VirtualMachineManager vmManager = getVirtualMachineManager();
        List<AttachingConnector> connectors = vmManager.attachingConnectors();

        AttachingConnector connector = first(connectors, c ->
                c.name().equals("com.jetbrains.jdi.SocketAttach") ||
                c.name().equals("com.sun.jdi.SocketAttach"));

        if (connector == null) throw new ExecutionException(txt("msg.debugger.error.FailedToInitializeSocketConnector"));

        TransportService transportService = createTransportService();
        return patchConnector(connector, transportService);
    }

    private static VirtualMachineManager getVirtualMachineManager() throws ExecutionException {
        try {
            Class<?> managerClass = Commons.coalesce(
                    () -> Classes.classForName("com.jetbrains.jdi.VirtualMachineManagerImpl"),
                    () -> Classes.classForName("com.sun.tools.jdi.VirtualMachineManagerImpl"));
            if (managerClass == null)  throw new IllegalStateException("JDI components not accessible");
            Method initMethod = managerClass.getMethod("virtualMachineManager");

            return cast(initMethod.invoke(null));
        } catch (Throwable e) {
            throw new ExecutionException(txt("msg.debugger.error.FailedToInitializeVirtualMachine"), e);
        }
    }

    private static ConnectorPatch patchConnector(AttachingConnector connector, TransportService transportService) throws ExecutionException {
        try {
            Field field = getTransportServiceField();
            TransportService originalTransportService = cast(field.get(connector));
            field.set(connector, transportService);
            return new ConnectorPatch(connector, field, originalTransportService, transportService);
        } catch (Throwable e) {
            throw new ExecutionException(txt("msg.debugger.error.FailedToInitializeTransportService"), e);
        }
    }

    private static Field getTransportServiceField() throws NoSuchFieldException {
        Class<?> connectorClass = Commons.coalesce(
                () -> Classes.classForName("com.jetbrains.jdi.GenericAttachingConnector"),
                () -> Classes.classForName("com.sun.tools.jdi.GenericAttachingConnector"));

        if (connectorClass == null) throw new IllegalStateException("JDI components not accessible");

        Field field = connectorClass.getDeclaredField("transportService");
        field.setAccessible(true);
        return field;
    }

    private record ConnectorPatch(
            AttachingConnector connector,
            Field transportServiceField,
            TransportService originalTransportService,
            TransportService patchedTransportService) implements AutoCloseable {

        @Override
        public void close() throws ExecutionException {
            try {
                TransportService currentTransportService = cast(transportServiceField.get(connector));
                if (currentTransportService == patchedTransportService) {
                    transportServiceField.set(connector, originalTransportService);
                }
            } catch (Throwable e) {
                throw new ExecutionException(txt("msg.debugger.error.FailedToInitializeTransportService"), e);
            }
        }
    }

    @NotNull
    private TransportService createTransportService() {
        // TODO jdi backward compatibility (attempt raw implementation of TransportService)
        return new SocketTransportService() {
            @Override
            public Connection attach(String address, long attachTimeout, long handshakeTimeout) throws IOException {
                doHandCheck();
                return createConnection();
            }
        };
    }

    @NotNull
    private Connection createConnection() {
        return new Connection() {
            public byte[] readPacket() throws IOException {
                return readPackets();
            }

            @Override
            public void writePacket(byte[] packet) throws IOException {
                writePackets(packet);
            }

            @Override
            public void close() {
                closeDebugConnection();
            }

            @Override
            public boolean isOpen() {
                return isDebugConnectionOpen();
            }
        };
    }

    void doHandCheck() throws IOException {
        log.info("Started attaching transport service...");

        byte[] signature = HANDSHAKE_SIGNATURE;
        readBuffer.clear();
        writeBuffer.clear();
        debugConnection.read(readBuffer);
        byte[] response = new byte[signature.length];
        readBuffer.get(response);
        if (Arrays.compare(signature, response) == 0) {
            log.warn("Transport service handshake unsuccessful");
        }
        writePackets(signature);
        readBuffer.clear();

        log.info("Finished attaching transport service");
    }

    // read just one packet at each time called
    // and buffer the rest
    byte[] readPackets() throws IOException {
        return packetReader.readPacket();
    }


    synchronized void writePackets(byte[] bytes) throws IOException {
        writeBuffer.clear();
        writeBuffer.put(bytes);
        writeBuffer.flip();
        debugConnection.write(writeBuffer);
    }
}
