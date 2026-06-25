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

import com.dbn.assistant.credential.ui.AssistantCredentialsSettingsForm;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.common.options.WorkspaceStorage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.options.setting.Settings.newElement;

@Getter
@Setter
@WorkspaceStorage
@EqualsAndHashCode(callSuper = false)
public class AssistantCredentialSettings
        extends BasicProjectConfiguration<AssistantSettings, AssistantCredentialsSettingsForm> {

    private AssistantCredentialBundle credentials;

    public AssistantCredentialSettings(AssistantSettings parent) {
        super(parent);
        credentials = new AssistantCredentialBundle(parent.getProject());
    }

    public void setCredentials(AssistantCredentialBundle credentials) {
        this.credentials = new AssistantCredentialBundle(getProject(), credentials.getElements());
    }

    @NotNull
    @Override
    public AssistantCredentialsSettingsForm createConfigurationEditor() {
        return new AssistantCredentialsSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "credential-settings";
    }

    @Override
    public void readConfiguration(Element element) {
        List<AssistantCredential> credentials = new ArrayList<>();
        Element credentialsElement = element.getChild("credentials");
        if (credentialsElement != null) {
            for (Element credentialElement : credentialsElement.getChildren()) {
                AssistantCredential credential = new AssistantCredential();
                credential.readConfiguration(credentialElement);
                credentials.add(credential);
            }
        }

        this.credentials.setCredentials(credentials);
    }

    @Override
    public void writeConfiguration(Element element) {
        Element credentialsElement = newElement(element, "credentials");
        for (AssistantCredential credential : credentials.getElements()) {
            Element credentialElement = newElement(credentialsElement, "credential");
            credential.writeConfiguration(credentialElement);
        }
    }
}
