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

package com.dbn.assistant.credential.local;

import com.dbn.common.options.ConfigMonitor;
import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Cloneable;
import com.dbn.credentials.DatabaseCredentialManager;
import com.dbn.credentials.Secret;
import com.dbn.credentials.SecretType;
import com.dbn.credentials.SecretsOwner;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.dbn.common.options.ConfigActivity.INITIALIZING;
import static com.dbn.common.options.setting.Settings.charsAttribute;
import static com.dbn.common.options.setting.Settings.setCharsAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Base64.decode;
import static com.dbn.common.util.Base64.encode;
import static com.dbn.common.util.Chars.isNotEmpty;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.credentials.SecretType.GENERIC_CREDENTIAL;

@Getter
@Setter
@EqualsAndHashCode
public class LocalCredential implements Cloneable<LocalCredential>, PersistentConfiguration, Presentable, SecretsOwner {
    private String id = UUID.randomUUID().toString();
    private String name;
    private String user;
    private char[] key;

    @Override
    @NotNull
    public String getName() {
        return nvl(name, "");
    }

    @Override
    public LocalCredential clone() {
        LocalCredential clone = new LocalCredential();
        clone.id = id;
        clone.name = name;
        clone.user = user;
        clone.key = key;
        return clone;
    }

    @Override
    public String toString() {
        return name;
    }


    @Override
    public void readConfiguration(Element element) {
        id = nvl(stringAttribute(element, "id"), id);
        name = stringAttribute(element, "name");
        user = stringAttribute(element, "user");
        if (isTransientContext()) {
            // only propagate credential key when config context is transient
            // (avoid storing it in config xml)
            key = decode(charsAttribute(element, "transient-key"));
        }
        restorePassword(element);
    }

    @Override
    public void writeConfiguration(Element element) {
        setStringAttribute(element, "id", id);
        setStringAttribute(element, "name", name);
        setStringAttribute(element, "user", user);

        if (isTransientContext()) {
            // only propagate credential key when config context is transient
            // (avoid storing it in config xml)
            setCharsAttribute(element, "transient-key", encode(key));
        }
    }

    @Deprecated // TODO cleanup in subsequent release (temporarily support old storage)
    private void restorePassword(Element element) {
        if (!ConfigMonitor.is(INITIALIZING)) return; // only during config initialization
        if (isNotEmpty(key)) return;

        key = charsAttribute(element, "key");
        DatabaseCredentialManager credentialManager = DatabaseCredentialManager.getInstance();
        credentialManager.queueSecretsInsert(getName(), getKeySecret());
    }



    /*********************************************************
     *                     SecretHolder                      *
     *********************************************************/

    @NotNull
    @Override
    public Object getSecretOwnerId() {
        return id;
    }

    @Override
    public @NotNull Secret[] getSecrets() {
        return new Secret[]{getKeySecret()};
    }

    private Secret getKeySecret() {
        return new Secret(SecretType.GENERIC_CREDENTIAL, user, key);
    }

    @Override
    public void initSecrets() {
        DatabaseCredentialManager credentialManager = DatabaseCredentialManager.getInstance();
        Secret secret = credentialManager.loadSecret(GENERIC_CREDENTIAL, getSecretOwnerId(), user);
        key = secret.getToken();
    }
}
