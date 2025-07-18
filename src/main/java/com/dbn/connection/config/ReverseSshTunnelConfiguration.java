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
import com.dbn.credentials.DatabaseCredentialManager;
import com.dbn.credentials.Secret;
import com.dbn.credentials.SecretsOwner;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.getChars;
import static com.dbn.common.options.setting.Settings.getEnum;
import static com.dbn.common.options.setting.Settings.getString;
import static com.dbn.common.options.setting.Settings.setChars;
import static com.dbn.common.options.setting.Settings.setEnum;
import static com.dbn.common.options.setting.Settings.setString;
import static com.dbn.common.util.Base64.decode;
import static com.dbn.common.util.Base64.encode;
import static com.dbn.credentials.SecretType.DEBUGGER_SSH_TUNNEL_KEY_PASSPHRASE;
import static com.dbn.credentials.SecretType.DEBUGGER_SSH_TUNNEL_PASSWORD;

@Getter
@Setter
public class ReverseSshTunnelConfiguration  extends BasicConfiguration <ConnectionDebuggerSettings, ConfigurationEditorForm> implements SecretsOwner {
    private String sshHost;
    private String sshUser;
    private char[] sshPassword;
    private String sshPort = "22";
    private SshAuthType sshAuthType = SshAuthType.PASSWORD;
    private String sshKeyFile;
    private char[] sshKeyPassphrase;
    private String sshBindHost = "127.0.0.1";
    private String sshBindPort = "0";

    public ReverseSshTunnelConfiguration(ConnectionDebuggerSettings parent) {
        super(parent);
    }


    @Override
    public void readConfiguration(Element element) {
        sshHost = getString(element, "reverse-ssh-host", sshHost);
        sshUser = getString(element, "reverse-ssh-user", sshUser);
        sshPort = getString(element, "reverse-ssh-port", sshPort);
        sshKeyFile = getString(element, "reverse-ssh-key-file", sshKeyFile);
        sshAuthType = getEnum(element, "reverse-ssh-auth-type", sshAuthType);
        sshBindHost = getString(element, "reverse-ssh-bind-host", sshBindHost);
        sshBindPort = getString(element, "reverse-ssh-bind-port", sshBindPort);

        if (isTransientContext()) {
            // only propagate password when config context is transient
            // (avoid storing it in config xml)
            sshPassword = decode(getChars(element, "transient-reverse-ssh-password", encode(sshPassword)));
            sshKeyPassphrase = decode(getChars(element, "transient-reverse-ssh-key-passphrase", encode(sshKeyPassphrase)));
        }

    }

    @Override
    public void writeConfiguration(Element element) {
        setString(element, "reverse-ssh-host", sshHost);
        setString(element, "reverse-ssh-user", sshUser);
        setString(element, "reverse-ssh-port", sshPort);
        setEnum(element, "reverse-ssh-auth-type", sshAuthType);
        setString(element, "reverse-ssh-key-file", sshKeyFile);
        setString(element, "reverse-ssh-bind-host", sshBindHost);
        setString(element, "reverse-ssh-bind-port", sshBindPort);

        if (isTransientContext()) {
            // only propagate password when config context is transient
            // (avoid storing it in config xml)
            setChars(element, "transient-reverse-ssh-password", encode(sshPassword));
            setChars(element, "transient-reverse-ssh-key-passphrase", encode(sshKeyPassphrase));
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
                getPasswordSecret(),
                getKeyPassphraseSecret()};
    }

    private Secret getPasswordSecret() {
        return new Secret(DEBUGGER_SSH_TUNNEL_PASSWORD, sshUser, sshPassword);
    }

    private Secret getKeyPassphraseSecret() {
        return new Secret(DEBUGGER_SSH_TUNNEL_KEY_PASSPHRASE, sshKeyFile, sshKeyPassphrase);
    }

    /**
     * Load password or passphrase from Password Safe
     */
    @Override
    public void initSecrets() {
        //if (!active) return;

        ConnectionId connectionId = getConnectionId();
        DatabaseCredentialManager credentialManager = DatabaseCredentialManager.getInstance();
        if (sshAuthType == SshAuthType.PASSWORD) {
            Secret secret = credentialManager.loadSecret(DEBUGGER_SSH_TUNNEL_PASSWORD, connectionId, sshUser);
            sshPassword = secret.getToken();
        } else if (sshAuthType == SshAuthType.KEY_PAIR) {
            Secret secret = credentialManager.loadSecret(DEBUGGER_SSH_TUNNEL_KEY_PASSPHRASE, connectionId, sshKeyFile);
            sshKeyPassphrase = secret.getToken();
        }
    }
}