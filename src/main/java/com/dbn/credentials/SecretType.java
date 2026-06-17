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

package com.dbn.credentials;

import com.dbn.common.ui.Presentable;
import lombok.Getter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import static com.dbn.nls.NlsResources.txt;

/**
 * Secret type classification used to uniquely identify secret tokens stored in {@link com.intellij.ide.passwordSafe.PasswordSafe}
 */
@Getter
public enum SecretType implements Presentable {
    CONNECTION_PASSWORD("Connection password"),                                       // connection passwords
    CONNECTION_AZURE_TOKEN_CLIENT_SECRET("Azure token client secret"),                // token client secret for azure
    CONNECTION_AZURE_TOKEN_CERTIFICATE_PASSWORD("Azure token certificate password"),  // token certificate password for azure
    CONNECTION_HASHICORP_VAULT_TOKEN("HashiCorp Vault token"),                        // config provider vault token for hashicorp
    CONNECTION_HASHICORP_VAULT_PASSWORD("HashiCorp Vault password"),                  // config provider userpass password for hashicorp
    CONNECTION_HASHICORP_APPROLE_SECRET_ID("HashiCorp AppRole secret ID"),            // config provider approle secret id for hashicorp
    CONNECTION_HASHICORP_GITHUB_TOKEN("HashiCorp GitHub token"),                      // config provider github token for hashicorp
    SSH_TUNNEL_PASSWORD("SSH tunnel password"),                                       // password for SSH tunnels
    SSH_TUNNEL_KEY_PASSPHRASE("SSH tunnel key passphrase"),                           // key passphrases for SSH tunnels
    DEBUGGER_SSH_TUNNEL_PASSWORD("Debugger SSH tunnel password"),                     // password for debugger SSH reverse tunnels
    DEBUGGER_SSH_TUNNEL_KEY_PASSPHRASE("Debugger SSH tunnel key passphrase"),         // key passphrases for debugger SSH reverse tunnels
    GENERIC_CREDENTIAL("Generic credential"),                                         // e.g. database assistant credential tokens
    STATE_ENCRYPTION_KEY("State encryption key"),                                     // key used to encrypt persistent state values
    ;

    SecretType(@NonNls String serviceName) {
        this.serviceName = serviceName;
    }

    private transient @Nls String name;
    private final @NonNls String serviceName;

    @Override
    public @Nls String getName() {
        String name = this.name;
        if (name == null) {
            name = txt("app.credentials.const.SecretType_" + name());
            this.name = name;
        }
        return name;
    }
}
