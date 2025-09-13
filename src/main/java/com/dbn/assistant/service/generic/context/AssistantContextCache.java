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

import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateExtension;
import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolCache;
import com.dbn.assistant.tool.AssistantToolCategory;
import com.dbn.common.action.UserDataKeys;
import com.dbn.common.text.TextContent;
import com.dbn.common.text.TextResources;
import com.dbn.connection.ConnectionHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dbn.common.action.UserDataKeys.ASSISTANT_CONTEXT_PROVIDER;

public class AssistantContextCache extends AssistantStateExtension implements Function<Object, String> {
    private final Map<String, String> entries = new ConcurrentHashMap<>();

    private AssistantContextCache(@NotNull AssistantState assistantState) {
        super(assistantState);
    }

    public static AssistantContextCache get(AssistantState assistantState) {
        return UserDataKeys.getUserDataSync(assistantState, ASSISTANT_CONTEXT_PROVIDER, () -> new AssistantContextCache(assistantState));
    }

    @Override
    public String apply(Object memoryId) {
        String chatId = Objects.toString(memoryId);
        return entries.computeIfAbsent(chatId, k -> createSystemMessage(chatId));
    }

    private String createSystemMessage(String chatId) {
        AssistantState assistantState = getAssistantState();

        ConnectionHandler connection = assistantState.getConnection();
        String content = TextResources.get(this, "system_message.md.ft");
        TextContent textContent = TextContent.markdown(content);
        textContent.initField("ASSISTANT_TOOL_CATEGORIES", getToolCategories());
        textContent.initField("ASSISTANT_TOOL_TYPES", getToolTypes());
        textContent.initField("DATABASE_TYPE", connection.getDatabaseType().getName());
        textContent.initField("DATABASE_NAME", connection.getName());

        return textContent.getText();
    }


    public String getToolCategories() {
        AssistantToolCache toolCache = getToolCache();
        AssistantToolCategory[] categories = toolCache.getAvailableToolCategories();
        return Arrays
                .stream(categories)
                .map(c -> "  * " + c.name() + ": " + c.getDescription())
                .collect(Collectors.joining("\n"));
    }

    private String getToolTypes() {
        AssistantToolCache toolCache = getToolCache();
        AssistantTool[] tools = toolCache.getAvailableTools();
        return Arrays
                .stream(tools)
                .map(t -> "  * " + t.getType() + ": " + t.getDescription())
                .collect(Collectors.joining("\n"));
    }

    private AssistantToolCache getToolCache() {
        AssistantState assistantState = getAssistantState();
        return AssistantToolCache.get(assistantState);
    }
}
