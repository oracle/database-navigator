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

import com.dbn.assistant.credential.AssistantCredentialBundle;
import com.dbn.assistant.profile.AssistantProfileBundle;
import com.dbn.assistant.profile.DeclaredAssistantProfile;
import com.dbn.assistant.provider.AIProviderId;
import com.dbn.common.routine.Consumer;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Builder
public class AssistantProfileEditRequest {
    private AssistantCredentialBundle credentials;
    private AssistantProfileBundle profiles;
    private DeclaredAssistantProfile profile;
    private AIProviderId providerId;
    private Consumer<DeclaredAssistantProfile> saveConsumer;

    public boolean isNewProfile() {
        return profile == null;
    }

    public void acceptProfile(DeclaredAssistantProfile profile) {
        if (saveConsumer == null) return;
        saveConsumer.accept(profile);
    }


    public Set<String> getUsedNames() {
        return profiles
                .getDeclaredProfiles()
                .stream()
                .filter(c -> profile == null || !c.getId().equals(profile.getId()))
                .map(c -> c.getName())
                .collect(Collectors.toSet());
    }
}
