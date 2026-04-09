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
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.mcp.AssistantMcpServerData;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIModelFeature;
import com.dbn.assistant.service.generic.context.AssistantInstructionsCache;
import com.dbn.assistant.service.generic.context.AssistantMemoryCache;
import com.dbn.assistant.service.generic.context.AssistantMemoryId;
import com.dbn.assistant.service.generic.model.AssistantModelInvoker;
import com.dbn.assistant.service.generic.model.AssistantModelType;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.AssistantToolCache;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.dbn.assistant.provider.AIModelFeature.INSTRUCTIONS;
import static com.dbn.assistant.provider.AIModelFeature.TOOLS;

@Slf4j
@Getter
abstract class AbstractModelInvoker<T> implements AssistantModelInvoker<T> {
    private final AssistantModelType modelType;

    public AbstractModelInvoker(AssistantModelType modelType) {
        this.modelType = modelType;
    }

    protected static ModelInvocationContext creatInvocationContext(AssistantState state, AssistantMemoryId memoryId, AssistantResponseConsumer consumer) {
        return ModelInvocationContext
                .builder()
                .memoryId(memoryId)
                .assistantState(state)
                .responseConsumer(consumer)
                .build();
    }

    protected static void initInternalToolProvider(AiServices<?> builder, ModelInvocationContext context) {
        if (context.isStateless()) return;

        AssistantState assistantState = context.getAssistantState();
        if (!isFeatureSupported(assistantState, TOOLS)) return;

        var tools = AssistantToolCache.get(assistantState);
        builder.toolProvider(tools.getProvider());
    }

    protected static void initExternalToolProviders(AiServices<?> builder, ModelInvocationContext context) {
        if (context.isStateless()) return;

        AssistantState assistantState = context.getAssistantState();
        if (!isFeatureSupported(assistantState, TOOLS)) return;

        AssistantResponseConsumer responseConsumer = context.getResponseConsumer();
        AssistantMcpServerData mcpServerData = AssistantMcpServerData.get(assistantState);
        List<ToolProvider> tools = mcpServerData.createToolProviders((m, e) -> responseConsumer.acceptToolError(m, e));
        builder.toolProviders(tools);
    }

    protected static void initSystemMessage(AiServices<?> builder, ModelInvocationContext context) {
        AssistantState assistantState = context.getAssistantState();
        if (!isFeatureSupported(assistantState, INSTRUCTIONS)) return;

        var instructions = AssistantInstructionsCache.get(assistantState);
        builder.systemMessageProvider(instructions);
    }

    protected static void initChatMemory(AiServices<?> builder, ModelInvocationContext context) {
        var memory = context.isStateless() ?
                prepareEmptyMemory() :
                prepareMemory(context.getAssistantState());
        builder.chatMemoryProvider(memory);
    }

    private static ChatMemoryProvider prepareEmptyMemory() {
        return memoryId -> MessageWindowChatMemory
                .builder()
                .id(memoryId)
                .maxMessages(5)
                .build();
    }

    protected static ChatMemoryProvider prepareMemory(AssistantState assistantState) {
        return AssistantMemoryCache.get(assistantState);
    }

    private static boolean isFeatureSupported(AssistantState state, AIModelFeature feature) {
        ChatContext context = state.getCurrentContext();
        AIModel model = context.getModel();
        if (model == null) return false;
        return model.isFeatureSupported(feature);
    }
}
