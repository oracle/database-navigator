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

import com.dbn.common.network.NetworkAddress;
import com.dbn.common.util.Chars;
import com.dbn.common.util.Commons;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.hosts.KnownHostEntry;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.forward.ExplicitPortForwardingTracker;
import org.apache.sshd.client.session.forward.PortForwardingTracker;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.SshException;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.core.CoreModuleProperties;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static com.dbn.common.exception.Exceptions.getMessage;
import static com.dbn.common.exception.Exceptions.rootCauseOf;
import static com.dbn.connection.ssh.SshAuthType.KEY_PAIR;
import static com.dbn.connection.ssh.SshConnections.toSshdSocketAddress;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@Getter
@Setter
public class SshTunnelConnector {
    private final SshTunnelConfig config;

    @NonNls
    private NetworkAddress localAddress = new NetworkAddress("localhost", 0);
    private ClientSession session;
    private SshClient client;
    private PortForwardingTracker tracker;
    private boolean reverseTunnel = false;

    public SshTunnelConnector(SshTunnelConfig config) {
        this.config = config;
    }

    public SshTunnelConnector(SshTunnelConfig config, NetworkAddress localAddress) {
        this.config = config;
        this.localAddress = localAddress;
    }

    public ClientSession connect() throws Exception {
        try {
            initPort();
            initClient();
            initSession();
            initAuth();
            initTracker();
            return session;
        } catch (Exception e) {
            disconnect();
            throw createTunnelException(e);
        }
    }

    private SshTunnelException createTunnelException(Exception e) {
        if (!isServerKeyValidationFailure(e)) {
            return new SshTunnelException("Failed to create SSH tunnel: " + getMessage(rootCauseOf(e)), e);
        }

        NetworkAddress proxyAddress = config.getProxyAddress();
        Path knownHostsFile = KnownHostEntry.getDefaultKnownHostsFile();
        String message = "SSH tunnel blocked: host key verification failed for " +
                proxyAddress.getHost() + ":" + proxyAddress.getPort() +
                ". DB Navigator could not verify this SSH server against the known-hosts file. " +
                "Verify the SSH bastion identity before updating " + knownHostsFile + ".";

        return new SshTunnelException(message, e);
    }

    private static boolean isServerKeyValidationFailure(Throwable e) {
        while (e != null) {
            if (e instanceof SshException sshException &&
                    sshException.getDisconnectCode() == SshConstants.SSH2_DISCONNECT_HOST_KEY_NOT_VERIFIABLE) return true;

            e = e.getCause();
        }
        return false;
    }

    private void initPort() throws IOException {
        if (localAddress.getPort() == 0) {
            try (ServerSocket serverSocket = new ServerSocket(0)) {
                localAddress.setPort(serverSocket.getLocalPort());
            }
        }
        log.info("SSH Tunnel Connection - Local port initialised as {}", localAddress.getPort());
    }

    private void initClient() {
        client = SshClient.setUpDefaultClient();
        if (reverseTunnel) {
            client.setForwardingFilter(
                    new ReverseSshTunnelForwardingFilter(toSshdSocketAddress(localAddress)));
        }
        client.setServerKeyVerifier(new StrictKnownHostsServerKeyVerifier());

        CoreModuleProperties.SOCKET_KEEPALIVE.set(client, true);

        client.start();
        log.info("SSH Tunnel Connection - client initialized");
    }

    private void initSession() throws Exception {
        NetworkAddress proxyAddress = config.getProxyAddress();
        ConnectFuture future = client.connect(
                config.getProxyUser(),
                proxyAddress.getHost(),
                proxyAddress.getPort());

        session = future.verify(30, TimeUnit.SECONDS).getSession();
        log.info("SSH Tunnel Connection - session initialized");
    }

    private void initAuth() throws Exception {
        if (config.getAuthType() == KEY_PAIR) {
            initKeyPairAuth();
        } else {
            String proxyPassword = Chars.toString(config.getProxyPassword());
            session.addPasswordIdentity(proxyPassword);
        }

        session.auth().verify(10, TimeUnit.SECONDS);
        log.info("SSH Tunnel Connection - authentication succeeded");
    }

    private void initKeyPairAuth() throws Exception{
        String keyFile = config.getKeyFile();
        String keyPassphrase = Chars.toString(Commons.nvl(config.getKeyPassphrase(), Chars.EMPTY_ARRAY));

        File privateKeyFile = new File(keyFile);
        try (InputStream keyFileStream = new FileInputStream(privateKeyFile)) {
            NamedResource namedResource = NamedResource.ofName(privateKeyFile.getName());
            FilePasswordProvider passwordProvider = (sessionContext, resourceKey, retryIndex) -> keyPassphrase;

            var keyPairs = SecurityUtils.loadKeyPairIdentities(session, namedResource, keyFileStream, passwordProvider);
            keyPairs.forEach(kp -> session.addPublicKeyIdentity(kp));
        }
    }

    private void initTracker() throws IOException {
        SshdSocketAddress localAddress = toSshdSocketAddress(this.localAddress);
        SshdSocketAddress remoteAddress = toSshdSocketAddress(config.getRemoteAddress());
        SshdSocketAddress boundAddress = startPortForwarding(remoteAddress, localAddress);
        tracker = new ExplicitPortForwardingTracker(session, true, localAddress, remoteAddress, boundAddress);
        log.info("SSH Tunnel Connection - tracker initialized");
    }

    private SshdSocketAddress startPortForwarding(SshdSocketAddress remoteAddress, SshdSocketAddress localAddress) throws IOException {
        return reverseTunnel ?
                session.startRemotePortForwarding(remoteAddress, localAddress) :
                session.startLocalPortForwarding(localAddress.getPort(), remoteAddress);
    }

    public boolean isConnected() {
        return session != null && session.isAuthenticated() && session.isOpen() && !session.isClosing();
    }

    public void disconnect() {
        try {
            if (tracker != null) {
                tracker.close();
                log.info("SSH Tunnel Connection - port forwarding stopped");
            }
            if (session != null && session.isOpen()) {
                session.close();
                log.info("SSH Tunnel Connection - session closed");
            }
            if (client != null && client.isOpen()) {
                client.stop();
                log.info("SSH Tunnel Connection - client stopped");
            }
        } catch (Exception e) {
            log.warn("Failed to close SSH tunnel connection", e);
            conditionallyLog(e);
        }
    }
}
