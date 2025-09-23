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
import com.dbn.assistant.credential.AssistantCredentialBundle;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.common.component.ProjectUnit;
import com.dbn.common.util.CollectionUtil;
import com.dbn.common.util.Lists;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Lists.first;

@Getter
@Setter
public class AssistantProfileBundle extends ProjectUnit {
    private final List<DeclaredAssistantProfile> declaredProfiles = new ArrayList<>();
    private static final List<PotentialAssistantProfile> potentialProfiles = createPotentialProfiles();

    public AssistantProfileBundle(Project project) {
        super(project);
    }

    public AssistantProfileBundle(Project project, List<DeclaredAssistantProfile> declaredProfiles) {
        this(project);
        setDeclaredProfiles(declaredProfiles);
    }

    public void setDeclaredProfiles(List<DeclaredAssistantProfile> profiles) {
        this.declaredProfiles.clear();
        CollectionUtil.cloneElements(profiles, this.declaredProfiles);
    }

    public void clear() {
        declaredProfiles.clear();
    }

    public void addDeclaredProfile(DeclaredAssistantProfile profile) {
        declaredProfiles.add(profile);
    }

    public DeclaredAssistantProfile getDeclaredProfile(String profileName) {
        return Lists.first(declaredProfiles, p -> p.getName().equals(profileName));
    }

    public List<ImplicitAssistantProfile> getImplicitProfiles() {
        AssistantSettings assistantSettings = AssistantSettings.getInstance(getProject());
        AssistantCredentialBundle credentials = assistantSettings.getCredentialSettings().getCredentials();
        return credentials.getImplicitProfiles();
    }

    public List<PotentialAssistantProfile> getPotentialProfiles() {
        return potentialProfiles;
    }

    public ImplicitAssistantProfile getImplicitProfile(String profileName) {
        return first(getImplicitProfiles(), p -> p.getName().equals(profileName));
    }

    private static @NonNull List<PotentialAssistantProfile> createPotentialProfiles() {
        List<AIProvider> providers = AIProviderData.getProviders(AssistantType.PUBLIC);
        return Lists.convert(providers, p -> new PotentialAssistantProfile(p));
    }
}
