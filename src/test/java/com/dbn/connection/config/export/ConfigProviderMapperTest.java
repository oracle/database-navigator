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

import com.dbn.common.database.DatabaseInfo;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.DatabaseProtocol;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.config.ConnectionConfigType;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.EasyConnectParameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ConfigProviderMapperTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void mapBuildsServiceDescriptorAndExportsConnectionProperties() throws Exception {
        ConnectionSettings settings = settings(DatabaseUrlType.SERVICE);
        DatabaseInfo info = settings.getDatabaseSettings().getDatabaseInfo();
        info.setHost("db.example.com");
        info.setPort("1522");
        info.setDatabase("production");
        settings.getDatabaseSettings().getAuthenticationInfo().setType(AuthenticationType.USER_PASSWORD);
        settings.getDatabaseSettings().getAuthenticationInfo().setUser("scott");
        settings.getPropertiesSettings().setProperties(Map.of(
                "connectTimeout", "1000",
                "ssl", "true",
                "oracle.net.wallet_location", "/wallet"));
        settings.getPropertiesSettings().setEnableAutoCommit(true);

        ConfigProviderPayload payload = ConfigProviderMapper.map(settings, request());

        assertEquals("(description=(address_list=(address=(protocol=tcp)(host=db.example.com)(port=1522)))(connect_data=(service_name=production)))", payload.getConnectDescriptor());
        assertEquals("scott", payload.getUser());
        assertNotNull(payload.getPassword());
        assertEquals("FILL_THIS_VALUE", payload.getPassword().getValue());
        assertEquals(1000, payload.getJdbc().get("connectTimeout"));
        assertEquals("true", payload.getJdbc().get("ssl"));
        assertFalse(payload.getJdbc().containsKey("autoCommit"));
        assertFalse(payload.getJdbc().containsKey("oracle.net.wallet_location"));
    }

    @Test
    public void mapRejectsMissingRequiredDescriptorFields() {
        ConnectionSettings settings = settings(DatabaseUrlType.SID);
        settings.getDatabaseSettings().getDatabaseInfo().setHost(" ");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ConfigProviderMapper.map(settings, request()));

        assertTrue(exception.getMessage().contains("Host"));
    }

    @Test
    public void mapResolvesTnsProfileDescriptor() throws Exception {
        Path tnsFolder = temporaryFolder.newFolder("tns").toPath();
        Files.writeString(tnsFolder.resolve("tnsnames.ora"),
                "PROD = (DESCRIPTION = (ADDRESS = (PROTOCOL = TCP)(HOST = db.example.com)(PORT = 1521))" +
                        "(CONNECT_DATA = (SERVICE_NAME = production)))");
        ConnectionSettings settings = settings(DatabaseUrlType.TNS);
        DatabaseInfo info = settings.getDatabaseSettings().getDatabaseInfo();
        info.setTnsFolder(tnsFolder.toString());
        info.setTnsProfile("prod");

        ConfigProviderPayload payload = ConfigProviderMapper.map(settings, request());

        assertEquals("(DESCRIPTION = (ADDRESS = (PROTOCOL = TCP)(HOST = db.example.com)(PORT = 1521))(CONNECT_DATA = (SERVICE_NAME = production)))",
                payload.getConnectDescriptor());
    }

    @Test
    public void mapExportsAllEasyConnectParameters() throws Exception {
        ConnectionSettings settings = settings(DatabaseUrlType.EZCONNECT);
        DatabaseInfo info = settings.getDatabaseSettings().getDatabaseInfo();
        info.setProtocol(DatabaseProtocol.TCPS);
        Map<String, String> parameters = new LinkedHashMap<>();
        EasyConnectParameters.PARAMETER_NAMES.forEach(key -> parameters.put(key, value(key)));
        EasyConnectParameters.TCPS_ONLY_PARAMETER_NAMES.forEach(key -> parameters.put(key, value(key)));
        info.setParameters(parameters);

        ConfigProviderPayload payload = ConfigProviderMapper.map(settings, request());
        String descriptor = payload.getConnectDescriptor();

        EasyConnectParameters.PARAMETER_NAMES.stream()
                .filter(key -> !isSecurityParameter(key))
                .forEach(key -> assertTrue(descriptor.contains(parameter(key, value(key)))));
        assertTrue(descriptor.contains(parameter("WALLET_LOCATION", "\"" + value("WALLET_LOCATION") + "\"")));
        assertTrue(descriptor.contains(parameter("SSL_SERVER_DN_MATCH", value("SSL_SERVER_DN_MATCH"))));
        assertTrue(descriptor.contains(parameter("SSL_SERVER_CERT_DN", "\"" + value("SSL_SERVER_CERT_DN") + "\"")));
    }

    @Test
    public void mapDoesNotAddPasswordTemplateForNonPasswordAuthentication() throws Exception {
        ConnectionSettings settings = settings(DatabaseUrlType.DATABASE);
        settings.getDatabaseSettings().getAuthenticationInfo().setType(AuthenticationType.USER);

        ConfigProviderPayload payload = ConfigProviderMapper.map(settings, request());

        assertNull(payload.getPassword());
    }

    @Test
    public void hasConfiguredWalletRecognizesDatabaseAndJdbcParameters() {
        ConnectionSettings settings = settings(DatabaseUrlType.DATABASE);
        settings.getDatabaseSettings().getDatabaseInfo().setParameters(Map.of("WALLET_LOCATION", "/wallet"));
        assertTrue(ConfigProviderMapper.hasConfiguredWallet(settings));

        settings.getDatabaseSettings().getDatabaseInfo().setParameters(Map.of());
        settings.getPropertiesSettings().setProperties(Map.of("oracle.net.wallet_location", "/wallet"));
        assertTrue(ConfigProviderMapper.hasConfiguredWallet(settings));
    }

    private static ConnectionSettings settings(DatabaseUrlType urlType) {
        ConnectionSettings settings = new ConnectionSettings(null, DatabaseType.ORACLE, ConnectionConfigType.BASIC);
        DatabaseInfo info = settings.getDatabaseSettings().getDatabaseInfo();
        info.setUrlType(urlType);
        info.setHost("localhost");
        info.setPort("1521");
        info.setDatabase("xe");
        return settings;
    }

    private static ConfigProviderExportRequest request() {
        return ConfigProviderExportRequest.builder().build();
    }

    private static boolean isSecurityParameter(String key) {
        return "WALLET_LOCATION".equals(key) ||
                "SSL_SERVER_DN_MATCH".equals(key) ||
                "SSL_SERVER_CERT_DN".equals(key);
    }

    private static String parameter(String key, String value) {
        return "(" + key.toLowerCase(Locale.ROOT) + "=" + value + ")";
    }

    private static String value(String key) {
        return key.toLowerCase(Locale.ROOT);
    }
}
