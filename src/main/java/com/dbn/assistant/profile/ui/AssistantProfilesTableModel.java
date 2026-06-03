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
import com.dbn.assistant.profile.AssistantTemperaturePreset;
import com.dbn.assistant.profile.DeclaredAssistantProfile;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.assistant.provider.AIProviderId;
import com.dbn.common.ui.table.DBNEntityEditableTableModel;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Strings;
import com.intellij.openapi.options.ConfigurationException;

import java.util.List;
import java.util.function.Supplier;

public class AssistantProfilesTableModel extends DBNEntityEditableTableModel<DeclaredAssistantProfile> {

    private final Supplier<AssistantCredentialBundle> credentials;
    AssistantProfilesTableModel(AssistantProfileBundle profiles) {
        super(() -> profiles.getDeclaredProfiles());
        this.credentials = () -> profiles.getCredentials();

        addColumn(txt("app.assistant.column.ProfileName"), String.class, p -> p.getName(), null);
        addColumn(txt("app.assistant.column.LlmProvider"), String.class, p -> getProviderName(p), null);
        addColumn(txt("app.assistant.column.Credential"), String.class, p -> getCredentialName(p), null);
        addColumn(txt("app.assistant.column.Temperature"), String.class, p -> getTemperatureName(p), null);
    }

    private String getTemperatureName(DeclaredAssistantProfile profile) {
        double temperature = profile.getTemperature();
        AssistantTemperaturePreset preset = profile.getTemperaturePreset();
        return preset == AssistantTemperaturePreset.CUSTOM ? preset.getName() + " (" + temperature + ")" : preset.getName();
    }

    private String getProviderName(DeclaredAssistantProfile profile) {
        AIProviderId providerId = profile.getProviderId();
        if (providerId == null) return "";

        AIProvider provider = AIProviderData.getProvider(AssistantType.PUBLIC, providerId);
        return provider == null ? "" : provider.getName();
    }

    private String getCredentialName(DeclaredAssistantProfile profile) {
        String credentialId = profile.getCredentialId();
        if (Strings.isEmpty(credentialId)) return "";

        List<AssistantCredential> credentials = this.credentials.get().getElements();
        AssistantCredential credential = Lists.first(credentials, c -> c.getId().equals(credentialId));
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
