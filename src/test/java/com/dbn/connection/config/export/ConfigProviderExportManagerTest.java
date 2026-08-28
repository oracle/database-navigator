/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.connection.config.export;

import com.dbn.connection.AuthenticationType;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ConnectionConfigType;
import com.dbn.connection.config.ConnectionSettings;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

public class ConfigProviderExportManagerTest {
    @Test
    public void acceptsMatchingDatabasePassword() {
        ConnectionSettings settings = settings("configured-secret");
        ConfigProviderExportRequest request = request("configured-secret");

        ConfigProviderExportManager.validateDatabasePassword(settings, request);
    }

    @Test
    public void rejectsDatabasePasswordThatDoesNotMatchConfiguredCredential() {
        ConnectionSettings settings = settings("configured-secret");
        ConfigProviderExportRequest request = request("wrong-secret");

        assertThrows(
                IllegalArgumentException.class,
                () -> ConfigProviderExportManager.validateDatabasePassword(settings, request));
    }

    @Test
    public void clearsExportRequestDatabasePassword() {
        char[] password = "secret".toCharArray();
        ConfigProviderExportRequest request = ConfigProviderExportRequest.builder()
                .includeDatabasePassword(true)
                .databasePassword(password)
                .build();

        request.clearDatabasePassword();

        assertArrayEquals(new char[password.length], password);
    }

    private static ConnectionSettings settings(String password) {
        ConnectionSettings settings = new ConnectionSettings(null, DatabaseType.ORACLE, ConnectionConfigType.BASIC);
        settings.getDatabaseSettings().getAuthenticationInfo().setType(AuthenticationType.USER_PASSWORD);
        settings.getDatabaseSettings().getAuthenticationInfo().setPassword(password.toCharArray());
        return settings;
    }

    private static ConfigProviderExportRequest request(String password) {
        return ConfigProviderExportRequest.builder()
                .includeDatabasePassword(true)
                .databasePassword(password.toCharArray())
                .build();
    }
}
