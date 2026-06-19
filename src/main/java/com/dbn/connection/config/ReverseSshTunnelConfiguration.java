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

package com.dbn.connection.config;

import com.dbn.common.options.BasicConfiguration;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ssh.SshAuthType;
import com.dbn.credentials.Secret;
import com.dbn.credentials.SecretsOwner;
import com.dbn.credentials.TransientSecretStore;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.getEnum;
import static com.dbn.common.options.setting.Settings.getString;
import static com.dbn.common.options.setting.Settings.setEnum;
import static com.dbn.common.options.setting.Settings.setSensitiveString;
import static com.dbn.credentials.SecretType.DEBUGGER_SSH_TUNNEL_KEY_PASSPHRASE;
import static com.dbn.credentials.SecretType.DEBUGGER_SSH_TUNNEL_PASSWORD;

@Getter
@Setter
public class ReverseSshTunnelConfiguration  extends BasicConfiguration <ConnectionDebuggerSettings, ConfigurationEditorForm> implements SecretsOwner {
    private String host;
    private String port = "22";

    private SshAuthType authType = SshAuthType.PASSWORD;
    private String user;
    private String keyFile;
    private final Secret keyPassphrase = new Secret(DEBUGGER_SSH_TUNNEL_KEY_PASSPHRASE, this::getConnectionId, () -> keyFile);
    private final Secret password = new Secret(DEBUGGER_SSH_TUNNEL_PASSWORD, this::getConnectionId, () -> user);
    private String bindHost = "127.0.0.1";
    private String bindPort = "0";

    public ReverseSshTunnelConfiguration(ConnectionDebuggerSettings parent) {
        super(parent);
    }

    @Override
    public void readConfiguration(Element element) {
        if (element == null) return;

        host = getString(element, "host", host);
        port = getString(element, "port", port);
        bindHost = getString(element, "bind-host", bindHost);
        bindPort = getString(element, "bind-port", bindPort);

        user = getString(element, "user", user);
        authType = getEnum(element, "auth-type", authType);
        keyFile = getString(element, "key-file", keyFile);

        if (isTransientContext()) {
            // transfer secrets outside transient config xml
            TransientSecretStore.consume(password, getConnectionId(), DEBUGGER_SSH_TUNNEL_PASSWORD, user);
            TransientSecretStore.consume(keyPassphrase, getConnectionId(), DEBUGGER_SSH_TUNNEL_KEY_PASSPHRASE, keyFile);
        }

    }

    @Override
    public void writeConfiguration(Element element) {
        setSensitiveString(element, "host", host);
        setSensitiveString(element, "port", port);
        setSensitiveString(element, "bind-host", bindHost);
        setSensitiveString(element, "bind-port", bindPort);

        setEnum(element, "auth-type", authType);
        setSensitiveString(element, "user", user);
        setSensitiveString(element, "key-file", keyFile);

        if (isTransientContext()) {
            // transfer secrets outside transient config xml
            TransientSecretStore.store(password, getConnectionId(), DEBUGGER_SSH_TUNNEL_PASSWORD, user);
            TransientSecretStore.store(keyPassphrase, getConnectionId(), DEBUGGER_SSH_TUNNEL_KEY_PASSPHRASE, keyFile);
        }
    }

    /*********************************************************
     *                     SecretHolder                      *
     *********************************************************/

    private ConnectionSettings getConnectionSettings() {
        return ensureParent().ensureParent();
    }

    private ConnectionId getConnectionId() {
        return getConnectionSettings().getConnectionId();
    }

    @Override
    public @NotNull Object getSecretOwnerId() {
        return getConnectionId();
    }

    @NotNull
    @Override
    public String getSecretOwnerName() {
        ConnectionSettings connectionSettings = getConnectionSettings();
        return connectionSettings.getDatabaseSettings().getName();
    }

    @Override
    public Secret[] getSecrets() {
        return new Secret[] {
                password.snapshot(),
                keyPassphrase.snapshot()};
    }

    public char[] getPassword() {
        return password.getToken();
    }

    public void setPassword(char[] password) {
        this.password.setToken(password);
    }

    public char[] getKeyPassphrase() {
        return keyPassphrase.getToken();
    }

    public void setKeyPassphrase(char[] keyPassphrase) {
        this.keyPassphrase.setToken(keyPassphrase);
    }

}
