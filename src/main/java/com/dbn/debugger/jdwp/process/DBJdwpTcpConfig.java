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

package com.dbn.debugger.jdwp.process;

import com.dbn.common.network.NetworkAddress;
import com.dbn.connection.ssh.SshTunnelConfig;
import com.dbn.debugger.JDWPTunnelType;
import lombok.Getter;

@Getter
public class DBJdwpTcpConfig {
    private final NetworkAddress localAddress;
    private final JDWPTunnelType tunnelType;
    private final SshTunnelConfig sshTunnelConfig;

    public DBJdwpTcpConfig(NetworkAddress localAddress, JDWPTunnelType tunnelType) {
        this.localAddress = localAddress;
        this.tunnelType = tunnelType;
        this.sshTunnelConfig = null;
    }
    public DBJdwpTcpConfig(NetworkAddress localAddress, JDWPTunnelType tunnelType, SshTunnelConfig sshTunnelConfig) {
        this.localAddress = localAddress;
        this.tunnelType = tunnelType;
        this.sshTunnelConfig = sshTunnelConfig;
    }
}
