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
import static com.dbn.common.util.Strings.isNotEmpty;

/**
 * Chat message context - preserving profile, model and action selection against an AI response message
 *
 * @author Dan Cioca (Oracle)
 */
@Getter // pseudo final (no setters)
@NoArgsConstructor
public final class ChatContext implements PersistentStateElement {
    private static final Gson GSON = new GsonBuilder().create();

    private String profile;
    private AIModel model;
    private PromptAction action = PromptAction.SHOW_SQL;
    private boolean interactive;

    public ChatContext(String profile, AIModel model, PromptAction action, boolean interactive) {
        this.profile = profile;
        this.model = model;
        this.action = action;
        this.interactive = interactive;
    }

    public String getAttributes() {
        Map<String, String> attributes = Map.of("model", model.getApiName());
        return GSON.toJson(attributes);
    }

    public boolean isInitialized() {
        return isNotEmpty(profile) && model != null;
    }

    public void initialize(@Nullable DBAIProfile profile) {
        if (profile == null) {
            this.profile = null;
            this.model = null;
            this.interactive = false;

        } else {
            this.profile = profile.getName();
            this.model = profile.getModel();
            this.interactive = profile.isInteractive();
        }
    }

    public boolean isProfileSwitch(ChatContext that) {
        return !Objects.equals(this.profile, that.profile);
    }

    public boolean isModelSwitch(ChatContext that) {
        return !Objects.equals(this.model, that.model);
    }

    public boolean isInterruptingActionSwitch(ChatContext that) {
        // when switched between CHAT and any other action, the chat is interrupted
        return (this.action == PromptAction.CHAT) == (that.action != PromptAction.CHAT);
    }

    @Override
    public void readState(Element element) {
        profile = stringAttribute(element, "profile");
        model = AIModel.forId(stringAttribute(element, "model"));
        action = enumAttribute(element, "action", PromptAction.class);
        interactive = booleanAttribute(element, "interactive", interactive);
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "profile", profile);
        setStringAttribute(element, "model", AIModel.getId(model));
        setEnumAttribute(element, "action", action);
        setBooleanAttribute(element, "interactive", interactive);
    }

    @Override
    public String toString() {
        return profile + " / " + model + " / " + action;
    }
}
