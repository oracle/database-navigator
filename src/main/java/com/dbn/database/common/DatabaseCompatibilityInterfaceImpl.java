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

package com.dbn.database.common;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.operation.DatabaseOperation;
import com.dbn.common.util.Chars;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionType;
import com.dbn.connection.ConnectorProperties;
import com.dbn.connection.DatabaseAttachmentHandler;
import com.dbn.connection.SessionId;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.ConnectionSslSettings;
import com.dbn.connection.ssl.SslConnectionManager;
import com.dbn.data.sorting.SortDirection;
import com.dbn.database.DatabaseCompatibility;
import com.dbn.database.DatabaseFeature;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.JdbcProperty;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.language.common.quotes.QuotePair;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.dbn.common.util.Classes.simpleClassName;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.connection.AuthenticationType.USER;
import static com.dbn.connection.AuthenticationType.USER_PASSWORD;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
public abstract class DatabaseCompatibilityInterfaceImpl implements DatabaseCompatibilityInterface {
    @NonNls
    private interface Property {
        String USER = "user";
        String PASSWORD = "password";

        String APPLICATION_NAME = "ApplicationName";
    }

    private final Set<DatabaseObjectTypeId> supportedObjectTypes = new HashSet<>(getSupportedObjectTypes());
    private final Set<DatabaseFeature> supportedFeatures = new HashSet<>(getSupportedFeatures());

    @Override
    public boolean supportsObjectType(DatabaseObjectTypeId objectTypeId) {
        return supportedObjectTypes.contains(objectTypeId);
    }

    @Override
    public boolean supportsFeature(DatabaseFeature feature) {
        return supportedFeatures.contains(feature);
    }

    @Override
    public boolean supportsFeature(DatabaseFeature feature, DatabaseObjectTypeId objectTypeId) {
        if (!supportsFeature(feature)) return false;
        if (!supportsObjectType(objectTypeId)) return false;

        return true;
    }

    @Override
    public boolean supportsOperation(DatabaseOperation operation) {
        return supportsFeature(operation.getFeature());
    }

    @Override
    public QuotePair getDefaultIdentifierQuotes() {
        return getIdentifierQuotes().getDefaultQuotes();
    }

    @Override
    @Nullable
    public String getDatabaseLogName() {
        return null;
    }

    @Override
    public String getOrderByClause(String columnName, SortDirection sortDirection, boolean nullsFirst) {
        return columnName + " " + sortDirection.getSqlToken() + " nulls" + (nullsFirst ? " first" : " last");
    }

    @Override
    public String getForUpdateClause() {
        return " for update";
    }

    @Override
    public String getSessionBrowserColumnName(String columnName) {
        return columnName;
    }

    @Override
    @Nullable
    public DatabaseAttachmentHandler getDatabaseAttachmentHandler() {
        return null;
    };

    public <T> T attemptFeatureInvocation(JdbcProperty feature, Callable<T> invoker) throws SQLException {
        ConnectionHandler connection = ConnectionHandler.local();
        DatabaseCompatibility compatibility = connection.getCompatibility();
        try {
            if (compatibility.isSupported(feature)) {
                return invoker.call();
            }
        } catch (SQLFeatureNotSupportedException | AbstractMethodError e) {
            conditionallyLog(e);
            log.warn("JDBC feature not supported " + feature + " (" + e.getMessage() + ")");
            compatibility.markUnsupported(feature);
        }
        return null;
    }

    @Override
    public ConnectorProperties createConnectorProperties() {
        Map<String, String> implicitProperties = getImplicitConnectionProperties();
        ConnectorProperties connectorProperties = new ConnectorProperties();
        connectorProperties.addAll(implicitProperties);
        return connectorProperties;
    }


    @Override
    public void initConnectorSslConnection(ConnectorProperties properties, ConnectionSettings settings) {
        ConnectionSslSettings sslSettings = settings.getSslSettings();
        if (!sslSettings.isActive()) return;

        SslConnectionManager connectionManager = SslConnectionManager.getInstance();
        connectionManager.ensureSslConnection(settings);
    }

    @Override
    public void initConnectorDebugger(ConnectorProperties properties, ConnectionSettings settings) {}

    @Override
    public void initConnectorFileAttachments(ConnectionSettings settings, Connection connection) {}

    @Override
    public void initConnectorAuthentication(ConnectorProperties properties, @Nullable AuthenticationInfo authenticationInfo) {
        if (authenticationInfo == null) return;

        AuthenticationType authenticationType = authenticationInfo.getType();
        if (authenticationType == null) return;

        if (authenticationType.isOneOf(USER, USER_PASSWORD)) {
            String user = authenticationInfo.getUser();
            if (isNotEmpty(user)) {
                properties.add(Property.USER, user);
            }
        }

        if (authenticationType == USER_PASSWORD) {
            char[] password = authenticationInfo.getPassword();
            if (Chars.isNotEmpty(password)) {
                properties.add(Property.PASSWORD, Chars.toString(password));
            }
        }
    }


    @Override
    public void initConnectorSession(ConnectorProperties properties, ConnectionSettings settings, SessionId sessionId) {
        if (!settings.isSigned()) return;

        ConnectionType connectionType = sessionId.getConnectionType();
        String appName = "DB Navigator - " + connectionType.getName();
        properties.add(Property.APPLICATION_NAME, appName);

    }

    @Override
    public boolean resetConnectorAndRetry(Throwable e, ConnectionSettings settings) {
        if (!settings.isSigned()) return false;

        // DBN-524 strongly asserted property names
        String message = nvl(e.getMessage(), simpleClassName(e));
        if (message.contains(Property.APPLICATION_NAME)) {
            settings.setSigned(false);
            return true;
        }
        return false;
    }
}
