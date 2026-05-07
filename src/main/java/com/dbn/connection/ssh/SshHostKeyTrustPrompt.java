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
import com.intellij.openapi.project.Project;
import org.apache.sshd.client.config.hosts.KnownHostEntry;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.security.PublicKey;

import static com.dbn.nls.NlsResources.txt;

final class SshHostKeyTrustPrompt {
    private static final String[] TRUST_OPTIONS = Messages.options(
            txt("msg.ssh.button.TrustAndSave"),
            txt("msg.shared.button.Cancel"));

    private final @Nullable Project project;

    SshHostKeyTrustPrompt(@Nullable Project project) {
        this.project = project;
    }

    boolean acceptUnknownHostKey(SocketAddress remoteAddress, PublicKey serverKey) {
        Path knownHostsFile = KnownHostEntry.getDefaultKnownHostsFile();
        String message = txt(
                "msg.ssh.message.UnknownHostKey",
                formatAddress(remoteAddress),
                KeyUtils.getKeyType(serverKey),
                KeyUtils.getFingerPrint(serverKey),
                knownHostsFile);

        return Messages.showConfirmationDialog(
                project,
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
}
