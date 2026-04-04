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

package com.dbn.assistant.chat.window.action;

import com.dbn.assistant.chat.Chat;
import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.mcp.AssistantMcpServerData;
import com.dbn.assistant.profile.AssistantProfile;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.assistant.tool.config.AssistantToolSettings;
import com.dbn.common.action.DataKeys;
import com.dbn.common.ui.component.DBNDiscardableComponent;
import com.dbn.common.ui.component.DBNFoldableComponent;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.assistant.profile.AssistantProfileLookup.getProfile;

public interface AssistantActionSupport {

    default ChatBoxForm getChatBox(DataContext dataContext) {
        return dataContext.getData(DataKeys.ASSISTANT_CHAT_BOX);
    }

    @Nullable
    default ChatBoxForm getChatBox(@NotNull AnActionEvent e) {
        return getChatBox(e.getDataContext());
    }

    default Project getProject(DataContext dataContext) {
        return dataContext.getData(PlatformDataKeys.PROJECT);
    }

    @Nullable
    default ConnectionId getConnectionId(@NotNull AnActionEvent e) {
        ChatBoxForm chatBox = getChatBox(e);
        return chatBox == null ? null : chatBox.getConnectionId();
    }

    @Nullable
    default AssistantState getAssistantState(@NotNull DataContext dataContext) {
        ChatBoxForm chatBox = getChatBox(dataContext);
        return chatBox == null ? null : chatBox.getAssistantState();
    }

    @Nullable
    default AssistantState getAssistantState(@NotNull AnActionEvent e) {
        return getAssistantState(e.getDataContext());
    }

    default ChatAvailability getChatAvailability(@NotNull AnActionEvent e) {
        AssistantState state = getAssistantState(e);
        return state == null ?
                ChatAvailability.NOT_INITIALIZED :
                state.getChatAvailability();
    }

    @Nullable
    default Chat getCurrentChat(@NotNull DataContext dataContext) {
        AssistantState state = getAssistantState(dataContext);
        return state == null ? null : state.getCurrentChat();
    }

    @Nullable
    default Chat getCurrentChat(@NotNull AnActionEvent e) {
        return getCurrentChat(e.getDataContext());
    }


    @Nullable
    default ChatContext getCurrentChatContext(@NotNull DataContext dataContext) {
        Chat chat = getCurrentChat(dataContext);
        return chat == null ? null : chat.getContext();
    }
    @Nullable
    default ChatContext getCurrentChatContext(@NotNull AnActionEvent e) {
        return getCurrentChatContext(e.getDataContext());
    }

    @Nullable
    default AssistantToolApprovals getToolApprovals(@NotNull AnActionEvent e) {
        AssistantToolSettings toolSettings = getToolSettings(e);
        if (toolSettings == null) return null;

        return toolSettings.getApprovals();
    }

    @Nullable
    default AssistantMcpServerData getMcpServerData(@NotNull AnActionEvent e) {
        AssistantState assistantState = getAssistantState(e);
        if (assistantState == null) return null;

        return assistantState.getMcpServerData();
    }

    @Nullable
    default AssistantToolSettings getToolSettings(@NotNull AnActionEvent e) {
        AssistantState assistantState = getAssistantState(e);
        if (assistantState == null) return null;

        return assistantState.getToolSettings();
    }

    @Nullable
    default AssistantProfile getSelectedProfile(@NotNull AnActionEvent e) {
        ChatContext chatContext = getCurrentChatContext(e);
        if (chatContext == null) return null;

        Project project = e.getProject();
        if (project == null) return null;

        String profileId = chatContext.getProfileId();
        return getProfile(project, profileId);
    }

    @Nullable
    default String getSelectedProfileName(@NotNull AnActionEvent e) {
        AssistantProfile profile = getSelectedProfile(e);
        if (profile == null) return null;

        return profile.getName();
    }

    default String getSelectedModelName(@NotNull AnActionEvent e) {
        AIModel model = getSelectedModel(e);
        if (model == null) return null;

        return model.getName();
    }

    default @Nullable AIModel getSelectedModel(@NotNull AnActionEvent e) {
        ChatContext chatContext = getCurrentChatContext(e);
        if (chatContext == null) return null;

        AssistantProfile profile = getSelectedProfile(e);
        if (profile == null) return null;

        return chatContext.getModel();
    }

    @Nullable
    default DBNFoldableComponent getFoldableComponent(@NotNull AnActionEvent e) {
        return e.getDataContext().getData(DataKeys.FOLDABLE_COMPONENT);
    }

    @Nullable
    default DBNDiscardableComponent getDiscardableComponent(@NotNull AnActionEvent e) {
        return e.getDataContext().getData(DataKeys.DISCARDABLE_COMPONENT);
    }
}
