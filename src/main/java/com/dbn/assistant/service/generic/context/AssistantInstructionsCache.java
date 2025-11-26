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
import com.dbn.assistant.profile.AssistantProfile;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProviderId;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateExtension;
import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolCache;
import com.dbn.assistant.tool.AssistantToolCategory;
import com.dbn.common.action.UserDataKeys;
import com.dbn.common.text.TextContent;
import com.dbn.common.text.TextResources;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dbn.assistant.profile.AssistantProfileLookup.getProfile;
import static com.dbn.common.action.UserDataKeys.ASSISTANT_INSTRUCTIONS_CACHE;

public class AssistantInstructionsCache extends AssistantStateExtension implements Function<Object, String> {
    private final Map<AssistantMemoryId, String> entries = new ConcurrentHashMap<>();

    private AssistantInstructionsCache(@NotNull AssistantState assistantState) {
        super(assistantState);
    }

    public static AssistantInstructionsCache get(AssistantState assistantState) {
        return UserDataKeys.getUserDataSync(assistantState, ASSISTANT_INSTRUCTIONS_CACHE, () -> new AssistantInstructionsCache(assistantState));
    }

    @Override
    public String apply(Object memoryId) {
        if (memoryId instanceof AssistantMemoryId) {
            AssistantMemoryId memId = (AssistantMemoryId) memoryId;
            if (memId.isStateless()) return null;

            return entries.computeIfAbsent(memId, k -> createSystemMessage(k));
        }
        return null;
    }

    private String createSystemMessage(AssistantMemoryId memoryId) {
        AssistantState assistantState = getAssistantState();
        ConnectionHandler connection = assistantState.getConnection();
        Project project = connection.getProject();

        String resourceName = isCompact(memoryId) ? "system_message_compact.md.ft" : "system_message.md.ft";
        String content = TextResources.get(this, resourceName);
        TextContent textContent = TextContent.markdown(content);
        textContent.initField("ASSISTANT_TOOL_CATEGORIES", getToolCategories());
        textContent.initField("ASSISTANT_TOOL_TYPES", getToolTypes());
        textContent.initField("DATABASE_TYPE", connection.getDatabaseType().getName());
        textContent.initField("DATABASE_NAME", connection.getName());

        String profileId = assistantState.getCurrentContext().getProfileId();

        AssistantProfile profile = getProfile(project, profileId);
        String userInstructions = profile == null ? "" : profile.getInstructions();

        textContent.initField("USER_INSTRUCTIONS", userInstructions);

        return textContent.getText();
    }

    private boolean isCompact(AssistantMemoryId memoryId) {
        String chatId = memoryId.getChatId();
        Chat chat = getAssistantState().getChat(chatId);
        if (chat == null) return false;

        AIModel model = chat.getContext().getModel();
        if (model == null) return false;

        AIProviderId providerId = model.getProviderId();
        AIProviderId baseProviderId = model.getBaseProviderId();
        // TODO quick workaround for cohere 4k limits - implement token metrics and limits (config and model definitions)
        return providerId == AIProviderId.OCI_GEN_AI && baseProviderId == AIProviderId.COHERE;
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
