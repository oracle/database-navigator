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

package com.dbn.assistant.chat.context;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.assistant.provider.AIProviderId;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.util.Objects;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
public final class ChatContextImpl implements ChatContext{
    private final AssistantType assistantType;
    private String profileId;
    private AIProviderId providerId;
    private String modelId;
    private String actionId;
    private boolean interactive = true;

    public ChatContextImpl(AssistantType assistantType) {
        this.assistantType = assistantType;
    }

    public ChatContextImpl(AssistantType assistantType, String profileId, AIProviderId providerId, String modelId, String actionId, boolean interactive) {
        this(assistantType);
        this.profileId = profileId;
        this.providerId = providerId;
        this.modelId = modelId;
        this.actionId = actionId;
        this.interactive = interactive;
    }

    @Override
    public AIProvider getProvider() {
        return AIProviderData.getProvider(assistantType, this.providerId);
    }

    public AIModel getModel() {
        AIProvider provider = getProvider();
        if (provider == null) return null;

        return provider.getModel(this.modelId);
    }

    @Override
    public Object getAction() {
        // not relevant for generic chat contexts
        return null;
    }

    @Override
    public boolean isInteractive() {
        // all contexts are fundamentally interactive
        // (except for Oracle "Select AI" with non-conversational profiles)
        return true;
    }

    @Override
    public boolean isModelSwitch(ChatContext that) {
        return !Objects.equals(this.modelId, that.getModelId());
    }

    @Override
    public boolean isProviderSwitch(ChatContext that) {
        return !Objects.equals(this.providerId, that.getProviderId());
    }

    @Override
    public boolean isProfileSwitch(ChatContext that) {
        return !Objects.equals(this.profileId, that.getProfileId());
    }

    @Override
    public boolean isActionSwitch(ChatContext that) {
        return false;
    }

    @Override
    public void readState(Element element) {
        providerId = enumAttribute(element, "provider", AIProviderId.class);
        profileId = stringAttribute(element, "profile");
        modelId = stringAttribute(element, "model");
        actionId = stringAttribute(element, "action");
        interactive = booleanAttribute(element, "interactive", interactive);
    }

    @Override
    public void writeState(Element element) {
        setEnumAttribute(element, "provider", providerId);
        setStringAttribute(element, "profile", profileId);
        setStringAttribute(element, "model", modelId);
        setStringAttribute(element, "action", actionId);
        setBooleanAttribute(element, "interactive", interactive);
    }

    public String toString() {
        return (profileId == null ? "" : (profileId + " / ")) + providerId + " / " + modelId + " / " + actionId;
    }
}
