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

package com.dbn.connection.ssl;

import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.util.Strings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.ConnectionSslSettings;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.applicationService;

public class SslConnectionManager extends ApplicationComponentBase {
    private final Map<SslConnectionConfig, SslConnection> sslConnectors = new ConcurrentHashMap<>();

    public SslConnectionManager() {
        super("DBNavigator.SslConnectionManager");
    }

    public static SslConnectionManager getInstance() {
        return applicationService(SslConnectionManager.class);
    }

    public SslConnection ensureSslConnection(ConnectionSettings connectionSettings) {
        ConnectionSslSettings sslSettings = connectionSettings.getSslSettings();
        if (!sslSettings.isActive()) return null;

        SslConnectionConfig config = createConfig(sslSettings);
        SslConnection connector = sslConnectors.computeIfAbsent(config, c -> new SslConnection(c));

        if (!connector.isConnected()) connector.connect();
        return connector;
    }

    @NotNull
    private static SslConnectionConfig createConfig(ConnectionSslSettings sslSettings) {
        String certificateAuthorityFilePath = sslSettings.getCertificateAuthorityFile();
        String clientCertificateFilePath = sslSettings.getClientCertificateFile();
        String clientKeyFilePath = sslSettings.getClientKeyFile();

        File certificateAuthorityFile = Strings.isEmpty(certificateAuthorityFilePath) ? null : new File(certificateAuthorityFilePath);
        File clientCertificateFile = Strings.isEmpty(clientCertificateFilePath) ? null : new File(clientCertificateFilePath);
        File clientKeyFile = Strings.isEmpty(clientKeyFilePath) ? null : new File(clientKeyFilePath);

        SslConnectionConfig config = new SslConnectionConfig(
                certificateAuthorityFile,
                clientCertificateFile,
                clientKeyFile);
        return config;
    }
}
