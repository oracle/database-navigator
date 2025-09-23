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

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.adapter.AssistantAdapter;
import com.dbn.assistant.adapter.AssistantResponseConsumer;
import com.dbn.assistant.chat.Chat;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.message.AuthorType;
import com.dbn.assistant.chat.message.ChatMessage;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolCache;
import com.dbn.assistant.tool.approval.AssistantToolApprovalException;
import com.dbn.assistant.tool.execution.AssistantToolInvocation;
import com.dbn.assistant.tool.execution.AssistantToolInvocationMonitor;
import com.dbn.assistant.tool.execution.AssistantToolRequest;
import com.dbn.common.message.MessageType;
import com.dbn.connection.ConnectionId;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.assistant.chat.message.AuthorType.AGENT;
import static com.dbn.assistant.chat.message.AuthorType.SYSTEM;
import static com.dbn.assistant.chat.message.AuthorType.USER;
import static com.dbn.assistant.state.AssistantStatus.QUERYING;
import static com.dbn.common.message.MessageType.ERROR;
import static com.dbn.common.message.MessageType.NEUTRAL;
import static com.dbn.common.util.Commons.nvl;

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
            ChatMessage agentMessage = createMessage(NEUTRAL, token, AGENT);
            chatBoxForm.appendMessage(chatId, agentMessage);
        } else if (author == AGENT) {
            lastMessage.appendToken(token);
            chatBoxForm.refreshMessage(lastMessage);
            // TODO update UI
        }
    }

    @Override
    public void acceptMessage(String message) {
        // ignore if token-stream is supported
        if (tokenized) return;
        message = nvl(message, "");

        ChatMessage agentMessage = createMessage(NEUTRAL, message, AGENT);
        chatBoxForm.appendMessage(chatId, agentMessage);
        log.info("Assistant query processed successfully.");
    }

    @Override
    public void acceptError(Throwable e) {
        if (e instanceof AssistantToolApprovalException) return;

        log.warn("Error processing assistant query", e);

        ConnectionId connectionId = getConnectionId();
        AssistantAdapter assistantAdapter = chatBoxForm.getAssistantAdapter();

        String message = assistantAdapter.prepareError(connectionId, chatContext, e);
        ChatMessage errorMessage = createMessage(ERROR, message, SYSTEM);
        chatBoxForm.appendMessage(chatId, errorMessage);
    }

    @Override
    public void acceptCompletion() {
        AssistantState assistantState = getAssistantState();
        assistantState.set(QUERYING, false);
    }

    @Override
    public void acceptToolRequest(String requestId, String toolName, String toolArguments) {
        Chat chat = getChat();
        if (chat == null) return; // chat discarded

        ChatMessage lastMessage = chat.getLastMessage();
        if (lastMessage == null) return;

        AssistantToolInvocation invocation = initToolInvocation(chatId, requestId, toolName, toolArguments);
        if (invocation == null) return;

        AuthorType author = lastMessage.getAuthor();
        if (author == USER) {
            // agent responded directly with a tool request
            lastMessage = createMessage(NEUTRAL, "", AGENT);
            lastMessage.appendToolRequest(invocation);
            chatBoxForm.appendMessage(chatId, lastMessage);
        } else if (author == AGENT) {
            lastMessage.appendToolRequest(invocation);
            chatBoxForm.refreshTools(lastMessage);
        }
    }

    @Nullable
    private AssistantToolInvocation initToolInvocation(String chatId, String requestId, String toolName, String toolArguments) {
        AssistantToolCache cache = getToolCache();
        AssistantTool tool = cache.getAssistantTool(toolName);
        if (tool == null) return null;

        AssistantToolRequest request = new AssistantToolRequest(chatId, requestId, toolName, toolArguments);
        AssistantToolInvocation invocation = new AssistantToolInvocation(request);

        AssistantState state = getAssistantState();
        AssistantToolInvocationMonitor monitor = new AssistantToolInvocationMonitor(state, tool);
        invocation.setMonitor(monitor);
        return invocation;
    }

    @Override
    public void acceptToolResponse(String requestId, String toolName, String toolResponse) {
        Chat chat = getChat();
        if (chat == null) return; // chat discarded

        ChatMessage lastMessage = chat.getLastMessage();
        if (lastMessage == null) return;

        if (lastMessage.getAuthor() == AGENT) {
            lastMessage.appendToolResponse(requestId, toolName, toolResponse);
            chatBoxForm.refreshTools(lastMessage);
        }
    }

    private @NotNull ChatMessage createMessage(MessageType type, String message, AuthorType author) {
        return new ChatMessage(getAssistantType(), type, message, author, chatContext);
    }


    private ConnectionId getConnectionId() {
        return chatBoxForm.getConnectionId();
    }

    private Chat getChat() {
        return chatBoxForm.getChat(chatId);
    }

    private AssistantState getAssistantState() {
        return chatBoxForm.getAssistantState();
    }

    private AssistantType getAssistantType() {
        return getAssistantState().getAssistantType();
    }

    private AssistantToolCache getToolCache() {
        AssistantState assistantState = getAssistantState();
        return AssistantToolCache.get(assistantState);
    }
}
