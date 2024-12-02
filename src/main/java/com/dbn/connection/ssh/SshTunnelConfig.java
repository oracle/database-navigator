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

import lombok.Value;

@Value
public class SshTunnelConfig {
    private final String proxyHost;
    private final int proxyPort;
    private final String proxyUser;
    private final char[] proxyPassword;
    private final SshAuthType authType;
    private final String keyFile;
    private final char[] keyPassphrase;

    private final String remoteHost;
    private final int remotePort;


    public SshTunnelConfig(String proxyHost, int proxyPort, String proxyUser, SshAuthType authType, String keyFile, char[] keyPassphrase, char[] proxyPassword, String remoteHost, int remotePort) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;

        this.authType = authType;
        this.keyFile = keyFile;
        this.keyPassphrase = keyPassphrase;

        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }
}
