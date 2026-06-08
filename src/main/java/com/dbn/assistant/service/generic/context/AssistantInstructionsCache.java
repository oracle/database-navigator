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

import com.dbn.assistant.AssistantMode;
import com.dbn.assistant.profile.AssistantProfile;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateExtension;
import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolCache;
import com.dbn.assistant.tool.AssistantToolCategory;
import com.dbn.common.action.UserDataKeys;
import com.dbn.common.text.TextContent;
import com.dbn.common.text.TextResources;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dbn.assistant.profile.AssistantProfileLookup.getProfile;
import static com.dbn.common.action.UserDataKeys.ASSISTANT_INSTRUCTIONS_CACHE;

public class AssistantInstructionsCache extends AssistantStateExtension implements Function<Object, String> {
    private final Map<AssistantMemoryId, SystemMessage> entries = new ConcurrentHashMap<>();

    private AssistantInstructionsCache(@NotNull AssistantState assistantState) {
        super(assistantState);
    }

    public static AssistantInstructionsCache get(AssistantState assistantState) {
        return UserDataKeys.getUserDataSync(assistantState, ASSISTANT_INSTRUCTIONS_CACHE, () -> new AssistantInstructionsCache(assistantState));
    }

    @Override
    public String apply(Object memoryId) {
        if (memoryId instanceof AssistantMemoryId memId) {
            if (memId.isStateless()) return null;

            SystemMessage message = entries.compute(memId, (i, m) -> resolveSystemMessage(m));
            return message.text;
        }
        return null;
    }

    private SystemMessage resolveSystemMessage(SystemMessage message) {
        int signature = getStateSignature();
        if (message == null || message.stateSignature != signature) {
            String systemMessage = createSystemMessage();
            return new SystemMessage(systemMessage, signature);
        }
        return message;
    }

    private int getStateSignature() {
        // consider all state attributes that may alter the system message
        // - operating mode
        // - tool approvals
        // - profile user instructions
        // ...

        AssistantState assistantState = getAssistantState();
        AssistantMode assistantMode = assistantState.getAssistantMode();
        int toolsSignature = assistantState.getToolApprovals().getSignature();
        String userInstructions = getUserInstructions();

        // TODO do we need logic with lower collision potential here?
        return Objects.hash(assistantMode, toolsSignature, userInstructions);
    }

    private String createSystemMessage() {
        AssistantState assistantState = getAssistantState();
        AssistantMode assistantMode = assistantState.getAssistantMode();
        ConnectionHandler connection = assistantState.getConnection();

        String resourceName = "system_message_" + assistantMode + ".md.ft";
        String content = TextResources.get(this, resourceName);
        TextContent textContent = TextContent.markdown(content);
        textContent.initField("ASSISTANT_TOOL_CATEGORIES", getToolCategories());
        textContent.initField("ASSISTANT_TOOL_TYPES", getToolTypes());
        textContent.initField("DATABASE_TYPE", connection.getDatabaseType().getName());
        textContent.initField("DATABASE_NAME", connection.getName());

        @NonNls
        String userInstructions = getUserInstructions();
        if (Strings.isEmpty(userInstructions)) userInstructions = "(none)";

        textContent.initField("USER_INSTRUCTIONS", userInstructions);

        return textContent.getText();
    }

    @NonNls
    private String getUserInstructions() {
        AssistantState assistantState = getAssistantState();
        String profileId = assistantState.getCurrentContext().getProfileId();

        Project project = assistantState.getProject();
        AssistantProfile profile = getProfile(project, profileId);
        return profile == null ? "" : profile.getInstructions();
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

    private record SystemMessage(String text, int stateSignature) {}
}
