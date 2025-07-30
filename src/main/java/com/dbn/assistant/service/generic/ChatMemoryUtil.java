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

package com.dbn.assistant.service.generic;

import com.dbn.assistant.chat.Chat;
import com.dbn.assistant.chat.message.AuthorType;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.action.UserDataKeys;
import com.dbn.common.util.Lists;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import static com.dbn.assistant.chat.message.AuthorType.AGENT;
import static com.dbn.assistant.chat.message.AuthorType.USER;

@UtilityClass
public class ChatMemoryUtil {

    @Nullable
    public static ChatMemory getCharMemory(String chatId, String prompt, AssistantState assistantState) {
        if (assistantState == null) return null;

        ChatMemory memory = UserDataKeys.CHAT_MEMORY.get(assistantState);
        if (memory != null) return memory;

        Chat chat = assistantState.getChat(chatId);
        memory = createChatMemory(chat);

        // memory may already contain the last prompt
        var messages = memory.messages();
        ChatMessage message = Lists.lastElement(messages);
        if (message instanceof UserMessage) {
            UserMessage userMessage = (UserMessage) message;
            if (userMessage.hasSingleText() && userMessage.singleText().equals(prompt)) {
                messages.remove(message);
            }
        }

        UserDataKeys.CHAT_MEMORY.set(assistantState, memory);
        return memory;
    }

    private static ChatMemory createChatMemory(Chat chat) {
        // TODO configurative message window /
        //TokenWindowChatMemory.withMaxTokens(10000, new OpenAiTokenCountEstimator());

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(100);
        for (var message : chat.getMessages()) {
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
        return chatMemory;
    }
}
