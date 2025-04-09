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

import com.dbn.assistant.chat.PersistentChatConversation;
import com.dbn.assistant.chat.ui.ConversationHistoryDialog;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.common.util.Dialogs;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

/**
 * Action for showcasing the rest of the conversations
 */
public class ConversationShowAllAction extends AbstractChatBoxAction {
    private final List<PersistentChatConversation> conversations;

    public ConversationShowAllAction(List<PersistentChatConversation> conversations) {
        this.conversations = conversations;
    }
    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return;
        Consumer<PersistentChatConversation> openAction = (PersistentChatConversation conversation) -> {
            chatBox.triggerContextChangeEvent(chatBox.getAssistantState().getChatContext(), conversation.getContext(), conversation);
        };
        Consumer<List<PersistentChatConversation>> deleteAction = (List<PersistentChatConversation> conversations) -> {
            chatBox.getAssistantState().getConversations().removeAll(conversations);
        };
        Dialogs.show(()-> new ConversationHistoryDialog(project, conversations, openAction, deleteAction));
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText("Others ...");
    }
}
