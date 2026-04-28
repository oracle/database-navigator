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

import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.config.hosts.KnownHostEntry;
import org.apache.sshd.client.keyverifier.DefaultKnownHostsServerKeyVerifier;
import org.apache.sshd.client.keyverifier.RejectAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.KeyUtils;

import java.net.SocketAddress;
import java.security.PublicKey;

@Slf4j
final class StrictKnownHostsServerKeyVerifier extends DefaultKnownHostsServerKeyVerifier {
    StrictKnownHostsServerKeyVerifier() {
        super(RejectAllServerKeyVerifier.INSTANCE);
    }

    @Override
    protected boolean acceptUnknownHostKey(ClientSession clientSession, SocketAddress remoteAddress, PublicKey serverKey) {
        log.warn("Rejecting SSH server key for unknown host {} ({})", remoteAddress, KeyUtils.getFingerPrint(serverKey));
        return false;
    }

    @Override
    public boolean acceptModifiedServerKey(
            ClientSession clientSession,
            SocketAddress remoteAddress,
            KnownHostEntry entry,
            PublicKey expected,
            PublicKey actual) {
        log.warn(
                "Rejecting changed SSH server key for host {} (expected {}, received {})",
                remoteAddress,
                KeyUtils.getFingerPrint(expected),
                KeyUtils.getFingerPrint(actual));
        return false;
    }

    @Override
    protected boolean acceptIncompleteHostKeys(
            ClientSession clientSession,
            SocketAddress remoteAddress,
            PublicKey serverKey,
            Throwable reason) {
        log.warn("Rejecting SSH server key for host {} because known-hosts verification failed", remoteAddress, reason);
        return false;
    }
}
