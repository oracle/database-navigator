package com.dbn.connection;

import com.dbn.common.constant.Constants;
import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.thread.Timeout;
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
import com.dbn.diagnostics.Diagnostics;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.common.notification.NotificationGroup.CONNECTION;
import static com.dbn.common.notification.NotificationSupport.sendErrorNotification;
import static com.dbn.common.util.Commons.nvl;
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
        String ORACLE_JDBC_TOKEN_AUTHENTICATION = "oracle.jdbc.tokenAuthentication";
        String ORACLE_JDBC_DEBUG_JDWP = "oracle.jdbc.debugJDWP";
    }

    private interface PropertyValue {
        String TOKEN_AUTHENTICATION_OCI_API_KEY = "OCI_API_KEY";
        String TOKEN_AUTHENTICATION_OCI_INTERACTIVE = "OCI_INTERACTIVE";
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
        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
        boolean driversLoaded = databaseSettings.driversLoaded();
        int connectTimeoutExtension = driversLoaded ? 0 : 20; // allow 20 seconds for drivers to load
        int connectTimeout = 300; //connectTimeoutExtension + connectionSettings.getDetailSettings().getConnectivityTimeoutSeconds();
        return connectTimeout;
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
        try {
            Properties properties = new Properties();

            // AUTHENTICATION
            AuthenticationInfo authenticationInfo = databaseSettings.getAuthenticationInfo();
            if (!authenticationInfo.isProvided() && this.authenticationInfo != null) {
                authenticationInfo = this.authenticationInfo;
            }

            AuthenticationType authenticationType = authenticationInfo.getType();
            if (Constants.isOneOf(authenticationType, AuthenticationType.USER, AuthenticationType.USER_PASSWORD)) {
                String user = authenticationInfo.getUser();
                if (Strings.isNotEmpty(user)) {
                    properties.put(Property.USER, user);
                }

                if (authenticationType == AuthenticationType.USER_PASSWORD) {
                    String password = authenticationInfo.getPassword();
                    if (Strings.isNotEmpty(password)) {
                        properties.put(Property.PASSWORD, password);
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
                } else {
                    //TODO...
                }
            }

            DatabaseType databaseType = databaseSettings.getDatabaseType();
            if (databaseType == DatabaseType.GENERIC) {
                databaseType = DatabaseType.resolve(databaseSettings.getDriver());
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
            Driver driver = ConnectionUtil.resolveDriver(databaseSettings);
            if (driver == null) {
                throw new SQLException("Could not resolve driver class.");
            }

            // SSL
            ConnectionSslSettings sslSettings = connectionSettings.getSslSettings();
            if (sslSettings.isActive()) {
                SslConnectionManager connectionManager = SslConnectionManager.getInstance();
                connectionManager.ensureSslConnection(connectionSettings);
                if (databaseType == DatabaseType.MYSQL) {
                    properties.setProperty(Property.USE_SSL, "true");
                    properties.setProperty(Property.REQUIRE_SSL, "true");
                } else if (databaseType == DatabaseType.POSTGRES) {
                    properties.setProperty(Property.SSL, "true");
                }
            }

            String connectionUrl = databaseSettings.getConnectionUrl();

            // SSH Tunnel
            ConnectionSshTunnelSettings sshTunnelSettings = connectionSettings.getSshTunnelSettings();
            if (sshTunnelSettings.isActive()) {
                SshTunnelManager sshTunnelManager = SshTunnelManager.getInstance();
                SshTunnelConnector sshTunnelConnector = sshTunnelManager.ensureSshConnection(connectionSettings);
                if (sshTunnelConnector != null) {
                    String localHost = sshTunnelConnector.getLocalHost();
                    String localPort = Integer.toString(sshTunnelConnector.getLocalPort());
                    connectionUrl = databaseSettings.getConnectionUrl(localHost, localPort);
                }
            }
            Diagnostics.databaseLag(CONNECT);

            Connection connection = connect(driver, connectionUrl, properties);
            if (connection == null) {
                throw new SQLException("Driver failed to create connection " + connectionUrl + ". No failure information provided by jdbc vendor.");
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
            DBNConnection conn = DBNConnection.wrap(
                    project,
                    connection,
                    connectionSettings.getDatabaseSettings().getName(),
                    connectionType,
                    connectionSettings.getConnectionId(),
                    sessionId);

            Resources.setAutoCommit(conn, autoCommit);
            return conn;

        } catch (Throwable e) {
            conditionallyLog(e);
            String message = nvl(e.getMessage(), e.getClass().getSimpleName());
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
        }
        return null;
    }

    private static Connection connect(Driver driver, String url, Properties properties) throws SQLException {
        return Classes.withClassLoader(driver, d -> d.connect(url, properties));
    }
}
