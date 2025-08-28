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

package com.dbn.assistant.service.generic.model.invoker;

import com.dbn.assistant.adapter.AssistantResponseConsumer;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.jetbrains.annotations.Nullable;

public class ChatModelInvoker extends AbstractModelInvoker<ChatModel>{
    public ChatModelInvoker() {
        super(ChatModel.class);
    }

    @Override
    public void invokeModel(ChatModel model, @Nullable ChatMemory memory, String prompt, AssistantResponseConsumer consumer) {
        try {
            ChatModelAdapter adapter;

            if (memory == null) {
                adapter = AiServices.
                        builder(ChatModelAdapter.class).
                        chatModel(model).
                        tools(getAssistantMcpTools()).
                        build();
            } else {
                adapter = AiServices.
                        builder(ChatModelAdapter.class).
                        chatModel(model).
                        chatMemory(memory).
                        tools(getAssistantMcpTools()).
                        build();
            }

            String message = adapter.chat(prompt);
            consumer.acceptMessage(message);

        } catch (Throwable e) {
            consumer.acceptError(e);

        } finally {
            consumer.acceptCompletion();
        }
    }
}
