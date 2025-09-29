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

import com.dbn.assistant.profile.ImplicitAssistantProfile;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.common.component.ProjectUnit;
import com.dbn.common.util.Strings;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.common.util.CollectionUtil.cloneElements;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Lists.first;

@Getter
@Setter
public class AssistantCredentialBundle extends ProjectUnit {

    private final List<AssistantCredential> elements = new ArrayList<>();
    private List<ImplicitAssistantProfile> implicitProfiles;

    public AssistantCredentialBundle(Project project) {
        super(project);
    }

    public AssistantCredentialBundle(Project project, List<AssistantCredential> elements) {
        this(project);
        setCredentials(elements);
    }

    public void setCredentials(List<AssistantCredential> credentials) {
        this.elements.clear();
        this.implicitProfiles = null;
        cloneElements(credentials, this.elements);
    }

    public void addCredential(AssistantCredential credential) {
        this.elements.add(credential);
        this.implicitProfiles = null;
    }

    public synchronized List<ImplicitAssistantProfile> getImplicitProfiles() {
        this.implicitProfiles = nvl(this.implicitProfiles, () -> createImplicitProfiles());
        return implicitProfiles;
    }

    private List<ImplicitAssistantProfile> createImplicitProfiles() {
        Project project = getProject();
        AssistantSettings assistantSettings = AssistantSettings.getInstance(project);
        List<AssistantCredential> credentials = assistantSettings.getCredentialSettings().getCredentials().getElements();
        return credentials
                .stream()
                .filter(c -> Strings.isNotEmpty(c.getProviderId()))
                .map(c -> new ImplicitAssistantProfile(project, c))
                .collect(Collectors.toList());
    }


    public int size() {
        return elements.size();
    }

    public AssistantCredential getCredential(String id) {
        return first(elements, c -> c.getId().equals(id));
    }

    public AssistantCredential getCredential(int index) {
        return elements.get(index);
    }

    public void initSecrets() {
        for (AssistantCredential element : elements) {
            element.initSecrets();
        }
    }
}
