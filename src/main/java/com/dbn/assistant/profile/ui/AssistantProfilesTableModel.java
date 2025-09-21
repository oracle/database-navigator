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

package com.dbn.assistant.profile.ui;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.assistant.credential.AssistantCredentialBundle;
import com.dbn.assistant.profile.AssistantProfile;
import com.dbn.assistant.profile.AssistantProfileBundle;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.common.ui.table.DBNTypedEditableTableModel;
import com.dbn.common.util.Strings;
import com.intellij.openapi.options.ConfigurationException;
import lombok.Getter;

@Getter
public class AssistantProfilesTableModel extends DBNTypedEditableTableModel<AssistantProfile> {
    private final AssistantCredentialBundle credentials;

    AssistantProfilesTableModel(AssistantProfileBundle profiles, AssistantCredentialBundle credentials) {
        super(AssistantProfile.class, profiles.getElements());
        this.credentials = credentials;

        addColumn("Profile Name", String.class, c -> c.getName(), null);
        addColumn("LLM Provider", String.class, c -> getProviderName(c.getProviderId()), null);
        addColumn("Credential", String.class, c -> getCredentialName(c.getCredentialId()), null);
    }

    private String getProviderName(String providerId) {
        if (Strings.isEmpty(providerId)) return "";

        AIProvider provider = AIProviderData.getProvider(AssistantType.PUBLIC, providerId);
        return provider == null ? "" : provider.getName();
    }

    private String getCredentialName(String credentialId) {
        if (Strings.isEmpty(credentialId)) return "";
        AssistantCredential credential = credentials.get(credentialId);
        return credential == null ? "" : credential.getName();
    }

    public void validate() throws ConfigurationException {
        for (AssistantProfile profile : getElements()) {
            if (Strings.isEmpty(profile.getName())) {
                throw new ConfigurationException("Please provide names for all profiles.");
            }
        }
    }
}
