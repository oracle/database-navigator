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

import com.dbn.assistant.chat.window.PromptAction;
import com.dbn.assistant.provider.AIModel;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.object.DBAIProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

/**
 * Chat message context - preserving profile, model and action selection against an AI response message
 *
 * @author Dan Cioca (Oracle)
 */
@Getter // pseudo final (no setters)
@NoArgsConstructor
public final class ChatContext implements PersistentStateElement {
    private static final Gson GSON = new GsonBuilder().create();

    private String profileName;
    private String modelName;
    private PromptAction action = PromptAction.SHOW_SQL;
    private boolean interactive;

    public ChatContext(String profileName, String modelName, PromptAction action, boolean interactive) {
        this.profileName = profileName;
        this.modelName = modelName;
        this.action = action;
        this.interactive = interactive;
    }

    public ChatContext(@Nullable DBAIProfile profileName) {
        if (profileName == null) {
            this.profileName = null;
            this.modelName = null;
            this.interactive = false;

        } else {
            this.profileName = profileName.getName();
            this.modelName = profileName.getModel().getName();
            this.interactive = profileName.isInteractive();
        }
    }

    public AIModel getModel() {
        return AIModel.forName(this.modelName);
    }

    public String getAttributes() {
        AIModel model = getModel();
        String modelApiName = model == null ? "undefined" : model.getApiName();
        Map<String, String> attributes = Map.of("model", modelApiName);
        return GSON.toJson(attributes);
    }

    public boolean isProfileSwitch(ChatContext that) {
        return !Objects.equals(this.profileName, that.profileName);
    }

    public boolean isModelSwitch(ChatContext that) {
        return !Objects.equals(this.modelName, that.modelName);
    }

    public boolean isInterruptingActionSwitch(ChatContext that) {
        // when switched between CHAT and any other action, the chat is interrupted
        return (this.action == PromptAction.CHAT) == (that.action != PromptAction.CHAT);
    }

    @Override
    public void readState(Element element) {
        profileName = stringAttribute(element, "profile");
        modelName = stringAttribute(element, "model");
        action = enumAttribute(element, "action", PromptAction.class);
        interactive = booleanAttribute(element, "interactive", interactive);
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "profile", profileName);
        setStringAttribute(element, "model", modelName);
        setEnumAttribute(element, "action", action);
        setBooleanAttribute(element, "interactive", interactive);
    }

    @Override
    public String toString() {
        return profileName + " / " + modelName + " / " + action;
    }
}
