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

import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIModelFeature;
import com.dbn.assistant.service.generic.context.AssistantInstructionsCache;
import com.dbn.assistant.service.generic.context.AssistantMemoryCache;
import com.dbn.assistant.service.generic.model.AssistantModelInvoker;
import com.dbn.assistant.service.generic.model.AssistantModelType;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.AssistantToolCache;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.dbn.assistant.provider.AIModelFeature.INSTRUCTIONS;
import static com.dbn.assistant.provider.AIModelFeature.TOOLS;

@Slf4j
@Getter
abstract class AbstractModelInvoker<T> implements AssistantModelInvoker<T> {
    private final AssistantModelType modelType;

    public AbstractModelInvoker(AssistantModelType modelType) {
        this.modelType = modelType;
    }

    protected void initToolProvider(AiServices<?> builder, AssistantState state, boolean stateless) {
        if (stateless) return;
        if (!isFeatureSupported(state, TOOLS)) return;

        var tools = AssistantToolCache.get(state);
        builder.toolProvider(tools);
    }

    protected void initSystemMessage(AiServices<?> builder, AssistantState state) {
        if (!isFeatureSupported(state, INSTRUCTIONS)) return;

        var instructions = AssistantInstructionsCache.get(state);
        builder.systemMessageProvider(instructions);
    }

    protected void initChatMemory(AiServices<?> builder, AssistantState state, boolean stateless) {
        var memory = stateless ?
                prepareEmptyMemory() :
                prepareMemory(state);
        builder.chatMemoryProvider(memory);
    }

    private static ChatMemoryProvider prepareEmptyMemory() {
        return memoryId -> MessageWindowChatMemory
                .builder()
                .id(memoryId)
                .maxMessages(5)
                .build();
    }

    protected ChatMemoryProvider prepareMemory(AssistantState assistantState) {
        return AssistantMemoryCache.get(assistantState);
    }

    private boolean isFeatureSupported(AssistantState state, AIModelFeature feature) {
        ChatContext context = state.getCurrentContext();
        AIModel model = context.getModel();
        if (model == null) return false;
        return model.isFeatureSupported(feature);
    }
}
