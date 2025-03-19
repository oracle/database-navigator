/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.assistant.chat;

import com.dbn.assistant.provider.AIModel;
import com.dbn.common.state.PersistentStateElement;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jdom.Element;

import java.util.Map;

import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

/**
 * Chat conversation context - preserving profile and model selection against a conversation
 */
@Getter
@Setter
@NoArgsConstructor
public class ChatConversationContext implements PersistentStateElement {
    private static final Gson GSON = new GsonBuilder().create();

    private String profile;
    private AIModel model;

    public ChatConversationContext(String profile, AIModel model) {
        this.profile = profile;
        this.model = model;
    }

    public String getAttributes() {
        Map<String, String> attributes = Map.of("model", model.getApiName());
        return GSON.toJson(attributes);
    }

    @Override
    public void readState(Element element) {
        profile = stringAttribute(element, "profile");
        model = AIModel.forId(stringAttribute(element, "model"));
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "profile", profile);
        setStringAttribute(element, "model", model.getId());
    }
}

