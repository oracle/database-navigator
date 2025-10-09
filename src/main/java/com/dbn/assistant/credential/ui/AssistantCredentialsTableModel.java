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

package com.dbn.assistant.credential.ui;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.assistant.credential.AssistantCredentialBundle;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.assistant.provider.AIProviderId;
import com.dbn.common.ui.table.DBNEntityEditableTableModel;
import com.dbn.common.util.Chars;
import com.dbn.common.util.Strings;
import com.intellij.openapi.options.ConfigurationException;

public class AssistantCredentialsTableModel extends DBNEntityEditableTableModel<AssistantCredential> {

    AssistantCredentialsTableModel(AssistantCredentialBundle credentials) {
        super(AssistantCredential.class, credentials.getElements());

        addColumn("Credential Name", String.class, c -> c.getName(), (c, v) -> c.setName(v));
        addColumn("LLM Provider", String.class, c -> getProviderName(c.getProviderId()), null);
        addColumn("User", String.class, c -> c.getUser(), (c, v) -> c.setUser(v));
        addColumn("Secret", String.class, c -> Chars.toString(c.getSecret()), (c, v) -> c.setSecret(Chars.fromString(v)));
    }

    private String getProviderName(AIProviderId providerId) {
        if (providerId == null) return "";

        AIProvider provider = AIProviderData.getProvider(AssistantType.PUBLIC, providerId);
        return provider == null ? "" : provider.getName();
    }

    public void validate() throws ConfigurationException {
        for (AssistantCredential credential : getElements()) {
            if (Strings.isEmpty(credential.getName())) {
                throw new ConfigurationException("Please provide names for all credentials.");
            }
        }
    }
}
