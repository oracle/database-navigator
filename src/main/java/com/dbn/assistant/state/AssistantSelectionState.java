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

package com.dbn.assistant.state;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderId;
import com.dbn.common.state.PersistentStateElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.assistant.provider.AIProviderData.getProvider;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

public class AssistantSelectionState implements PersistentStateElement {
    private final Map<AssistantType, Map<AIProviderId, AIModel>> modelSelections = new ConcurrentHashMap<>();

    public AIModel getSelectedModel(AssistantType assistantType, AIProviderId providerId) {
        Map<AIProviderId, AIModel> modelSelections = getModelSelections(assistantType);
        return modelSelections.computeIfAbsent(providerId, p -> getProvider(assistantType, p).getDefaultModel());
    }

    public AIModel setSelectedModel(AssistantType assistantType, AIProviderId providerId, AIModel model) {
        Map<AIProviderId, AIModel> modelSelections = getModelSelections(assistantType);
        return modelSelections.put(providerId, model);
    }

    private @NotNull Map<AIProviderId, AIModel> getModelSelections(AssistantType assistantType) {
        return this.modelSelections.computeIfAbsent(assistantType, t -> new ConcurrentHashMap<>());
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        Element modelSelectionElement = element.getChild("model-selection");
        for (Element assistantElement : childrenOf(modelSelectionElement, "assistant")) {
            AssistantType assistantType = enumAttribute(assistantElement, "type", AssistantType.class);
            Map<AIProviderId, AIModel> modelSelections = getModelSelections(assistantType);
            for (Element selectionElement : childrenOf(assistantElement, "selection")) {
                AIProviderId providerId = enumAttribute(selectionElement, "provider-id", AIProviderId.class);
                String modelId = stringAttribute(selectionElement, "model-id");

                AIProvider provider = getProvider(assistantType, providerId);
                AIModel model = provider.getModel(modelId);
                modelSelections.put(providerId, model);
            }
        }
    }

    @Override
    public void writeState(Element element) {
        Element modelSelectionElement = newElement(element, "model-selection");
        for (AssistantType assistantType : this.modelSelections.keySet()) {
            Element assistantElement = newElement(modelSelectionElement, "assistant");
            setEnumAttribute(assistantElement, "type", assistantType);

            Map<AIProviderId, AIModel> modelSelections = getModelSelections(assistantType);
            for (AIProviderId providerId : modelSelections.keySet()) {
                AIModel model = modelSelections.get(providerId);
                String modelId = model.getId();

                Element selectionElement = newElement(assistantElement, "selection");
                setEnumAttribute(selectionElement, "provider-id", providerId);
                setStringAttribute(selectionElement, "model-id", modelId);
            }
        }
    }
}
