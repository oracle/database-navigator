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
import com.dbn.common.util.Strings;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionPropertiesSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.info.ConnectionInfo;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.database.interfaces.DatabaseMessageParserInterface;
import com.dbn.diagnostics.DiagnosticsManager;
import com.dbn.diagnostics.data.DiagnosticBundle;
import com.dbn.driver.DatabaseDriverManager;
import com.dbn.driver.DriverBundle;
import com.dbn.driver.DriverSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;

import static com.dbn.common.util.TimeUtil.millisSince;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class ConnectionUtil {
    private ConnectionUtil() {}


    public static DBNConnection connect(ConnectionHandler connection, SessionId sessionId) throws SQLException {
        AuthenticationInfo authenticationInfo = ensureAuthenticationInfo(connection);

        return ConnectionContext.surround(connection.createConnectionContext(), () -> {
            long start = System.currentTimeMillis();
            ConnectionHandlerStatusHolder connectionStatus = connection.getConnectionStatus();

            DiagnosticsManager diagnosticsManager = DiagnosticsManager.getInstance(connection.getProject());
            DiagnosticBundle<SessionId> diagnostics = diagnosticsManager.getConnectivityDiagnostics(connection.getConnectionId());
            try {
                DatabaseCompatibilityInterface compatibility = connection.getCompatibilityInterface();
                DatabaseAttachmentHandler attachmentHandler = compatibility.getDatabaseAttachmentHandler();
                ConnectionSettings connectionSettings = connection.getSettings();
                ConnectionPropertiesSettings propertiesSettings = connectionSettings.getPropertiesSettings();


                DBNConnection conn = connect(
                        connectionSettings,
                        connectionStatus,
                        connection.getTemporaryAuthenticationInfo(),
                        sessionId,
                        propertiesSettings.isEnableAutoCommit(),
                        attachmentHandler);
                ConnectionInfo connectionInfo = new ConnectionInfo(conn.getMetaData());
                connection.setConnectionInfo(connectionInfo);
                connectionStatus.setAuthenticationError(null);
                connection.getCompatibility().read(conn.getMetaData());
                initSessionUser(connection, conn);
                diagnostics.log(sessionId, false, false, millisSince(start));
                return conn;
            } catch (SQLTimeoutException e) {
                conditionallyLog(e);
                diagnostics.log(sessionId, false, true, millisSince(start));
                throw e;
            } catch (SQLException e) {
                conditionallyLog(e);
                diagnostics.log(sessionId, true, false, millisSince(start));
                DatabaseMessageParserInterface messageParser = connection.getMessageParserInterface();
                if (messageParser.isAuthenticationException(e)) {
                    authenticationInfo.setPassword(null);
                    connectionStatus.setAuthenticationError(new AuthenticationError(authenticationInfo, e));
                }
                throw e;
            }
        });
    }

    /**
     * Initializes the session-user for the given connection handler.
     * Verifies if the session user has already been confirmed and skips the initialization if this is true
     * @param connection the database to be verified
     * @param conn the jdbc connection to be used for loading the session user
     * @throws SQLException if the load of session-user fails
     */
    private static void initSessionUser(ConnectionHandler connection, DBNConnection conn) throws SQLException {
        ConnectionDatabaseSettings databaseSettings = connection.getSettings().getDatabaseSettings();
        if (databaseSettings.getSessionUser() == null || !databaseSettings.isSessionUserConfirmed()) {
            String sessionUser = connection.getMetadataInterface().loadSessionUser(conn);
            databaseSettings.setSessionUser(sessionUser);
            databaseSettings.setSessionUserConfirmed(true);
        }
    }

    @NotNull
    private static AuthenticationInfo ensureAuthenticationInfo(ConnectionHandler connection) throws SQLException {
        // do not retry connection on authentication error unless
        // credentials changed (account can be locked on several invalid attempts)

        ConnectionHandlerStatusHolder statusHolder = connection.getConnectionStatus();
        AuthenticationError authenticationError = statusHolder.getAuthenticationError();
        AuthenticationInfo authenticationInfo = initAuthenticationInfo(connection);

        if (authenticationError != null &&
                !authenticationError.isExpired() &&
                !authenticationError.isObsolete(authenticationInfo) &&
                authenticationInfo.hasUserInformation()) {

            // prevent user account locking due to successive failed authentication attempts
            // (throw last auth exception)
            throw authenticationError.getException();
        }
        return authenticationInfo;
    }

    @NotNull
    private static AuthenticationInfo initAuthenticationInfo(ConnectionHandler connection) {
        ConnectionSettings connectionSettings = connection.getSettings();
        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
        AuthenticationInfo authenticationInfo = databaseSettings.getAuthenticationInfo();
        if (!authenticationInfo.isProvided()) {
            authenticationInfo = connection.getTemporaryAuthenticationInfo();
        }
        return authenticationInfo;
    }

    @NotNull
    public static DBNConnection connect(
            ConnectionSettings connectionSettings,
            @Nullable ConnectionHandlerStatusHolder connectionStatus,
            @Nullable AuthenticationInfo temporaryAuthenticationInfo,
            @NotNull SessionId sessionId,
            boolean autoCommit,
            @Nullable DatabaseAttachmentHandler attachmentHandler) throws SQLException {
        Connector connector = new Connector(
                sessionId,
                temporaryAuthenticationInfo,
                connectionSettings,
                connectionStatus,
                attachmentHandler,
                autoCommit);

        DBNConnection connection = connector.connect();

        SQLException exception = connector.getException();
        if (exception != null) throw exception;
        if (connection == null) throw new SQLTimeoutException("Could not connect to database. Communication timeout");

        return connection;
    }

    @Nullable
    public static Driver resolveDriver(ConnectionDatabaseSettings databaseSettings) throws Exception {
        DatabaseDriverManager driverManager = DatabaseDriverManager.getInstance();
        DriverSource driverSource = databaseSettings.getDriverSource();
        String driverClassName = databaseSettings.getDriver();
        if (Strings.isEmpty(driverClassName)) return null;

        DriverBundle drivers = null;
        if (driverSource == DriverSource.EXTERNAL) {
            File driverLibrary = databaseSettings.getDriverLibraryFile();
            if (driverLibrary == null) return null;

            drivers = driverManager.getDrivers(driverLibrary);

        } else if (driverSource == DriverSource.BUNDLED) {
            DatabaseType databaseType = databaseSettings.getDatabaseType();
            if (databaseType == null) return null;

            drivers = driverManager.getBundledDrivers(databaseType);
        }

        if (drivers == null || drivers.isEmpty()) return null;

        ConnectionId connectionId = databaseSettings.getConnectionId();
        return drivers.getDriver(driverClassName, connectionId);
    }

    public static double getDatabaseVersion(DatabaseMetaData databaseMetaData) throws SQLException {
        int majorVersion = databaseMetaData.getDatabaseMajorVersion();
        int minorVersion = databaseMetaData.getDatabaseMinorVersion();
        return Double.parseDouble(majorVersion + "." + minorVersion);
    }

    public static DatabaseType getDatabaseType(DatabaseMetaData databaseMetaData) throws SQLException {
        String productName = databaseMetaData.getDatabaseProductName();
        return DatabaseType.resolve(productName);
    }
}
