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
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.ConnectionSshTunnelSettings;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.applicationService;
import static com.dbn.connection.ssh.SshConnections.createTunnelConfig;

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

        SshTunnelConfig config = createTunnelConfig(connectionSettings);
        SshTunnelConnector connector = sshTunnelConnectors.computeIfAbsent(config, c -> new SshTunnelConnector(c));

        if (!connector.isConnected()) {
            connector.connect();
        }
        return connector;
    }
}
