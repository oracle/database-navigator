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

import com.dbn.assistant.chat.ChatContext;
import com.dbn.assistant.chat.window.ContextChangeEvent;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.common.feature.FeatureAvailability;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Action for starting a new conversation
 */
public class NewConversationAction extends AbstractChatBoxAction {
    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return;
        if(!chatBox.getAssistantState().getCurrentConversation().getContext().isActive()){
            getAssistantState(e).setAvailability(FeatureAvailability.AVAILABLE);
        }
        ChatContext oldContext = chatBox.getAssistantState().getChatContext();
        ChatContext newContext = new ChatContext(oldContext.getProfile(), oldContext.getModel(), oldContext.getAction(), oldContext.isInteractive());
        ContextChangeEvent contextChangeEvent = new ContextChangeEvent(newContext, newContext, null, true, chatBox);
        contextChangeEvent.trigger();
//        chatBox.reloadProfiles();
    }
    //TODO
    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
    }
}
