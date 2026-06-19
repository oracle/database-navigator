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

import com.dbn.assistant.profile.ui.AssistantProfilesSettingsForm;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.common.options.WorkspaceStorage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.newElement;

@Getter
@Setter
@WorkspaceStorage
@EqualsAndHashCode(callSuper = false)
public class AssistantProfileSettings
        extends BasicProjectConfiguration<AssistantSettings, AssistantProfilesSettingsForm> {

    private AssistantProfileBundle profiles;

    public AssistantProfileSettings(AssistantSettings settings) {
        super(settings);
        this.profiles = new AssistantProfileBundle(this);
    }

    @NotNull
    @Override
    public AssistantProfilesSettingsForm createConfigurationEditor() {
        return new AssistantProfilesSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "profile-settings";
    }

    @Override
    public void readConfiguration(Element element) {
        profiles.clear();
        Element profilesElement = element.getChild("profiles");
        if (profilesElement == null) return;

        for (Element profileElement : profilesElement.getChildren()) {
            DeclaredAssistantProfile profile = new DeclaredAssistantProfile();
            profile.readConfiguration(profileElement);
            profiles.addDeclaredProfile(profile);
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        Element profilesElement = newElement(element, "profiles");
        for (DeclaredAssistantProfile profile : profiles.getDeclaredProfiles()) {
            Element profileElement = newElement(profilesElement, "profile");
            profile.writeConfiguration(profileElement);
        }
    }
}
