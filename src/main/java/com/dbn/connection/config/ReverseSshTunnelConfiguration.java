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
import com.dbn.connection.ssh.SshAuthType;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.*;
import static com.dbn.common.util.Base64.decode;
import static com.dbn.common.util.Base64.encode;

@Getter
@Setter
public class ReverseSshTunnelConfiguration  extends BasicConfiguration <ConnectionSettings, ConfigurationEditorForm>{
    private String sshHost;
    private String sshUser;
    private char[] sshPassword;
    private String sshPort = "22";
    private SshAuthType sshAuthType = SshAuthType.PASSWORD;
    private String sshKeyFile;
    private char[] sshKeyPassphrase;
    private String sshBindHost = "127.0.0.1";
    private String sshBindPort = "0";

    public ReverseSshTunnelConfiguration(ConnectionSettings parent) {
        super(parent);
    }


    @Override
    public void readConfiguration(Element element) {
        sshHost = getString(element, "reverse-ssh-host", sshHost);
        sshUser = getString(element, "reverse-ssh-user", sshUser);
        sshPort = getString(element, "reverse-ssh-port", sshPort);
        sshPassword = decode(getChars(element, "reverse-ssh-password", sshPassword));
        sshKeyFile = getString(element, "reverse-ssh-key-file", sshKeyFile);
        sshKeyPassphrase = decode(getChars(element, "reverse-ssh-key-passphrase",sshKeyPassphrase));
        sshAuthType = getEnum(element, "reverse-ssh-auth-type", sshAuthType);
        sshBindHost = getString(element, "reverse-ssh-bind-host", sshBindHost);
        sshBindPort = getString(element, "reverse-ssh-bind-port", sshBindPort);
    }

    @Override
    public void writeConfiguration(Element element) {
        setString(element, "reverse-ssh-host", sshHost);
        setString(element, "reverse-ssh-user", sshUser);
        setChars(element, "reverse-ssh-password", encode(sshPassword));
        setString(element, "reverse-ssh-port", sshPort);
        setEnum(element, "reverse-ssh-auth-type", sshAuthType);
        setString(element, "reverse-ssh-key-file", sshKeyFile);
        setChars(element, "reverse-ssh-key-passphrase", encode(sshKeyPassphrase));
        setString(element, "reverse-ssh-bind-host", sshBindHost);
        setString(element, "reverse-ssh-bind-port", sshBindPort);

    }
}