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

package com.dbn.assistant.service.generic.context;

import com.dbn.assistant.chat.Chat;
import com.dbn.assistant.chat.message.AuthorType;
import com.dbn.assistant.chat.message.ChatMessage;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateExtension;
import com.dbn.common.action.UserDataKeys;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.assistant.chat.message.AuthorType.AGENT;
import static com.dbn.assistant.chat.message.AuthorType.USER;
import static com.dbn.common.action.UserDataKeys.ASSISTANT_MEMORY_CACHE;

public class AssistantMemoryCache extends AssistantStateExtension implements ChatMemoryProvider {
    private final Map<String, ChatMemory> entries = new ConcurrentHashMap<>();

    private AssistantMemoryCache(@NotNull AssistantState assistantState) {
        super(assistantState);
    }

    public ChatMemory get(String chatId) {
        return entries.computeIfAbsent(chatId, k -> createChatMemory(chatId));
    }

    public static AssistantMemoryCache get(AssistantState assistantState) {
        return UserDataKeys.getUserDataSync(assistantState, ASSISTANT_MEMORY_CACHE, () -> new AssistantMemoryCache(assistantState));
    }

    private ChatMemory createChatMemory(String chatId) {
        // TODO configurative message-window vs. token-window
        // TokenWindowChatMemory.withMaxTokens(10000, new TokenCountEstimator());

        ChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(chatId)
                .maxMessages(100)
                .build();

        restoreChatMemory(chatMemory, chatId);
        return chatMemory;
    }

    private void restoreChatMemory(ChatMemory chatMemory, String chatId) {
        AssistantState assistantState = getAssistantState();
        Chat chat = assistantState.getChat(chatId);
        if (chat == null) return;

        List<ChatMessage> messages = chat.getMessages();
        if (messages.isEmpty()) return;

        for (var message : messages) {
            if (chat.isRecentPrompt(message)) return; // skip the last prompt from memory restore (will be added by the framework)

            AuthorType author = message.getAuthor();
            String content = message.getContent();
            if (author == USER) {
                UserMessage userMessage = UserMessage.from(content);
                chatMemory.add(userMessage);
            } else if (author == AGENT) {
                AiMessage agentMessage = AiMessage.from(content);
                chatMemory.add(agentMessage);
            }
        }
    }

    @Override
    public ChatMemory get(Object memoryId) {
        return get(Objects.toString(memoryId));
    }
}
