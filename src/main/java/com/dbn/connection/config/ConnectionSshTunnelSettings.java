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

import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ui.ConnectionSshTunnelSettingsForm;
import com.dbn.connection.ssh.SshAuthType;
import com.dbn.credentials.Secret;
import com.dbn.credentials.SecretsOwner;
import com.dbn.credentials.SecretsOwnerRegistry;
import com.dbn.credentials.TransientSecretStore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.ConfigMonitor.isClipboardStorage;
import static com.dbn.common.options.setting.Settings.getBoolean;
import static com.dbn.common.options.setting.Settings.getEnum;
import static com.dbn.common.options.setting.Settings.getString;
import static com.dbn.common.options.setting.Settings.setBoolean;
import static com.dbn.common.options.setting.Settings.setEnum;
import static com.dbn.common.options.setting.Settings.setSensitiveString;
import static com.dbn.credentials.SecretType.SSH_TUNNEL_KEY_PASSPHRASE;
import static com.dbn.credentials.SecretType.SSH_TUNNEL_PASSWORD;
import static com.dbn.nls.NlsResources.txt;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ConnectionSshTunnelSettings extends BasicProjectConfiguration<ConnectionSettings, ConnectionSshTunnelSettingsForm> implements SecretsOwner {
    private boolean active = false;
    private String host;
    private String user;
    private String port = "22";
    private SshAuthType authType = SshAuthType.PASSWORD;
    private String keyFile;

    private final Secret password = new Secret(SSH_TUNNEL_PASSWORD, () -> getConnectionId(), () -> user);
    private final Secret keyPassphrase = new Secret(SSH_TUNNEL_KEY_PASSPHRASE, () -> getConnectionId(), () -> keyFile);

    ConnectionSshTunnelSettings(ConnectionSettings parent) {
        super(parent);
        SecretsOwnerRegistry.register(this);
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.connection.title.SshTunnelSettings");
    }

    /*********************************************************
     *                     Configuration                     *
     *********************************************************/
    @NotNull
    @Override
    public ConnectionSshTunnelSettingsForm createConfigurationEditor() {
        return new ConnectionSshTunnelSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "ssh-settings";
    }

    @Override
    public void readConfiguration(Element element) {
        active = getBoolean(element, "active", active);
        host = getString(element, "proxy-host", host);
        port = getString(element, "proxy-port", port);
        user = getString(element, "proxy-user", user);

        authType = getEnum(element, "auth-type", authType);
        keyFile = getString(element, "key-file", keyFile);

        if (isTransientContext()) {
            // transfer secrets outside transient config xml
            TransientSecretStore.consume(password, getConnectionId(), SSH_TUNNEL_PASSWORD, user);
            TransientSecretStore.consume(keyPassphrase, getConnectionId(), SSH_TUNNEL_KEY_PASSPHRASE, keyFile);
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        setBoolean(element, "active", !isClipboardStorage() && active);
        setSensitiveString(element, "proxy-host", host);
        setSensitiveString(element, "proxy-port", port);
        setSensitiveString(element, "proxy-user", user);
        setEnum(element, "auth-type", authType);
        setSensitiveString(element, "key-file", keyFile);

        if (isTransientContext()) {
            // transfer secrets outside transient config xml
            TransientSecretStore.store(password, getConnectionId(), SSH_TUNNEL_PASSWORD, user);
            TransientSecretStore.store(keyPassphrase, getConnectionId(), SSH_TUNNEL_KEY_PASSPHRASE, keyFile);
        }
    }

    public ConnectionId getConnectionId() {
        return ensureParent().getConnectionId();
    }

    /*********************************************************
     *                     SecretHolder                      *
     *********************************************************/

    @Override
    public @NotNull Object getSecretOwnerId() {
        return getConnectionId();
    }

    @Override
    public String getSecretOwnerName() {
        return ensureParent().getDatabaseSettings().getName();
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
