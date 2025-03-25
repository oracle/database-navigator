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
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.common.action.ComboBoxAction;
import com.dbn.common.action.DataKeys;
import com.dbn.common.util.Actions;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.List;

/**
 * Action for selecting an old conversation
 */
public class ConversationSelectDropdownAction extends ComboBoxAction implements DumbAware {

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(JComponent component, DataContext dataContext) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        ChatBoxForm chatBox = dataContext.getData(DataKeys.ASSISTANT_CHAT_BOX);
        if (chatBox == null) return actionGroup;

        List<PersistentChatConversation> conversations = chatBox.getConversations();
        conversations.forEach(c -> actionGroup.add(new ConversationSelectAction(c)));
        actionGroup.addSeparator();

        actionGroup.add(new ConversationShowAllAction());
        return actionGroup;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        ChatBoxForm chatBox = e.getData(DataKeys.ASSISTANT_CHAT_BOX);

        Presentation presentation = e.getPresentation();
        presentation.setText(getText(e));
        presentation.setDescription("Select a conversation");
        //TODO when will it be enabled
//        presentation.setEnabled(enabled);
    }

    private String getText(@NotNull AnActionEvent e) {
        ChatBoxForm chatBox = e.getData(DataKeys.ASSISTANT_CHAT_BOX);
        if (chatBox == null) return "Conversation";

        String text = getSelectedConversationName(e);
        if (text != null){
            if(text.isEmpty()) return "Default";
            return text;
        }

        List<PersistentChatConversation> conversations = chatBox.getConversations();
        if (!conversations.isEmpty()) return "Conversation";

        return "Conversation";
    }

    @Nullable
    private static String getSelectedConversationName(@NotNull AnActionEvent e) {
        PersistentChatConversation conversation = getSelectedConversation(e);
        if (conversation == null) return null;

        return Actions.adjustActionName(conversation.getTitle());
    }

    @Nullable
    private static PersistentChatConversation getSelectedConversation(@NotNull AnActionEvent e) {
        ChatBoxForm chatBox = e.getData(DataKeys.ASSISTANT_CHAT_BOX);
        if (chatBox == null) return null;

        return chatBox.getConversations().isEmpty() ? null : chatBox.getConversations().get(0);
    }

    @Override
    protected boolean shouldShowDisabledActions() {
        return true;
    }
}
