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

import com.dbn.common.util.Messages;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.config.hosts.KnownHostEntry;
import org.apache.sshd.client.keyverifier.DefaultKnownHostsServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.security.PublicKey;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import static com.dbn.nls.NlsResources.txt;

@Slf4j
final class StrictKnownHostsServerKeyVerifier extends DefaultKnownHostsServerKeyVerifier {
    private static final String[] TRUST_OPTIONS = Messages.options(
            txt("msg.ssh.button.TrustAndSave"),
            txt("msg.shared.button.Cancel"));

    private static final Set<PosixFilePermission> OWNER_ONLY_DIRECTORY_PERMISSIONS = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE);

    private static final Set<PosixFilePermission> OWNER_ONLY_FILE_PERMISSIONS = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE);

    private HostKeyMismatch hostKeyMismatch;
    private Throwable knownHostsFileUpdateFailure;

    StrictKnownHostsServerKeyVerifier() {
        super((s, a, k) -> acceptUnknownHostKey(a, k));
    }

    static boolean acceptUnknownHostKey(SocketAddress remoteAddress, PublicKey serverKey) {
        Path knownHostsFile = KnownHostEntry.getDefaultKnownHostsFile();
        String message = txt(
                "msg.ssh.message.UnknownHostKey",
                formatAddress(remoteAddress),
                KeyUtils.getKeyType(serverKey),
                KeyUtils.getFingerPrint(serverKey),
                knownHostsFile);

        return Messages.showConfirmationDialog(
                null,
                txt("msg.ssh.title.TrustSshServer"),
                message,
                TRUST_OPTIONS,
                1) == 0;
    }

    private static String formatAddress(SocketAddress address) {
        if (address instanceof InetSocketAddress inetAddress) {
            return inetAddress.getHostString() + ":" + inetAddress.getPort();
        }
        return String.valueOf(address);
    }

    @Nullable
    HostKeyMismatch getHostKeyMismatch() {
        return hostKeyMismatch;
    }

    @Nullable
    Throwable getKnownHostsFileUpdateFailure() {
        return knownHostsFileUpdateFailure;
    }

    @Override
    public boolean verifyServerKey(ClientSession clientSession, SocketAddress remoteAddress, PublicKey serverKey) {
        hostKeyMismatch = null;
        knownHostsFileUpdateFailure = null;
        return super.verifyServerKey(clientSession, remoteAddress, serverKey);
    }

    @Override
    protected boolean acceptUnknownHostKey(ClientSession clientSession, SocketAddress remoteAddress, PublicKey serverKey) {
        boolean accepted = super.acceptUnknownHostKey(clientSession, remoteAddress, serverKey);
        if (accepted && knownHostsFileUpdateFailure != null) {
            log.warn("Rejecting SSH server key for host {} because it could not be saved", remoteAddress, knownHostsFileUpdateFailure);
            return false;
        }
        return accepted;
    }

    @Override
    public boolean acceptModifiedServerKey(
            ClientSession clientSession,
            SocketAddress remoteAddress,
            KnownHostEntry entry,
            PublicKey expected,
            PublicKey actual) {
        hostKeyMismatch = new HostKeyMismatch(actual);
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

    @Override
    protected KnownHostEntry updateKnownHostsFile(
            ClientSession clientSession,
            SocketAddress remoteAddress,
            PublicKey serverKey,
            Path file,
            Collection<HostEntryPair> knownHosts)
            throws Exception {
        ensureKnownHostsFile(file);
        KnownHostEntry entry = super.updateKnownHostsFile(clientSession, remoteAddress, serverKey, file, knownHosts);
        if (entry == null) {
            throw new IOException("Could not create known-hosts entry for " + remoteAddress);
        }
        return entry;
    }

    @Override
    protected void handleKnownHostsFileUpdateFailure(
            ClientSession clientSession,
            SocketAddress remoteAddress,
            PublicKey serverKey,
            Path file,
            Collection<HostEntryPair> knownHosts,
            Throwable reason) {
        knownHostsFileUpdateFailure = reason;
        super.handleKnownHostsFileUpdateFailure(clientSession, remoteAddress, serverKey, file, knownHosts, reason);
    }

    private static void ensureKnownHostsFile(Path knownHostsFile) throws IOException {
        Path sshDirectory = knownHostsFile.getParent();
        if (sshDirectory != null) {
            Files.createDirectories(sshDirectory);
            setPosixPermissionsIfSupported(sshDirectory, OWNER_ONLY_DIRECTORY_PERMISSIONS);
        }

        if (Files.notExists(knownHostsFile)) {
            Files.createFile(knownHostsFile);
        }
        if (!Files.isRegularFile(knownHostsFile)) {
            throw new IOException("Known-hosts path is not a regular file: " + knownHostsFile);
        }
        setPosixPermissionsIfSupported(knownHostsFile, OWNER_ONLY_FILE_PERMISSIONS);
    }

    private static void setPosixPermissionsIfSupported(Path path, Set<PosixFilePermission> permissions) throws IOException {
        PosixFileAttributeView view = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (view != null) {
            view.setPermissions(permissions);
        }
    }

    static final class HostKeyMismatch {
        private final PublicKey actual;

        private HostKeyMismatch(PublicKey actual) {
            this.actual = actual;
        }

        String getActualKeyType() {
            return KeyUtils.getKeyType(actual);
        }

        String getActualFingerprint() {
            return KeyUtils.getFingerPrint(actual);
        }
    }
}
