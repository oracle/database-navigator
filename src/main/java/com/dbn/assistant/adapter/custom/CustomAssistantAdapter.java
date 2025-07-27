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

package com.dbn.assistant.adapter.custom;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.adapter.AssistantAdapterBase;
import com.dbn.assistant.adapter.ui.AssistantContextActionsForm;
import com.dbn.assistant.adapter.ui.AssistantIntroductionForm;
import com.dbn.assistant.adapter.ui.AssistantPromptActionsForm;
import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.common.exception.Exceptions;
import com.dbn.connection.ConnectionId;

import static com.dbn.nls.NlsResources.txt;

public class CustomAssistantAdapter extends AssistantAdapterBase {
    public static final CustomAssistantAdapter INSTANCE = new CustomAssistantAdapter();

    public CustomAssistantAdapter() {
        super(AssistantType.CUSTOM);
    }

    @Override
    public ChatContext createChatContext(ConnectionId connectionId) {
        return null;
    }

    @Override
    public AssistantIntroductionForm createIntroductionForm(ChatBoxForm chatBoxForm) {
        return null;
    }

    @Override
    public AssistantContextActionsForm createContextActionsForm(ChatBoxForm chatBoxForm) {
        return null;
    }

    @Override
    public AssistantPromptActionsForm createPromptActionsForm(ChatBoxForm chatBoxForm) {
        return null;
    }

    @Override
    public ChatAvailability getChatAvailability(ConnectionId connectionId) {
        return ChatAvailability.AVAILABLE;
    }

    @Override
    public void initializeAssistant(ConnectionId connectionId) {

    }

    @Override
    public boolean isCurrentChatActive(ConnectionId connectionId) {
        return true;
    }

    @Override
    public boolean isCurrentContextEnabled(ConnectionId connectionId) {
        return true;
    }

    @Override
    public boolean isCurrentContextValid(ConnectionId connectionId) {
        return true;
    }

    @Override
    public void showHelpDialog(ConnectionId connectionId) {

    }

    @Override
    public String preparePrompt(ConnectionId connectionId, ChatContext chatContext, String prompt) {
        return prompt;
    }

    @Override
    public String prepareError(ConnectionId connectionId, ChatContext chatContext, Throwable e) {
        e = Exceptions.rootCauseOf(e);
        String errorMessage = Exceptions.getMessage(e);
        return txt("msg.assistant.error.AssistantInvocationFailure", getAssistantType().getName(), errorMessage);
    }

    @Override
    public String generate(
            String prompt,
            ConnectionId connectionId,
            ChatContext chatContext) {
        return ""; // TODO

    }
}
