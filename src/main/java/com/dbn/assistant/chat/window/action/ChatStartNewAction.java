/*
 * Copyright 2024 Oracle and/or its affiliates
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

import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.ChatContextEvent;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.compatibility.Compatibility;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Action for starting a new chat
 */
public class ChatStartNewAction extends AbstractChatBoxAction {
    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return;

        AssistantState assistantState = chatBox.getAssistantState();
        ChatContext currentContext = assistantState.getCurrentContext();
        ChatContextEvent event = chatBox.createContextEvent(
                currentContext,
                currentContext,
                null,
                true);

        chatBox.processContextEvent(event);
    }

    @Override
    @Compatibility
    public boolean displayTextInToolbar() {
        return true;
    }


    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        boolean enabled = isEnabled(e);

        e.getPresentation().setEnabled(enabled);
    }

    private boolean isEnabled(@NotNull AnActionEvent e) {
        ChatAvailability availability = getChatAvailability(e);

        return availability.isOneOf(
                ChatAvailability.AVAILABLE,
                ChatAvailability.INACTIVE_CHAT_SELECTED);
    }
}
