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

import com.dbn.common.constant.Constants;
import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.network.NetworkAddress;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Timeout;
import com.dbn.common.ui.dialog.ExceptionTreeDialog;
import com.dbn.common.util.Chars;
import com.dbn.common.util.Classes;
import com.dbn.common.util.Strings;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.ConnectionSshTunnelSettings;
import com.dbn.connection.config.ConnectionSslSettings;
import com.dbn.connection.config.file.DatabaseFile;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.ssh.SshTunnelConnector;
import com.dbn.connection.ssh.SshTunnelManager;
import com.dbn.connection.ssl.SslConnectionManager;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.diagnostics.Diagnostics;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.*;

import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.common.notification.NotificationCategory.CONNECTION;
import static com.dbn.common.notification.NotificationSupport.sendErrorNotification;
import static com.dbn.common.thread.Dispatch.getCurrentModalityState;
import static com.dbn.common.util.Classes.simpleClassName;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Maps.toProperties;
import static com.dbn.connection.AuthenticationTokenType.AZURE_INTERACTIVE;
import static com.dbn.connection.AuthenticationTokenType.AZURE_SERVICE_PRINCIPAL_CERTIFICATE;
import static com.dbn.connection.AuthenticationTokenType.AZURE_SERVICE_PRINCIPAL_TOKEN;
import static com.dbn.connection.AuthenticationTokenType.OCI_API_KEY;
import static com.dbn.connection.AuthenticationTokenType.OCI_INTERACTIVE;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.diagnostics.data.Activity.CONNECT;
import static com.dbn.nls.NlsResources.txt;

@Getter
class Connector {
    @NonNls
    private interface Property {
        String APPLICATION_NAME = "ApplicationName";
        String SESSION_PROGRAM = "v$session.program";

        String USER = "user";
        String PASSWORD = "password";

        String SSL = "ssl";
        String USE_SSL = "useSSL";
        String REQUIRE_SSL = "requireSSL";

        // TODO: can we reference the raw Oracle connection type rather than the
        //  generic JDBC ones to get these constants.
        String ORACLE_JDBC_OCI_PROFILE = "oracle.jdbc.ociProfile";
        String ORACLE_JDBC_OCI_CONFIG_FILE = "oracle.jdbc.ociConfigFile";
        // if this value is set, it puts the driver into on of the token auth
        // modes based setting one of PropertyValue.TOKEN_AUTHENTICATION_*
        String ORACLE_JDBC_TOKEN_AUTHENTICATION = "oracle.jdbc.tokenAuthentication";
        String ORACLE_JDBC_DEBUG_JDWP = "oracle.jdbc.debugJDWP";
        String ORACLE_JDBC_AZURE_DATABASE_APPLICATION_ID_URI = "oracle.jdbc.azureDatabaseApplicationIdUri";
        String ORACLE_JDBC_AZURE_CLIENT_CERTIFICATE_FILE = "oracle.jdbc.clientCertificate";
        String ORACLE_JDBC_AZURE_CLIENT_CERTIFICATE_PASSWORD = "oracle.jdbc.clientCertificatePassword";
        String ORACLE_JDBC_AZURE_CLIENT_SECRET = "oracle.jdbc.clientSecret";
        String ORACLE_JDBC_AZURE_CLIENT_ID = "oracle.jdbc.clientId";
        String ORACLE_JDBC_AZURE_TENANT_ID = "oracle.jdbc.tenantId";
        String ORACLE_JDBC_SSL_SERVER_DN_MATCH = "oracle.net.ssl_server_dn_match";
    }

    @NonNls
    private interface PropertyValue {
        String TOKEN_AUTHENTICATION_OCI_API_KEY = "OCI_API_KEY";
        String TOKEN_AUTHENTICATION_OCI_INTERACTIVE = "OCI_INTERACTIVE";
        String TOKEN_AUTHENTICATION_AZURE_INTERACTIVE = "AZURE_INTERACTIVE";
        String TOKEN_AUTHENTICATION_AZURE_SERVICE_PRINCIPAL = "AZURE_SERVICE_PRINCIPAL";
    }

    private final SessionId sessionId;
    private final AuthenticationInfo authenticationInfo;
    private final ConnectionSettings connectionSettings;
    private final ConnectionHandlerStatusHolder connectionStatus;
    private final DatabaseAttachmentHandler databaseAttachmentHandler;
    private final boolean autoCommit;
    private SQLException exception;

    Connector(
            SessionId sessionId,
            AuthenticationInfo authenticationInfo,
            ConnectionSettings connectionSettings,
            ConnectionHandlerStatusHolder connectionStatus,
            DatabaseAttachmentHandler databaseAttachmentHandler,
            boolean autoCommit) {
        this.sessionId = sessionId;
        this.authenticationInfo = authenticationInfo;
        this.connectionSettings = connectionSettings;
        this.connectionStatus = connectionStatus;
        this.databaseAttachmentHandler = databaseAttachmentHandler;
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
        // we need some of these in the catch condition of this tru
        Optional<DatabaseCompatibilityInterface> dbCompatibility = Optional.empty();
        Optional<AuthenticationInfo> authInfo = Optional.empty();
        Driver driver = null;
        try {
            DatabaseType databaseType = databaseSettings.getDatabaseType();
            if (databaseType == DatabaseType.GENERIC) {
                databaseType = DatabaseType.resolve(databaseSettings.getDriver());
            }
            DatabaseInterfaces databaseInterfaces = DatabaseInterfacesBundle.get(databaseType);
            DatabaseCompatibilityInterface compatibilityInterface = databaseInterfaces.getCompatibilityInterface();
            dbCompatibility = Optional.ofNullable(compatibilityInterface);

            Map<String, String> implicitProperties = compatibilityInterface.getImplicitConnectionProperties();
            Map<String, String> properties = new HashMap<>(implicitProperties);

            // AUTHENTICATION
            AuthenticationInfo authenticationInfo = databaseSettings.getAuthenticationInfo();
            if (!authenticationInfo.isProvided() && this.authenticationInfo != null) {
                authenticationInfo = this.authenticationInfo;
            }
            authInfo = Optional.of(authenticationInfo);
            AuthenticationType authenticationType = authenticationInfo.getType();
            if (Constants.isOneOf(authenticationType, AuthenticationType.USER, AuthenticationType.USER_PASSWORD)) {
                String user = authenticationInfo.getUser();
                if (Strings.isNotEmpty(user)) {
                    properties.put(Property.USER, user);
                }

                if (authenticationType == AuthenticationType.USER_PASSWORD) {
                    char[] password = authenticationInfo.getPassword();
                    if (Chars.isNotEmpty(password)) {
                        properties.put(Property.PASSWORD, Chars.toString(password));
                    }
                }
            }

            // Token Auth
            if (authenticationType == AuthenticationType.TOKEN) {
                // TODO move this logic to "com.dbn.database.interfaces" - maybe a new DatabaseConnectivityInterface
                AuthenticationTokenType tokenType = authenticationInfo.getTokenType();
                if (tokenType == OCI_INTERACTIVE) {
                    properties.put(Property.ORACLE_JDBC_TOKEN_AUTHENTICATION, PropertyValue.TOKEN_AUTHENTICATION_OCI_INTERACTIVE);
                }
                else if (tokenType == OCI_API_KEY) {
                    properties.put(Property.ORACLE_JDBC_TOKEN_AUTHENTICATION, PropertyValue.TOKEN_AUTHENTICATION_OCI_API_KEY);
                    properties.put(Property.ORACLE_JDBC_OCI_CONFIG_FILE, nvl(authenticationInfo.getTokenConfigFile(), ""));
                    properties.put(Property.ORACLE_JDBC_OCI_PROFILE, nvl(authenticationInfo.getTokenProfile(), ""));
                } else if (tokenType == AZURE_INTERACTIVE) {
                    properties.put(Property.ORACLE_JDBC_TOKEN_AUTHENTICATION, PropertyValue.TOKEN_AUTHENTICATION_AZURE_INTERACTIVE);
                    properties.put(Property.ORACLE_JDBC_AZURE_DATABASE_APPLICATION_ID_URI, authenticationInfo.getAzureDatabaseApplicationIdUri());
                } else if (tokenType == AZURE_SERVICE_PRINCIPAL_CERTIFICATE) {
                    copyCommonAzureServicePrincipalProperties(properties, authenticationInfo);
                    properties.put(Property.ORACLE_JDBC_AZURE_CLIENT_CERTIFICATE_FILE, authenticationInfo.getAzureClientCertificateFile());
                    properties.put(Property.ORACLE_JDBC_AZURE_CLIENT_CERTIFICATE_PASSWORD,
                            Chars.toStringAcceptEmpty(authenticationInfo.getAzureClientCertificatePassword()));
                } else if (tokenType == AZURE_SERVICE_PRINCIPAL_TOKEN) {
                    copyCommonAzureServicePrincipalProperties(properties, authenticationInfo);
                    properties.put(Property.ORACLE_JDBC_AZURE_CLIENT_SECRET, Chars.toStringAcceptEmpty(authenticationInfo.getAzureClientSecret()));
                }  else {
                    //TODO...
                }
            }

            // SESSION INFO
            ConnectionType connectionType = sessionId.getConnectionType();
            String appName = "Database Navigator - " + connectionType.getName();
            if (connectionSettings.isSigned()) {
                properties.put(Property.APPLICATION_NAME, appName);
            }

            // PROPERTIES
            Map<String, String> configProperties = connectionSettings.getPropertiesSettings().getProperties();
            if (databaseType == DatabaseType.ORACLE) {
                // TODO move this logic to "com.dbn.database.interfaces" - maybe a new DatabaseConnectivityInterface

                properties.put(Property.SESSION_PROGRAM, appName);
                // i check if we have got jdwpHostPort if yes i get a connection using CONNECTION_PROPERTY_THIN_DEBUG_JDWP property
                // TODO jdwpHostPort may remain resident if this stage is not reached for any reason... (maybe add transient properties container to settings)
                String jdwpHostPort = configProperties.remove("jdwpHostPort");
                if (Strings.isNotEmpty(jdwpHostPort)) {
                    properties.put(Property.ORACLE_JDBC_DEBUG_JDWP, jdwpHostPort);
                }
            }
            // TODO: if token_auth stop the settings from overriding the OCI properties?
            properties.putAll(configProperties);

            // DRIVER
            driver = ConnectionUtil.resolveDriver(databaseSettings);
            if (driver == null) {
                throw new SQLException("Could not resolve driver class.");
            }

            // SSL
            ConnectionSslSettings sslSettings = connectionSettings.getSslSettings();
            if (sslSettings.isActive()) {
                SslConnectionManager connectionManager = SslConnectionManager.getInstance();
                connectionManager.ensureSslConnection(connectionSettings);
                if (databaseType == DatabaseType.MYSQL) {
                    properties.put(Property.USE_SSL, "true");
                    properties.put(Property.REQUIRE_SSL, "true");
                } else if (databaseType == DatabaseType.POSTGRES) {
                    properties.put(Property.SSL, "true");
                }
            }

            String connectionUrl = databaseSettings.getConnectionUrl();

            // SSH Tunnel
            ConnectionSshTunnelSettings sshTunnelSettings = connectionSettings.getSshTunnelSettings();
            if (sshTunnelSettings.isActive()) {
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

            Connection connection = connect(driver, connectionUrl, toProperties(properties));
            if (connection == null) {
                throw new SQLException("Driver failed to create connection. No failure information provided by jdbc vendor.");
            }

            if (connectionStatus != null) {
                connectionStatus.setConnectionException(null);
                connectionStatus.setConnected(true);
                connectionStatus.setValid(true);
            }

            Project project = connectionSettings.getProject();
            if (databaseAttachmentHandler != null) {
                List<DatabaseFile> attachedFiles = databaseSettings.getDatabaseInfo().getAttachedFiles();
                for (DatabaseFile databaseFile : attachedFiles) {
                    String filePath = databaseFile.getPath();
                    try {
                        databaseAttachmentHandler.attachDatabase(connection, filePath, databaseFile.getSchema());
                    } catch (Exception e) {
                        conditionallyLog(e);
                        sendErrorNotification(project, CONNECTION, txt("ntf.connection.error.UnableToAttachFile", filePath, e));
                    }
                }
            }

            DatabaseMetaData metaData = connection.getMetaData();
            databaseType = ConnectionUtil.getDatabaseType(metaData);
            databaseSettings.setConfirmedDatabaseType(databaseType);
            databaseSettings.setDatabaseVersion(ConnectionUtil.getDatabaseVersion(metaData));
            databaseSettings.setConnectivityStatus(ConnectivityStatus.VALID);
            String connectionName = connectionSettings.getDatabaseSettings().getName();
            ConnectionId connectionId = connectionSettings.getConnectionId();

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
            String message = nvl(e.getMessage(), simpleClassName(e));
            if (connectionSettings.isSigned()) {
                // DBN-524 strongly asserted property names
                if (message.contains(Property.APPLICATION_NAME)) {
                    connectionSettings.setSigned(false);
                    return connect();
                }
            }

            DatabaseType databaseType = DatabaseType.resolve(databaseSettings.getDriver());
            databaseSettings.setConfirmedDatabaseType(databaseType);
            databaseSettings.setConnectivityStatus(ConnectivityStatus.INVALID);
            if (connectionStatus != null) {
                connectionStatus.setConnectionException(e);
                connectionStatus.setValid(false);
            }
            exception = toSqlException(e, "Connection error: " + message);
            // if we have all the info we need, pass this on to the
            // compatibility layer to see if there is any extra additional processing
            // necessary.
            if (dbCompatibility.isPresent() && authInfo.isPresent()) {
               ConnectionExceptionInfo info = new ConnectionExceptionInfo(e,
                       driver != null ? driver.getClass().getClassLoader() : null,
                       authInfo.get());
               dbCompatibility.get().handleConnectionException(info);
            }

            if (Diagnostics.isDeveloperMode() &&  sessionId == SessionId.TEST) {
                Dispatch.execute(getCurrentModalityState(), () -> {
                    new ExceptionTreeDialog(exception).show();
                });

            }
        }
        return null;
    }

    private void copyCommonAzureServicePrincipalProperties(Map<String, String> properties, AuthenticationInfo authenticationInfo) {
        properties.put(Property.ORACLE_JDBC_TOKEN_AUTHENTICATION, PropertyValue.TOKEN_AUTHENTICATION_AZURE_SERVICE_PRINCIPAL);
        properties.put(Property.ORACLE_JDBC_SSL_SERVER_DN_MATCH, "yes");
        properties.put(Property.ORACLE_JDBC_AZURE_CLIENT_ID, authenticationInfo.getAzureClientId());
        properties.put(Property.ORACLE_JDBC_AZURE_TENANT_ID, authenticationInfo.getAzureTenantId());
        properties.put(Property.ORACLE_JDBC_AZURE_DATABASE_APPLICATION_ID_URI, authenticationInfo.getAzureDatabaseApplicationIdUri());
    }

    private static Connection connect(Driver driver, String url, Properties properties) throws SQLException {
        return Classes.withClassLoader(driver, d -> d.connect(url, properties));
    }
}
