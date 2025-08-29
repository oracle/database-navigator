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

import com.dbn.assistant.memory.ChatMemoryCache;
import com.dbn.assistant.service.generic.model.AssistantModelInvoker;
import com.dbn.assistant.service.generic.model.AssistantModelType;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolFactories;
import com.dbn.assistant.tool.AssistantToolFactory;
import com.dbn.connection.ConnectionHandler;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
abstract class AbstractModelInvoker<T> implements AssistantModelInvoker<T> {
    private final AssistantModelType modelType;

    public AbstractModelInvoker(AssistantModelType modelType) {
        this.modelType = modelType;
    }

    protected AssistantTool[] prepareTools(AssistantState state) {
        List<AssistantTool> tools = new ArrayList<>();
        List<AssistantToolFactory> factories = AssistantToolFactories.list();
        for (AssistantToolFactory factory : factories) {
            try {
                // TODO cache the tools
                ConnectionHandler connection = state.getConnection();
                AssistantTool tool = factory.createTool(connection);
                tools.add(tool);
            } catch (Throwable e) {
                log.error("Failed to create {} assistant tool of type {} (class {})",
                        factory.getToolCategory(),
                        factory.getToolType(),
                        factory.getToolClass(), e);
            }
        }

        return tools.toArray(new AssistantTool[0]);
    }

    protected ChatMemoryProvider prepareMemory(AssistantState assistantState) {
        return ChatMemoryCache.get(assistantState);
    }
}
