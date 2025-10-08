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

package com.dbn.assistant.service.generic.action;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.context.ChatContextImpl;
import com.dbn.assistant.chat.window.action.AbstractChatBoxAction;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProviderId;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ModelSelectAction extends AbstractChatBoxAction {
    private final AIModel model;

    ModelSelectAction(AIModel model) {
        this.model = model;
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return;

        AssistantType assistantType = chatBox.getAssistantType();
        ChatContext currentContext = chatBox.getCurrentContext();
        AIProviderId providerId = currentContext.getProviderId();

        DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
        assistantManager.getSelectionState().setSelectedModel(assistantType, providerId, model);

        // preserve profile and action from the current context
        ChatContext targetContext = new ChatContextImpl(
                assistantType,
                currentContext.getProfileId(),
                providerId,
                model.getId(),
                currentContext.getActionId(),
                currentContext.isInteractive());

        chatBox.attemptContextSwitch(targetContext);
    }

    public String getModelId() {
        return model.getId();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(model.getName(), false);
    }
}
