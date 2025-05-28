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

import com.dbn.assistant.chat.ChatConversation;
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
import java.util.Comparator;
import java.util.List;

/**
 * Action for selecting an old conversation
 */
public class ConversationHistoryDropdownAction extends BasicActionGroup implements DumbAware {
    private static final int MAX_SIZE = 5;

    @Override
    protected @NotNull AnAction[] loadChildren(AnActionEvent e) {
        ChatBoxForm chatBox = e.getData(DataKeys.ASSISTANT_CHAT_BOX);
        if (chatBox == null) return AnAction.EMPTY_ARRAY;

        List<ChatConversation> conversations = chatBox.getAssistantState().getSavedConversations();
        if (conversations.isEmpty()) return AnAction.EMPTY_ARRAY;

        List<AnAction> actionList = new ArrayList<>();

        // show most recent conversations first
        conversations.
                stream().
                sorted(Comparator.comparingLong(c -> System.currentTimeMillis() - c.getTimestamp())).
                limit(MAX_SIZE).
                forEach(c -> actionList.add(new ConversationSelectAction(c)));

        if (conversations.size() > MAX_SIZE) {
            actionList.add(Separator.create());
            actionList.add(new ConversationHistoryShowAllAction(conversations));
        }

        return actionList.toArray(new AnAction[0]);
    }
    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setDescription("Select a conversation");
    }
}
