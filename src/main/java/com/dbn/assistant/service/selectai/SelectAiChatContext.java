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

package com.dbn.assistant.service.selectai;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.context.ChatContextImpl;
import com.dbn.assistant.provider.AIModel;
import com.dbn.common.util.Strings;
import com.dbn.object.DBAIProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Chat message context - preserving profile, model and action selection against an AI response message
 *
 * @author Dan Cioca (Oracle)
 */
@Getter // pseudo final (no setters)
public final class SelectAiChatContext implements ChatContext {
    private static final Gson GSON = new GsonBuilder().create();

    @Delegate
    private final ChatContext delegate;

    private SelectAiChatContext(ChatContext delegate) {
        this.delegate = delegate;
        initActionName();
    }

    public static SelectAiChatContext wrap(ChatContext delegate) {
        if (delegate instanceof SelectAiChatContext) {
            return (SelectAiChatContext) delegate;
        }
        return new SelectAiChatContext(delegate);
    }

    public SelectAiChatContext(@Nullable DBAIProfile profile) {
        this.delegate = new ChatContextImpl(AssistantType.SELECT_AI);
        if (profile == null) return;

        setProfileName(profile.getName());
        this.setProviderId(profile.getProviderId());
        this.setModelId(profile.getModelId());
        setInteractive(profile.isInteractive());
        initActionName();
    }

    private void initActionName() {
        if (Strings.isEmpty(delegate.getActionId())) {
            delegate.setActionId(PromptAction.SHOW_SQL.name());
        }
    }

    public PromptAction getAction() {
        return PromptAction.get(this.getActionId());
    }

    public String getAttributes() {
        AIModel model = getModel();
        String modelApiName = model == null ? "undefined" : model.getApiName();
        Map<String, String> attributes = Map.of("model", modelApiName);
        return GSON.toJson(attributes);
    }

    public boolean isActionSwitch(ChatContext that) {
        // when switched between CHAT and any other action, the chat is interrupted
        return (this.getAction() == PromptAction.CHAT) == (that.getAction() != PromptAction.CHAT);
    }
}
