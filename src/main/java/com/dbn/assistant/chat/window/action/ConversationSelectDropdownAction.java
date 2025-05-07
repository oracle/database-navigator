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
import com.dbn.common.action.BasicActionGroup;
import com.dbn.common.action.DataKeys;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Action for selecting an old conversation
 */
public class ConversationSelectDropdownAction extends BasicActionGroup implements DumbAware {
    @Override
    protected @NotNull AnAction[] loadChildren(AnActionEvent e) {
        ChatBoxForm chatBox = e.getData(DataKeys.ASSISTANT_CHAT_BOX);
        if (chatBox == null) return new AnAction[0];

        List<PersistentChatConversation> conversations = chatBox.getConversations().stream().filter(conv -> conv.getTitle() != null && !conv.getTitle().isEmpty()).collect(Collectors.toList());
        if (conversations.isEmpty()) return new AnAction[0];

        List<AnAction> actionList = new ArrayList<>();

        conversations.stream()
                .limit(3)
                .forEach(c -> actionList.add(new ConversationSelectAction(c)));

        actionList.add(Separator.create());

        actionList.add(new ConversationShowAllAction(conversations));

        return actionList.toArray(new AnAction[0]);
    }
    @Override
    public void update(@NotNull AnActionEvent e) {
        ChatBoxForm chatBox = e.getData(DataKeys.ASSISTANT_CHAT_BOX);

        Presentation presentation = e.getPresentation();
        presentation.setDescription("Select a conversation");
        //TODO when will it be enabled
        presentation.setEnabled(true);
    }
}
