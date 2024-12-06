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
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
public abstract class DBJdwpCloudProcessStarter extends DBJdwpProcessStarter{

    private String jdwpHostPort = null;
    private NSTunnelConnectionProxy debugConnection = null;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(320000);
    private final ByteBuffer writeBuffer = ByteBuffer.allocate(320000);

    DBJdwpCloudProcessStarter(ConnectionHandler connection) {
        super(connection);
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
                throw new IOException("Could not find driver for Cloud NSTunnelConnection class loading");
            }
            ClassLoader classLoader = driver.getClass().getClassLoader();
            debugConnection = NSTunnelConnectionInitializer.newInstance(classLoader, URL, props);
            if (debugConnection == null) {
                throw new IOException("Could not load tunneling object. Does the current driver support Cloud NS?");
            }

            jdwpHostPort = debugConnection.tunnelAddress();
            ConnectionPropertiesSettings connectionSettings = getConnection().getSettings().getPropertiesSettings();
            connectionSettings.getProperties().put("jdwpHostPort", jdwpHostPort);

        } catch (Throwable e) {
            throw new ExecutionException("Failed to connect debugger. Cause: " + e.getMessage(), e);
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
     * cloud database start's implementation : use attach connector . and using reflection to override
     * the behavior of attach connector to use the NSTunnelConnection instead of establish new connection
     * between debugger and database
     * @param session session to be passed to {@link XDebugProcess#XDebugProcess} constructor
     */
    @NotNull
    @Override
    public XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
        initConnector();
        connect();

        Executor executor = DefaultDebugExecutor.getDebugExecutorInstance();
        RunProfile runProfile = session.getRunProfile();
        assertNotNull(runProfile,"invalid run profile");


        Project project = session.getProject();
        ExecutionEnvironment environment = ExecutionEnvironmentBuilder.create(project, executor, runProfile).build();
        String host = extractHost(jdwpHostPort);
        String port = extractPort(jdwpHostPort);
        DBJdwpTcpConfig tcpConfig = new DBJdwpTcpConfig(host, Integer.parseInt(port), true);
        RemoteConnection remoteConnection = new RemoteConnection(true, host, port, false);

        RunProfileState state = Failsafe.nn(runProfile.getState(executor, environment));

        DebugEnvironment debugEnvironment = new DefaultDebugEnvironment(environment, state, remoteConnection, true);
        DebuggerManagerEx debuggerManagerEx = DebuggerManagerEx.getInstanceEx(project);
        DebuggerSession debuggerSession = debuggerManagerEx.attachVirtualMachine(debugEnvironment);
        assertNotNull(debuggerSession, "Could not initialize JDWP listener");


        return createDebugProcess(session, debuggerSession, tcpConfig);

    }
    public static String extractHost(String input) {
        int hostStartIndex = input.indexOf("host=") + 5;
        int hostEndIndex = input.indexOf(";port=");
        return input.substring(hostStartIndex, hostEndIndex);
    }

    public static String extractPort(String input) {
        int portStartIndex = input.indexOf("port=") + 5;
        return input.substring(portStartIndex);
    }

    private void initConnector() throws ExecutionException {
        VirtualMachineManager vmManager = getVirtualMachineManager();
        List<AttachingConnector> connectors = vmManager.attachingConnectors();

        AttachingConnector connector = first(connectors, c ->
                c.name().equals("com.jetbrains.jdi.SocketAttach") ||
                c.name().equals("com.sun.jdi.SocketAttach"));

        if (connector == null) throw new ExecutionException("Failed to initialise socket connector");

        TransportService transportService = createTransportService();
        patchConnector(connector, transportService);
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
            throw new ExecutionException("Failed to initialise virtual machine", e);
        }
    }

    private static void patchConnector(AttachingConnector connector, TransportService transportService) throws ExecutionException {
        try {
            Class<?> connectorClass = Commons.coalesce(
                    () -> Classes.classForName("com.jetbrains.jdi.GenericAttachingConnector"),
                    () -> Classes.classForName("com.sun.tools.jdi.GenericAttachingConnector"));

            if (connectorClass == null) throw new IllegalStateException("JDI components not accessible");

            Field declaredField = connectorClass.getDeclaredField("transportService");
            declaredField.setAccessible(true);
            declaredField.set(connector, transportService);
        } catch (Throwable e) {
            throw new ExecutionException("Failed to initialise transport service", e);
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
        System.out.println("handshake starts ...........");
        byte[] hello = "JDWP-Handshake".getBytes(StandardCharsets.UTF_8);
        readBuffer.clear();
        writeBuffer.clear();
        debugConnection.read(readBuffer);
        byte[] hello_read = new byte[hello.length];
        readBuffer.get(hello_read);
        if (Arrays.compare(hello, hello_read) == 0) { // TODO java 8 compatibility issue
            System.out.println("handshake not done");
        }
        writePackets(hello);
        readBuffer.clear();

        System.out.println("handshake finishes ...........");
    }

    // read just one packet at each time called
    // and buffer the rest
    byte[] readPackets() throws IOException {
        if (readBuffer.position() > 0) {
            // the buffer contains incomplete packet
            int packetLength = readBuffer.getInt(0);
            while(readBuffer.position() < packetLength) {
                debugConnection.read(readBuffer);
            }

            readBuffer.flip();
            byte[] packet = new byte[packetLength];
            readBuffer.get(packet);

            if (readBuffer.hasRemaining()) {
                byte[] extra = new byte[readBuffer.limit() - readBuffer.position()];
                readBuffer.get(extra);
                readBuffer.clear();
                readBuffer.put(extra);
            } else {
                readBuffer.clear();
            }

            return packet;
        }

        readBuffer.clear();
        debugConnection.read(readBuffer);
        return readPackets();

    }


    synchronized void writePackets(byte[] bytes) throws IOException {
        writeBuffer.clear();
        writeBuffer.put(bytes);
        writeBuffer.flip();
        debugConnection.write(writeBuffer);
    }
}
