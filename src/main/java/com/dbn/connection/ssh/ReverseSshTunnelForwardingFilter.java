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

package com.dbn.connection.ssh;

import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.server.forward.ForwardingFilter;
import org.apache.sshd.server.forward.TcpForwardingFilter.Type;

import java.util.Objects;

/**
 * Allows only the reverse JDWP callback opened by DBN; rejects all other SSH forwarding capabilities.
 */
final class ReverseSshTunnelForwardingFilter implements ForwardingFilter {
    private final SshdSocketAddress localAddress;

    ReverseSshTunnelForwardingFilter(SshdSocketAddress localAddress) {
        this.localAddress = Objects.requireNonNull(localAddress, "localAddress");
    }

    @Override
    public boolean canForwardAgent(Session session, String requestType) {
        return false;
    }

    @Override
    public boolean canForwardX11(Session session, String requestType) {
        return false;
    }

    @Override
    public boolean canListen(SshdSocketAddress address, Session session) {
        return false;
    }

    @Override
    public boolean canConnect(Type type, SshdSocketAddress address, Session session) {
        return type == Type.Forwarded && isLocalTunnelTarget(address);
    }

    private boolean isLocalTunnelTarget(SshdSocketAddress address) {
        if (address == null || address.getPort() != localAddress.getPort()) return false;

        String expectedHost = localAddress.getHostName();
        String actualHost = address.getHostName();
        return SshdSocketAddress.isEquivalentHostName(expectedHost, actualHost, false) ||
                SshdSocketAddress.isLoopbackAlias(expectedHost, actualHost);
    }
}
