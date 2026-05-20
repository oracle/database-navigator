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

package com.dbn.connection;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.network.NetworkAddress;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Timeout;
import com.dbn.common.ui.dialog.ExceptionTreeDialog;
import com.dbn.common.util.Classes;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionPropertiesSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.ConnectionSshTunnelSettings;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.ssh.SshTunnelConnector;
import com.dbn.connection.ssh.SshTunnelManager;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.diagnostics.Diagnostics;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import static com.dbn.common.exception.Exceptions.getMessage;
import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.common.thread.Dispatch.getCurrentModalityState;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.diagnostics.data.Activity.CONNECT;

@Getter
class Connector {
    private final SessionId sessionId;
    private final AuthenticationInfo authenticationInfo;
    private final ConnectionSettings connectionSettings;
    private final ConnectionHandlerStatusHolder connectionStatus;
    private final boolean autoCommit;
    private SQLException exception;

    Connector(
            SessionId sessionId,
            AuthenticationInfo authenticationInfo,
            ConnectionSettings connectionSettings,
            ConnectionHandlerStatusHolder connectionStatus,
            boolean autoCommit) {
        this.sessionId = sessionId;
        this.authenticationInfo = authenticationInfo;
        this.connectionSettings = connectionSettings;
        this.connectionStatus = connectionStatus;
        this.autoCommit = autoCommit;
    }


    private int getConnectTimeout() {
        int timeout = connectionSettings.getDetailSettings().getConnectivityTimeoutSeconds();
        int timeoutExtension = 0;

        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
        boolean driversLoaded = databaseSettings.driversLoaded();
        if (!driversLoaded) timeoutExtension += 30; // allow 30 seconds for drivers to load
        if (databaseSettings.isInteractiveAuthentication()) timeoutExtension += 120; // allow 2 additional minutes for interactive login

        return timeout + timeoutExtension;
    }



    @Nullable
    public DBNConnection connect() {
        int connectTimeout = getConnectTimeout();
        String identifier = "Connecting to \"" + connectionSettings.getDatabaseSettings().getName() + "\"";
        return Timeout.call(identifier, connectTimeout, null, true, () -> doConnect());
    }

    private DBNConnection doConnect() {
        //trace(this);
        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();

        DatabaseType databaseType = databaseSettings.getDatabaseType();
        if (databaseType == DatabaseType.GENERIC) {
            databaseType = DatabaseType.resolve(databaseSettings.getDriver());
        }
        DatabaseInterfaces databaseInterfaces = DatabaseInterfacesBundle.get(databaseType);
        DatabaseCompatibilityInterface compatibility = databaseInterfaces.getCompatibilityInterface();

        Driver driver = null;
        AuthenticationInfo authenticationInfo = null;
        try {
            ConnectorProperties properties = compatibility.createConnectorProperties();

            // AUTHENTICATION
            authenticationInfo = databaseSettings.getAuthenticationInfo();
            if (!authenticationInfo.isProvided() && this.authenticationInfo != null) {
                authenticationInfo = this.authenticationInfo;
            }
            if (!databaseSettings.isConfigHttps()) {
                compatibility.initConnectorAuthentication(properties, authenticationInfo);
            }

            // SESSION INFO
            compatibility.initConnectorSession(properties, connectionSettings, sessionId);

            // DEBUGGER
            compatibility.initConnectorDebugger(properties, connectionSettings);

            // PROPERTIES
            // add missing - prevent overriding the OCI properties
            ConnectionPropertiesSettings propertiesSettings = connectionSettings.getPropertiesSettings();
            properties.addMissing(propertiesSettings.getProperties());

            // DRIVER
            driver = ConnectionUtil.resolveDriver(databaseSettings);
            if (driver == null) {
                throw new SQLException("Could not resolve driver class.");
            }

            // SSL CONNECTION
            compatibility.initConnectorSslConnection(properties, connectionSettings);

            String connectionUrl = databaseSettings.getConnectionUrl();
            if (databaseSettings.isConfigFile()) {
                connectionUrl = databaseSettings.getConnectionUrlForConnect();
            }

            // SSH Tunnel
            ConnectionSshTunnelSettings sshTunnelSettings = connectionSettings.getSshTunnelSettings();
            if (sshTunnelSettings.isActive() && !databaseSettings.isConfigFile()) {
                SshTunnelManager sshTunnelManager = SshTunnelManager.getInstance();
                SshTunnelConnector sshTunnelConnector = sshTunnelManager.ensureSshConnection(connectionSettings);
                if (sshTunnelConnector != null) {
                    NetworkAddress localAddress = sshTunnelConnector.getLocalAddress();
                    String localHost = localAddress.getHost();
                    String localPort = localAddress.getPortString();
                    connectionUrl = databaseSettings.getConnectionUrl(localHost, localPort);
                }
            }
            Diagnostics.databaseLag(CONNECT);

            Connection connection = connect(driver, connectionUrl, properties.export());
            if (connection == null) {
                throw new SQLException("Driver failed to create connection. No failure information provided by jdbc vendor.");
            }

            if (connectionStatus != null) {
                connectionStatus.setConnectionException(null);
                connectionStatus.setConnected(true);
                connectionStatus.setValid(true);
            }

            // FILE ATTACHMENTS
            compatibility.initConnectorFileAttachments(connectionSettings, connection);

            ConnectionType connectionType = sessionId.getConnectionType();
            DatabaseMetaData metaData = connection.getMetaData();
            databaseType = ConnectionUtil.getDatabaseType(metaData);
            databaseSettings.setConfirmedDatabaseType(databaseType);
            databaseSettings.setDatabaseVersion(ConnectionUtil.getDatabaseVersion(metaData));
            databaseSettings.setConnectivityStatus(ConnectivityStatus.VALID);
            String connectionName = connectionSettings.getDatabaseSettings().getName();
            ConnectionId connectionId = connectionSettings.getConnectionId();

            Project project = connectionSettings.getProject();
            DBNConnection conn = DBNConnection.wrap(
                    project,
                    connection,
                    databaseType,
                    connectionType,
                    connectionId,
                    connectionName,
                    sessionId);

            Resources.setAutoCommit(conn, autoCommit);
            return conn;

        } catch (Throwable e) {
            conditionallyLog(e);
            if (compatibility.resetConnectorAndRetry(e, connectionSettings)) return connect();

            databaseType = DatabaseType.resolve(databaseSettings.getDriver());
            databaseSettings.setConfirmedDatabaseType(databaseType);
            databaseSettings.setConnectivityStatus(ConnectivityStatus.INVALID);
            if (connectionStatus != null) {
                connectionStatus.setConnectionException(e);
                connectionStatus.setValid(false);
            }

            String message = getMessage(e);
            exception = toSqlException(e, "Connection error: " + message);

            // if we have all the info we need, pass this on to the
            // compatibility layer to see if there is any extra additional processing necessary.
            if (authenticationInfo != null) {
               ConnectionExceptionInfo info = new ConnectionExceptionInfo(e,
                       driver != null ? driver.getClass().getClassLoader() : null,
                       authenticationInfo);
               compatibility.handleConnectionException(info);
            }

            if (Diagnostics.isDeveloperMode() &&  sessionId == SessionId.TEST) {
                Dispatch.execute(getCurrentModalityState(), () -> {
                    new ExceptionTreeDialog(exception).show();
                });

            }
        }
        return null;
    }

    private static Connection connect(Driver driver, String url, Properties properties) throws SQLException {
        return Classes.withClassLoader(driver, () -> driver.connect(url, properties));
    }
}
