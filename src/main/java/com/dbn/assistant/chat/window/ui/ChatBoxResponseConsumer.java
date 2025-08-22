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

package com.dbn.assistant.chat.window.ui;

import com.dbn.assistant.adapter.AssistantAdapter;
import com.dbn.assistant.adapter.AssistantResponseConsumer;
import com.dbn.assistant.chat.Chat;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.message.AuthorType;
import com.dbn.assistant.chat.message.ChatMessage;
import com.dbn.assistant.state.AssistantState;
import com.dbn.connection.ConnectionId;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import static com.dbn.assistant.chat.message.AuthorType.AGENT;
import static com.dbn.assistant.chat.message.AuthorType.SYSTEM;
import static com.dbn.assistant.chat.message.AuthorType.USER;
import static com.dbn.assistant.state.AssistantStatus.QUERYING;
import static com.dbn.common.message.MessageType.ERROR;
import static com.dbn.common.message.MessageType.NEUTRAL;

@Slf4j
class ChatBoxResponseConsumer implements AssistantResponseConsumer {
    private final ChatBoxForm chatBoxForm;
    private final ChatContext chatContext;
    private final String chatId;
    private transient boolean tokenized;

    public ChatBoxResponseConsumer(ChatBoxForm chatBoxForm, ChatContext chatContext, String chatId) {
        this.chatBoxForm = chatBoxForm;
        this.chatContext = chatContext;
        this.chatId = chatId;
    }

    @Override
    public void acceptToken(String token) {
        tokenized = true;
        Chat chat = getChat();
        ChatMessage lastMessage = chat.getLastMessage();
        if (lastMessage == null) return;

        AuthorType author = lastMessage.getAuthor();
        if (author == USER) {
            // consume first agent token
            ChatMessage agentMessage = new ChatMessage(NEUTRAL, token, AGENT, chatContext);
            chatBoxForm.appendMessage(chatId, agentMessage);
        } else if (author == AGENT) {
            lastMessage.appendToken(token);
            chatBoxForm.refreshMessage(lastMessage);
            // TODO update UI
        }
    }

    private @NotNull ConnectionId getConnectionId() {
        return chatBoxForm.getConnectionId();
    }

    private Chat getChat() {
        return chatBoxForm.getChat(chatId);
    }

    @Override
    public void acceptMessage(String message) {
        // ignore if token-stream is supported
        if (tokenized) return;

        ChatMessage agentMessage = new ChatMessage(NEUTRAL, message, AGENT, chatContext);
        chatBoxForm.appendMessage(chatId, agentMessage);
        log.info("Assistant query processed successfully.");
    }

    @Override
    public void acceptError(Throwable e) {
        log.warn("Error processing assistant query", e);

        ConnectionId connectionId = getConnectionId();
        AssistantAdapter assistantAdapter = chatBoxForm.getAssistantAdapter();

        String message = assistantAdapter.prepareError(connectionId, chatContext, e);
        ChatMessage errorMessage = new ChatMessage(ERROR, message, SYSTEM, chatContext);
        chatBoxForm.appendMessage(chatId, errorMessage);
    }

    @Override
    public void acceptCompletion() {
        AssistantState assistantState = chatBoxForm.getAssistantState();
        assistantState.set(QUERYING, false);
    }
}
