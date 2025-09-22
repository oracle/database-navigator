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

import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.common.util.Lists;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dbn.common.util.Lists.first;

@UtilityClass
public class AssistantProfileLookup {
    public static List<DeclaredAssistantProfile> getDeclaredProfiles(Project project) {
        AssistantProfileBundle profiles = getProfiles(project);
        return profiles.getDeclaredProfiles();
    }

    public static List<ImplicitAssistantProfile> getImplicitProfiles(Project project) {
        AssistantProfileBundle profiles = getProfiles(project);
        return profiles.getImplicitProfiles();
    }

    public static List<PotentialAssistantProfile> getPotentialProfiles(Project project) {
        AssistantProfileBundle profiles = getProfiles(project);
        return profiles.getPotentialProfiles();
    }

    public static AssistantProfileBundle getProfiles(Project project) {
        AssistantSettings assistantSettings = AssistantSettings.getInstance(project);
        return assistantSettings.getProfileSettings().getProfiles();
    }

    public static DeclaredAssistantProfile getDeclaredProfile(Project project, String profileName) {
        List<DeclaredAssistantProfile> profiles = getDeclaredProfiles(project);
        return first(profiles, p -> p.getName().equals(profileName));
    }

    public static ImplicitAssistantProfile getImplicitProfile(Project project, String profileName) {
        List<ImplicitAssistantProfile> profiles = getImplicitProfiles(project);
        return first(profiles, p -> p.getName().equals(profileName));
    }

    public static List<ImplicitAssistantProfile> getUndefinedImplicitProfiles(Project project) {
        List<ImplicitAssistantProfile> implicitProfiles = getImplicitProfiles(project);
        List<DeclaredAssistantProfile> declaredProfiles = getDeclaredProfiles(project);
        Set<String> declaredProviderIds = declaredProfiles.stream().map(p -> p.getProviderId()).collect(Collectors.toSet());

        return Lists.filter(implicitProfiles, p -> !declaredProviderIds.contains(p.getProviderId()));
    }

    public static List<PotentialAssistantProfile> getUndefinedPotentialProfiles(Project project) {
        List<ImplicitAssistantProfile> implicitProfiles = getImplicitProfiles(project);
        List<DeclaredAssistantProfile> declaredProfiles = getDeclaredProfiles(project);
        List<PotentialAssistantProfile> potentialProfiles = getPotentialProfiles(project);

        Set<String> implicitProviderIds = implicitProfiles.stream().map(p -> p.getProviderId()).collect(Collectors.toSet());
        Set<String> declaredProviderIds = declaredProfiles.stream().map(p -> p.getProviderId()).collect(Collectors.toSet());

        return Lists.filter(potentialProfiles, p ->
                !declaredProviderIds.contains(p.getProviderId()) &&
                !implicitProviderIds.contains(p.getProviderId()));
    }
}
