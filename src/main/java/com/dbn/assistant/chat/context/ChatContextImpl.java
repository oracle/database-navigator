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

import com.dbn.assistant.AssistantMode;
import com.dbn.assistant.AssistantType;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.assistant.provider.AIProviderId;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.util.Objects;

import static com.dbn.assistant.AssistantMode.DEVELOPMENT;
import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
public final class ChatContextImpl implements ChatContext {
    private final AssistantType assistantType;
    private AssistantMode assistantMode = DEVELOPMENT;
    private String profileId;
    private AIProviderId providerId;
    private String modelId;
    private String actionId;
    private boolean interactive = true;

    private DBObjectRef<DBTable> embeddingTable;

    public ChatContextImpl(AssistantType assistantType) {
        this.assistantType = assistantType;
    }

    public ChatContextImpl(AssistantType assistantType, AssistantMode assistantMode, String profileId, AIProviderId providerId, String modelId, String actionId, boolean interactive) {
        this(assistantType);
        this.assistantMode = assistantMode;
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
    public String getProviderName() {
        AIProvider provider = getProvider();
        return provider == null ? Objects.toString(providerId) : provider.getName();
    }

    @Override
    public String getModelName() {
        AIModel model = getModel();
        return model == null ? modelId : model.getName();
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
        assistantMode = enumAttribute(element, "mode", DEVELOPMENT);
        providerId = enumAttribute(element, "provider", AIProviderId.class);
        profileId = stringAttribute(element, "profile");
        modelId = stringAttribute(element, "model");
        actionId = stringAttribute(element, "action");
        interactive = booleanAttribute(element, "interactive", interactive);

        Element embeddingTableElement = element.getChild("embedding-table");
        if (embeddingTableElement != null) {
            embeddingTable = new DBObjectRef<>();
            embeddingTable.readState(embeddingTableElement);
        }
    }

    @Override
    public void writeState(Element element) {
        setEnumAttribute(element, "mode", assistantMode);
        setEnumAttribute(element, "provider", providerId);
        setStringAttribute(element, "profile", profileId);
        setStringAttribute(element, "model", modelId);
        setStringAttribute(element, "action", actionId);
        setBooleanAttribute(element, "interactive", interactive);

        if (embeddingTable != null) {
            Element embeddingTableElement = newElement(element, "embedding-table");
            embeddingTable.writeState(embeddingTableElement);
        }

    }

    public String toString() {
        return (profileId == null ? "" : (profileId + " / ")) + providerId + " / " + modelId + " / " + actionId;
    }
}
