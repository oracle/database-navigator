/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.credential;

import com.dbn.assistant.provider.AIProviderId;
import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Cloneable;
import com.dbn.credentials.DatabaseCredentialManager;
import com.dbn.credentials.Secret;
import com.dbn.credentials.SecretType;
import com.dbn.credentials.SecretsOwner;
import com.dbn.credentials.SecretsOwnerRegistry;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static com.dbn.common.options.setting.Settings.charsAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.setCharsAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Base64.decode;
import static com.dbn.common.util.Base64.encode;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.credentials.SecretType.GENERIC_CREDENTIAL;

@Getter
@Setter
@EqualsAndHashCode
public class AssistantCredential implements Cloneable<AssistantCredential>, PersistentConfiguration, Presentable, SecretsOwner {
    private String id = UUID.randomUUID().toString();
    private String name;
    private String user;
    private AIProviderId providerId;
    private char[] key;


    public AssistantCredential() {
        SecretsOwnerRegistry.register(this);
    }

    @Override
    @NotNull
    public String getName() {
        return nvl(name, "");
    }

    @Override
    public AssistantCredential clone() {
        AssistantCredential clone = new AssistantCredential();
        clone.id = id;
        clone.name = name;
        clone.user = user;
        clone.providerId = providerId;
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
        providerId = enumAttribute(element, "provider", AIProviderId.class);

        if (isTransientContext()) {
            // only propagate credential key when config context is transient
            // (avoid storing it in config xml)
            key = decode(charsAttribute(element, "transient-key"));
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        setStringAttribute(element, "id", id);
        setStringAttribute(element, "name", name);
        setStringAttribute(element, "user", user);
        setEnumAttribute(element, "provider", providerId);

        if (isTransientContext()) {
            // only propagate credential key when config context is transient
            // (avoid storing it in config xml)
            setCharsAttribute(element, "transient-key", encode(key));
        }
    }

    /*********************************************************
     *                     SecretHolder                      *
     *********************************************************/

    @NotNull
    @Override
    public Object getSecretOwnerId() {
        return id;
    }

    @NotNull
    @Override
    public String getSecretOwnerName() {
        return name;
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
        this.key = secret.getToken();
    }
}
