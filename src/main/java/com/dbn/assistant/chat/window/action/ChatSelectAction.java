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

import com.dbn.assistant.chat.Chat;
import com.dbn.assistant.chat.ChatContext;
import com.dbn.assistant.chat.ChatContextEvent;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.common.util.Actions;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Action for selecting one individual chat
 */
public class ChatSelectAction extends AbstractChatBoxAction {
    private final Chat chat;
    ChatSelectAction(Chat chat) {
        this.chat = chat;
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return;

        ChatContext currentContext = chatBox.getAssistantState().getCurrentContext();
        ChatContext targetContext = chat.getContext();
        ChatContextEvent event = new ChatContextEvent(currentContext, targetContext, chat.getId(), false);
        chatBox.processContextEvent(event);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(Actions.adjustActionName(chat.getTitle()));
    }
}
