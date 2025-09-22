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
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.ref.WeakRefCache;
import com.dbn.common.util.CollectionUtil;
import com.dbn.common.util.Lists;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.common.util.Lists.first;

@Getter
@Setter
public class AssistantProfileBundle {
    private final ProjectRef project;
    private final List<DeclaredAssistantProfile> declaredProfiles = new ArrayList<>();
    private static final List<PotentialAssistantProfile> potentialProfiles = createPotentialProfiles();
    private static final WeakRefCache<AssistantProfileBundle, List<ImplicitAssistantProfile>> implicitProfiles = WeakRefCache.weakKey();

    public AssistantProfileBundle(Project project) {
        this.project = ProjectRef.of(project);
    }

    public AssistantProfileBundle(Project project, List<DeclaredAssistantProfile> declaredProfiles) {
        this(project);
        setDeclaredProfiles(declaredProfiles);
    }

    public Project getProject() {
        return project.ensure();
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
        return implicitProfiles.computeIfAbsent(this, b -> createImplicitProfiles());
    }

    public List<PotentialAssistantProfile> getPotentialProfiles() {
        return potentialProfiles;
    }

    public ImplicitAssistantProfile getImplicitProfile(String profileName) {
        return first(getImplicitProfiles(), p -> p.getName().equals(profileName));
    }

    private List<ImplicitAssistantProfile> createImplicitProfiles() {
        Project project = getProject();
        AssistantSettings assistantSettings = AssistantSettings.getInstance(project);
        List<AssistantCredential> credentials = assistantSettings.getCredentialSettings().getCredentials().getElements();
        return credentials
                .stream()
                .filter(c -> c.getProviderId() != null)
                .map(c -> new ImplicitAssistantProfile(project, c))
                .collect(Collectors.toList());
    }

    private static @NonNull List<PotentialAssistantProfile> createPotentialProfiles() {
        List<AIProvider> providers = AIProviderData.getProviders(AssistantType.PUBLIC);
        return Lists.convert(providers, p -> new PotentialAssistantProfile(p));
    }
}
