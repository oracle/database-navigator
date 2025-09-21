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
import com.dbn.assistant.profile.AssistantProfile;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.action.DataKeys;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.common.util.Lists.first;

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


    default List<AssistantProfile> getAssistantProfiles(Project project) {
        AssistantSettings assistantSettings = AssistantSettings.getInstance(project);
        return assistantSettings.getProfileSettings().getProfiles().getElements();
    }

    default AssistantProfile getAssistantProfile(Project project, String profileName) {
        List<AssistantProfile> profiles = getAssistantProfiles(project);
        return first(profiles, p -> p.getName().equals(profileName));
    }

    @Nullable
    default String getSelectedProfileName(@NotNull AnActionEvent e) {
        ChatContext chatContext = getCurrentChatContext(e);
        if (chatContext == null) return null;

        return chatContext.getProfileName();
    }
}
