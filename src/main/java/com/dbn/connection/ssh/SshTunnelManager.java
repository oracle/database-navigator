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

package com.dbn.connection.ssh;

import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.ConnectionSshTunnelSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.util.Strings.parseInt;

public class SshTunnelManager extends ApplicationComponentBase {
    private final Map<SshTunnelConfig, SshTunnelConnector> sshTunnelConnectors = new ConcurrentHashMap<>();

    public SshTunnelManager() {
        super("DBNavigator.SshTunnelManager");
    }

    public static SshTunnelManager getInstance() {
        return applicationService(SshTunnelManager.class);
    }

    public SshTunnelConnector ensureSshConnection(ConnectionSettings connectionSettings) throws Exception {
        ConnectionSshTunnelSettings sshSettings = connectionSettings.getSshTunnelSettings();
        if (!sshSettings.isActive()) return null;

        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
        SshTunnelConfig config = createConfig(databaseSettings, sshSettings);
        SshTunnelConnector connector = sshTunnelConnectors.computeIfAbsent(config, c -> new SshTunnelConnector(c));

        if (!connector.isConnected()) connector.connect();
        return connector;
    }

    @NotNull
    private static SshTunnelConfig createConfig(ConnectionDatabaseSettings databaseSettings, ConnectionSshTunnelSettings sshSettings) {
        DatabaseInfo databaseInfo = databaseSettings.getDatabaseInfo();

        String proxyHost = sshSettings.getHost();
        int proxyPort = parseInt(sshSettings.getPort(), -1);

        String remoteHost = databaseInfo.getHost();
        int remotePort = parseInt(databaseInfo.getPort(), -1);

        return new SshTunnelConfig(
                proxyHost,
                proxyPort,
                sshSettings.getUser(),
                sshSettings.getAuthType(),
                sshSettings.getKeyFile(),
                sshSettings.getKeyPassphrase(),
                sshSettings.getPassword(),
                remoteHost,
                remotePort);
    }
}
