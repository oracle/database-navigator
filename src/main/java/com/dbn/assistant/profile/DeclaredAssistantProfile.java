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
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.UUIDs;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
public class DeclaredAssistantProfile implements AssistantProfile, PersistentConfiguration, Cloneable<DeclaredAssistantProfile> {
    private AssistantType assistantType = AssistantType.PUBLIC;
    private String id = UUIDs.compact();
    private String name;
    private String providerId;
    private String credentialId;

    @Nullable
    public AIProvider getProvider(){
        return AIProviderData.getProvider(assistantType, providerId);
    }

    public String getDefaultModelId() {
        AIProvider provider = getProvider();
        if (provider == null) return null;
        return provider.getDefaultModelId();
    }

    public void readConfiguration(Element element) {
        assistantType = enumAttribute(element, "assistant-type", assistantType);
        id = stringAttribute(element, "id", id);
        name = stringAttribute(element, "name");
        providerId = stringAttribute(element, "provider-id");
        credentialId = stringAttribute(element, "credential-id");
    }

    @Override
    public void writeConfiguration(Element element) {
        setEnumAttribute(element, "assistant-type", assistantType);
        setStringAttribute(element, "id", id);
        setStringAttribute(element, "name", name);
        setStringAttribute(element, "provider-id", providerId);
        setStringAttribute(element, "credential-id", credentialId);
    }

    @Override
    public DeclaredAssistantProfile clone() {
        DeclaredAssistantProfile clone = new DeclaredAssistantProfile();
        clone.name = name;
        clone.providerId = providerId;
        clone.credentialId = credentialId;
        return clone;
    }
}
