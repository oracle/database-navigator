/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.tool.impl;

import com.dbn.assistant.tool.AssistantToolBase;
import com.dbn.assistant.tool.spec.ConnectionInfoTool;
import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.connection.AuthenticationTokenType;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionSettings;

import static com.dbn.common.util.Enumerations.nameOf;

public class ConnectionInfoToolImpl extends AssistantToolBase implements ConnectionInfoTool {

    @Override
    public ConnectionInformation loadConnectionInformation() {
        ConnectionHandler connection = getConnection();

        ConnectionInformation ci = new ConnectionInformation();
        ConnectionSettings settings = connection.getSettings();
        ConnectionDatabaseSettings databaseSettings = settings.getDatabaseSettings();

        DatabaseInfo databaseInfo = databaseSettings.getDatabaseInfo();
        ci.setConfigType(nameOf(databaseInfo.getUrlType()));
        ci.setDatabaseHost(databaseInfo.getHost());
        ci.setDatabasePort(databaseInfo.getPort());
        ci.setDatabaseName(databaseInfo.getDatabase());

        ci.setTnsFolder(databaseInfo.getTnsFolder());
        ci.setTnsProfile(databaseInfo.getTnsProfile());

        ci.setServerType(nameOf(databaseInfo.getServerType()));
        ci.setProtocol(nameOf(databaseInfo.getProtocol()));

        ci.setConnectionParameters(databaseInfo.getParameters());
        ci.setConnectionProperties(settings.getPropertiesSettings().getProperties());

        ci.setAuthenticationInfo(createAuthenticationInfo(connection));
        return ci;

    }

    private AuthenticationInformation createAuthenticationInfo(ConnectionHandler connection) {
        AuthenticationInfo authenticationInfo = connection.getAuthenticationInfo();
        AuthenticationInformation ai = new AuthenticationInformation();

        ai.setAuthenticationType(nameOf(authenticationInfo.getType()));
        ai.setUserName(authenticationInfo.getUser());
        ai.setPassword(obfuscateSecret(authenticationInfo.getPassword()));

        AuthenticationTokenType tokenType = authenticationInfo.getTokenType();
        if (tokenType != null) {
            ai.setTokenType(nameOf(tokenType));

            TokenAuthenticationInfo tai = new TokenAuthenticationInfo();
            tai.setOciTokenConfigFile(authenticationInfo.getTokenConfigFile());
            tai.setOciTokenProfile(authenticationInfo.getTokenProfile());
            tai.setOciCompartmentOcid(authenticationInfo.getCompartmentOcid());
            tai.setOciDatabaseOcid(authenticationInfo.getDatabaseOcid());

            tai.setAzureClientId(authenticationInfo.getAzureClientId());
            tai.setAzureTenantId(authenticationInfo.getAzureTenantId());
            tai.setAzureClientCertificateFile(authenticationInfo.getAzureClientCertificateFile());
            tai.setAzureClientCertificatePassword(obfuscateSecret(authenticationInfo.getAzureClientCertificatePassword()));
            tai.setAzureClientSecret(obfuscateSecret(authenticationInfo.getAzureClientSecret()));
            tai.setAzureDatabaseAppIdUri(authenticationInfo.getAzureDatabaseApplicationIdUri());
            ai.setTokenAuthenticationInfo(tai);
        }
        return ai;

    }

    private static String obfuscateSecret(char[] secret) {
        if (secret == null) return null;
        if (secret.length == 0) return null;
        return "**********";
    }

}
