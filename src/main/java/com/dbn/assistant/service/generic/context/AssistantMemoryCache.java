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
import com.dbn.assistant.chat.message.ChatMessageToolSection;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateExtension;
import com.dbn.assistant.tool.execution.AssistantToolRequest;
import com.dbn.common.action.UserDataKeys;
import com.dbn.common.util.Strings;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.assistant.chat.message.AuthorType.AGENT;
import static com.dbn.assistant.chat.message.AuthorType.USER;
import static com.dbn.common.action.UserDataKeys.ASSISTANT_MEMORY_CACHE;
import static dev.langchain4j.data.message.ToolExecutionResultMessage.toolExecutionResultMessage;
import static java.util.Collections.singletonList;

public class AssistantMemoryCache extends AssistantStateExtension implements ChatMemoryProvider {
    private final Map<AssistantMemoryId, ChatMemory> entries = new ConcurrentHashMap<>();

    private AssistantMemoryCache(@NotNull AssistantState assistantState) {
        super(assistantState);
    }

    public ChatMemory get(AssistantMemoryId memoryId) {
        return entries.compute(memoryId, (k, v) ->
                v == null ?
                    createChatMemory(k) :
                    cleanupChatMemory(v));
    }

    private static boolean isValidMessage(ChatMessage message) {
        if (message instanceof AiMessage) {

            AiMessage aiMessage = (AiMessage) message;
            List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests();
            boolean hasTools = toolExecutionRequests != null && !toolExecutionRequests.isEmpty();
            boolean hasContent = Strings.isNotEmpty(aiMessage.text());

            return hasTools || hasContent;
        }
        return true;
    }

    public static AssistantMemoryCache get(AssistantState assistantState) {
        return UserDataKeys.getUserDataSync(assistantState, ASSISTANT_MEMORY_CACHE, () -> new AssistantMemoryCache(assistantState));
    }

    private ChatMemory createChatMemory(AssistantMemoryId memoryId) {
        ChatMemory chatMemory = createMemory(memoryId);

        String chatId = memoryId.getChatId();
        restoreChatMemory(chatMemory, chatId);
        return chatMemory;
    }

    private ChatMemory cleanupChatMemory(ChatMemory memory) {
        var messages = memory.messages();
        boolean hasEmptyMessages = messages.stream().anyMatch(m -> !isValidMessage(m));
        if (hasEmptyMessages) {
            ChatMemory cleanMemory = createMemory(memory.id());
            messages.stream().filter(m -> isValidMessage(m)).forEach(m -> cleanMemory.add(m));
            return cleanMemory;
        }
        return memory;
    }

    private static ChatMemory createMemory(Object memoryId) {
        // TODO configurative message-window vs. token-window
        // TokenWindowChatMemory.withMaxTokens(10000, new TokenCountEstimator());

        return MessageWindowChatMemory
                .builder()
                .id(memoryId)
                .maxMessages(100)
                .build();
    }

    private void restoreChatMemory(ChatMemory chatMemory, String chatId) {
        AssistantState assistantState = getAssistantState();
        Chat chat = assistantState.getChat(chatId);
        if (chat == null) return;

        var messages = chat.getMessages();
        if (messages.isEmpty()) return;

        for (var message : messages) {
            if (chat.isRecentPrompt(message)) return; // skip the last prompt from memory restore (will be added by the framework)

            AuthorType author = message.getAuthor();
            String content = message.getContent();
            if (author == USER) {
                UserMessage userMessage = UserMessage.from(content);
                chatMemory.add(userMessage);
            } else if (author == AGENT) {
                List<ChatMessageToolSection> toolSections = message.getToolSections();
                if (toolSections == null || toolSections.isEmpty()) {
                    AiMessage agentMessage = AiMessage.from(content);
                    chatMemory.add(agentMessage);
                } else {
                    int offset = 0;
                    for (ChatMessageToolSection toolSection : toolSections) {
                        AssistantToolRequest toolRequest = toolSection.getRequest();
                        String toolRequestId = toolRequest.getRequestId();
                        String toolName = toolRequest.getUtilityName();
                        String toolArguments = toolRequest.getUtilityArguments();

                        int toolOffset = toolSection.getOffset();
                        String sectionContent = content.substring(offset, toolOffset);

                        ToolExecutionRequest executionRequest = ToolExecutionRequest
                                .builder()
                                .id(toolRequestId)
                                .name(toolName)
                                .arguments(toolArguments)
                                .build();

                        AiMessage agentMessage = AiMessage.from(sectionContent, singletonList(executionRequest));
                        chatMemory.add(agentMessage);

                        String toolResponseContent = toolSection.getResponseContent();
                        if (Strings.isNotEmpty(toolResponseContent)) {
                            ToolExecutionResultMessage toolResultMessage = toolExecutionResultMessage(toolRequestId, toolName, toolResponseContent);
                            chatMemory.add(toolResultMessage);
                        }

                        offset = toolOffset;
                    }

                    if (offset < content.length()) {
                        AiMessage agentMessage = AiMessage.from(content.substring(offset));
                        chatMemory.add(agentMessage);
                    }

                }


            }
        }
    }

    @Override
    public ChatMemory get(Object memoryId) {
        return get((AssistantMemoryId) memoryId);
    }
}
