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

package com.dbn.assistant.adapter;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.adapter.ui.AssistantContextActionsForm;
import com.dbn.assistant.adapter.ui.AssistantIntroductionForm;
import com.dbn.assistant.adapter.ui.AssistantPromptActionsForm;
import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.extensions.ExtensionPointName;

public interface AssistantAdapter {
    ExtensionPointName<AssistantAdapter> EP = ExtensionPointName.create("com.dbn.assistantAdapter");

    AssistantType getAssistantType();

    ChatContext createChatContext(ConnectionId connectionId);

    ChatContext enrichChatContext(ChatContext context);

    AssistantIntroductionForm createIntroductionForm(ChatBoxForm chatBoxForm);

    /**
     * Creates the context selection actions form
     * (typically displayed at the top of the assistant chat window)
     *
     * @return the form to be displayed on top of the assistant chat window
     */
    AssistantContextActionsForm createContextActionsForm(ChatBoxForm chatBoxForm);

    /**
     * Creates the prompt actions form
     * (typically displayed at the bottom of the assistant chat window, just above the prompt text-area)
     *
     * @return the form to be displayed at the bottom of the assistant chat window
     */
    AssistantPromptActionsForm createPromptActionsForm(ChatBoxForm chatBoxForm);

    ChatAvailability getChatAvailability(ConnectionId connectionId);

    void initializeAssistant(ConnectionId connectionId);

    boolean isCurrentChatActive(ConnectionId connectionId);

    boolean isCurrentContextEnabled(ConnectionId connectionId);

    boolean isCurrentContextValid(ConnectionId connectionId);

    void showHelpDialog(ConnectionId connectionId);

    String buildChatContextTitle(ChatContext context);

    void generate(
            String prompt,
            String chatId,
            ConnectionId connectionId,
            ChatContext chatContext,
            AssistantResponseConsumer responseConsumer);

    String generateTitle(
            String chatId,
            ConnectionId connectionId,
            ChatContext context) throws Exception;

    String preparePrompt(ConnectionId connectionId, ChatContext chatContext, String prompt);

    String prepareError(ConnectionId connectionId, ChatContext chatContext, Throwable e);
}
