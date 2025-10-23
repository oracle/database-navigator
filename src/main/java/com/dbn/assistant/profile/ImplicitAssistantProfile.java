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

package com.dbn.assistant.profile;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.assistant.credential.AssistantCredentialLookup;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.assistant.provider.AIProviderId;
import com.dbn.common.project.ProjectRef;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.assistant.profile.AssistantTemperaturePreset.BALANCED;

@Getter
public class ImplicitAssistantProfile implements AssistantProfile {
    @NonNls
    private final String id;
    private final String credentialId;

    private final ProjectRef project;

    public ImplicitAssistantProfile(Project project, AssistantCredential credential) {
        this.project = ProjectRef.of(project);
        this.credentialId = credential.getId();
        this.id = "implicit-profile-" + credential.getId();
    }

    @Override
    public AssistantType getAssistantType() {
        return AssistantType.PUBLIC;
    }

    @Override
    public AIProviderId getProviderId() {
        AssistantCredential credential = getCredential();
        return credential == null ? null : credential.getProviderId();
    }

    private Project getProject() {
        return ProjectRef.ensure(project);
    }

    @Nullable
    public AssistantCredential getCredential() {
        Project project = getProject();
        return AssistantCredentialLookup.getCredential(project, credentialId);
    }

    public AIProvider getProvider() {
        return AIProviderData.getProvider(getAssistantType(), getProviderId());
    }

    @Override
    public String getDefaultModelId() {
        AIProvider provider = getProvider();
        return provider == null ? null : provider.getDefaultModelId();
    }

    @Override
    public @NotNull String getName() {
        AssistantCredential credential = getCredential();
        if (credential == null) return "Undefined";

        return credential.getName();
    }

    @Override
    public double getTemperature() {
        return BALANCED.getValue();
    }
}
