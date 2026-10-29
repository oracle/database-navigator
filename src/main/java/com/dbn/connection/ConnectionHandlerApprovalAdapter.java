/*
 * Copyright 2026 Oracle and/or its affiliates
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

import com.dbn.common.approval.UserApprovalAction;
import com.dbn.common.approval.UserApprovalAdapter;
import com.dbn.common.approval.UserApprovalOption;
import com.dbn.common.checksum.Checksum;
import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionPropertiesSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.ConnectionSshTunnelSettings;
import com.dbn.connection.config.ConnectionSslSettings;
import com.dbn.connection.config.provider.ConfigProviderInfo;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.dbn.common.approval.UserApprovalAction.CONNECTION_WORKSPACE_RESTORE;
import static com.dbn.common.checksum.ChecksumType.SHA_256;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.common.util.Strings.truncateWithMiddleEllipsis;
import static com.dbn.nls.NlsResources.txt;

/**
 * Prepares user approval information for restoring workspace connections and reconnecting
 * to database targets that were saved in project state.
 */
public class ConnectionHandlerApprovalAdapter implements UserApprovalAdapter<ConnectionHandler> {
    private static final int TARGET_MAX_LENGTH = 60;
    private static final UserApprovalOption[] APPROVAL_OPTIONS = {
            UserApprovalOption.one(txt("msg.shared.button.TrustAndConnect")),
            UserApprovalOption.all(txt("msg.connection.button.TrustAllAndConnect")),
            UserApprovalOption.none(txt("msg.shared.button.Cancel"))};

    @Override
    public Class<ConnectionHandler> getApprovalClass() {
        return ConnectionHandler.class;
    }

    @Override
    public UserApprovalAction getApprovalAction() {
        return CONNECTION_WORKSPACE_RESTORE;
    }

    @Override
    public String getApprovalTitle(ConnectionHandler connection) {
        return txt("msg.connection.title.TrustConnection");
    }

    @Override
    public String getApprovalMessage(ConnectionHandler connection) {
        ConnectionDatabaseSettings databaseSettings = connection.getSettings().getDatabaseSettings();
        AuthenticationInfo authenticationInfo = databaseSettings.getAuthenticationInfo();
        return txt("msg.connection.question.TrustConnection",
                databaseSettings.getName(),
                getConnectionTarget(databaseSettings),
                databaseSettings.getDatabaseType().getName(),
                nvl(databaseSettings.getDriver(), ""),
                authenticationInfo.getType());
    }

    @Override
    @NonNls
    public String getApprovalKey(ConnectionHandler connection) {
        Project project = connection.getProject();
        return "connection:" + nvl(project.getBasePath(), "") + ":" + connection.getConnectionId().id();
    }

    @Override
    @NonNls
    public String getApprovalSignature(ConnectionHandler connection) {
        return Checksum.fromStringContent(getSignatureContent(connection), SHA_256);
    }

    @Override
    public UserApprovalOption[] getApprovalOptions(ConnectionHandler connection) {
        return APPROVAL_OPTIONS;
    }

    @Override
    public List<ConnectionHandler> getApprovalSiblings(ConnectionHandler connection, UserApprovalOption option) {
        return connection.getConnectionBundle().getAllConnections().stream()
                .filter(c -> !c.isVirtual())
                .toList();
    }

    private static String getConnectionTarget(ConnectionDatabaseSettings databaseSettings) {
        String connectionUrl = databaseSettings.getConnectionUrl();
        if (isNotEmpty(connectionUrl)) return truncateWithMiddleEllipsis(connectionUrl, TARGET_MAX_LENGTH);

        DatabaseInfo databaseInfo = databaseSettings.getDatabaseInfo();
        String host = nvl(databaseInfo.getHost(), "");
        String port = nvl(databaseInfo.getPort(), "");
        return port.isEmpty() ? host : host + ":" + port;
    }

    private static String getSignatureContent(ConnectionHandler connection) {
        ConnectionSettings settings = connection.getSettings();
        ConnectionDatabaseSettings databaseSettings = settings.getDatabaseSettings();
        ConnectionPropertiesSettings propertiesSettings = settings.getPropertiesSettings();
        AuthenticationInfo authenticationInfo = databaseSettings.getAuthenticationInfo();
        DatabaseInfo databaseInfo = databaseSettings.getDatabaseInfo();
        ConnectionSslSettings sslSettings = settings.getSslSettings();
        ConnectionSshTunnelSettings sshSettings = settings.getSshTunnelSettings();
        ConfigProviderInfo providerInfo = databaseSettings.getConfigProviderInfo();

        StringBuilder builder = new StringBuilder();
        appendToken(builder, connection.getConnectionId().id());
        appendToken(builder, databaseSettings.getName());
        appendToken(builder, databaseSettings.getDatabaseType().name());
        appendToken(builder, databaseSettings.getDriver());
        appendToken(builder, databaseSettings.getDriverLibrary());
        appendToken(builder, databaseSettings.getDriverSource());
        appendToken(builder, databaseSettings.getConnectionUrl());
        appendToken(builder, databaseInfo.getHost());
        appendToken(builder, databaseInfo.getPort());
        appendToken(builder, databaseInfo.getDatabase());
        appendToken(builder, databaseInfo.getTnsFolder());
        appendToken(builder, databaseInfo.getTnsProfile());
        appendToken(builder, databaseInfo.getProtocol());
        appendToken(builder, databaseInfo.getServerType());
        appendToken(builder, authenticationInfo.getType());
        appendToken(builder, authenticationInfo.getUser());
        appendToken(builder, authenticationInfo.getAdminPrivilege());
        appendToken(builder, authenticationInfo.getTokenType());
        appendToken(builder, authenticationInfo.getTokenConfigFile());
        appendToken(builder, authenticationInfo.getTokenProfile());
        appendToken(builder, authenticationInfo.getDatabaseOcid());
        appendToken(builder, authenticationInfo.getCompartmentOcid());
        appendToken(builder, authenticationInfo.getAzureDatabaseApplicationIdUri());
        appendToken(builder, authenticationInfo.getAzureClientId());
        appendToken(builder, authenticationInfo.getAzureTenantId());
        appendToken(builder, authenticationInfo.getAzureClientCertificateFile());
        appendToken(builder, sslSettings.isActive());
        appendToken(builder, sslSettings.getCertificateAuthorityFile());
        appendToken(builder, sslSettings.getClientCertificateFile());
        appendToken(builder, sslSettings.getClientKeyFile());
        appendToken(builder, sshSettings.isActive());
        appendToken(builder, sshSettings.getHost());
        appendToken(builder, sshSettings.getPort());
        appendToken(builder, sshSettings.getUser());
        appendToken(builder, sshSettings.getAuthType());
        appendToken(builder, sshSettings.getKeyFile());
        appendToken(builder, databaseSettings.getConfigType());
        appendToken(builder, providerInfo.getProviderSourceType());
        appendToken(builder, providerInfo.getCloudProviderType());
        appendToken(builder, providerInfo.getCloudAuthenticationType());
        appendToken(builder, providerInfo.getProviderLocation());
        appendToken(builder, providerInfo.getProviderProfileKey());
        appendToken(builder, providerInfo.getOciConfigFile());
        appendToken(builder, providerInfo.getOciConfigProfile());
        appendToken(builder, providerInfo.getAwsRegion());
        appendToken(builder, providerInfo.getGcpStorageProject());
        appendToken(builder, providerInfo.getGcpStorageBucket());
        appendToken(builder, providerInfo.getGcpStorageObject());
        appendToken(builder, providerInfo.getAzureAppConfigLabel());
        appendToken(builder, providerInfo.getAzureClientId());
        appendToken(builder, providerInfo.getAzureTenantId());
        appendToken(builder, providerInfo.getAzureClientCertificatePath());
        appendToken(builder, providerInfo.getHashicorpVaultAddress());
        appendToken(builder, providerInfo.getHashicorpVaultNamespace());
        appendToken(builder, providerInfo.getHashicorpVaultUsername());
        appendToken(builder, providerInfo.getHashicorpUserpassAuthPath());
        appendToken(builder, providerInfo.getHashicorpAppRoleId());
        appendToken(builder, providerInfo.getHashicorpAppRoleAuthPath());
        appendToken(builder, providerInfo.getHashicorpGithubAuthPath());
        appendProperties(builder, databaseInfo.getParameters());
        appendProperties(builder, propertiesSettings.getProperties());
        return builder.toString();
    }

    private static void appendProperties(StringBuilder builder, Map<String, String> properties) {
        new TreeMap<>(properties).forEach((key, value) -> {
            appendToken(builder, key);
            appendToken(builder, value);
        });
    }

    private static void appendToken(StringBuilder builder, Object token) {
        String value = token == null ? "" : token.toString();
        builder.append(value.length()).append(':').append(value);
    }
}
