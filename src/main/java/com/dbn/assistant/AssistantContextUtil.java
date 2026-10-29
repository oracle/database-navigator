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

package com.dbn.assistant;

import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.profile.AssistantProfile;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.assistant.provider.AIProviderId;
import com.dbn.assistant.state.AssistantState;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import static com.dbn.assistant.profile.AssistantProfileLookup.getProfile;

public class AssistantContextUtil {
    @Nullable
    private static ConnectionHandler getConnection(ConnectionId connectionId) {
        return ConnectionHandler.get(connectionId);
    }

    @Nullable
    public static AssistantState getAssistantState(ConnectionId connectionId, AssistantType assistantType) {
        ConnectionHandler connection = getConnection(connectionId);
        if (connection == null) return null;

        Project project = connection.getProject();
        DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
        return assistantManager.getAssistantState(connectionId, assistantType);
    }

    @Nullable
    public static ChatContext getChatContext(ConnectionId connectionId, AssistantType assistantType) {
        AssistantState assistantState = getAssistantState(connectionId, assistantType);
        if (assistantState == null) return null;

        ChatContext chatContext = assistantState.getCurrentContext();
        if (assistantType == AssistantType.PUBLIC) {
            normalizeGenericChatContext(connectionId, assistantType, chatContext);
        }
        return chatContext;
    }

    private static void normalizeGenericChatContext(ConnectionId connectionId, AssistantType assistantType, ChatContext chatContext) {
        if (chatContext == null) return;

        ConnectionHandler connection = getConnection(connectionId);
        if (connection == null) return;

        Project project = connection.getProject();
        AssistantProfile profile = getProfile(project, chatContext.getProfileId());
        if (profile == null) return;

        AIProviderId profileProviderId = profile.getProviderId();
        AIProvider provider = AIProviderData.getProvider(assistantType, profileProviderId);
        if (provider == null) return;

        if (profileProviderId == chatContext.getProviderId() && chatContext.getModel() != null) {
            return;
        }

        DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
        AIModel model = assistantManager.getSelectionState().getSelectedModel(assistantType, profileProviderId);
        if (model == null || model.getProviderId() != profileProviderId) {
            model = provider.getDefaultModel();
            if (model != null) {
                assistantManager.getSelectionState().setSelectedModel(assistantType, profileProviderId, model);
            }
        }
        if (model == null) return;

        // Restore the profile's provider-specific model selection when saved context values are stale.
        chatContext.setProviderId(profileProviderId);
        chatContext.setModelId(model.getId());
        assistantManager.notifyConfigChanges();
    }
}
