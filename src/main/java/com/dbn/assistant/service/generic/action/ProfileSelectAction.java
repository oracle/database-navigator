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
import com.dbn.assistant.profile.AssistantProfile;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProviderId;
import com.dbn.assistant.state.AssistantSelectionState;
import com.dbn.common.ref.WeakRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.assistant.profile.AssistantProfileUtil.verifyAssistantProfile;

public class ProfileSelectAction extends AbstractChatBoxAction {
    private final WeakRef<AssistantProfile> profile;
    ProfileSelectAction(AssistantProfile profile) {
        this.profile = WeakRef.of(profile);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return;

        AssistantProfile profile = getProfile();
        verifyAssistantProfile(project, profile, p -> switchContext(e, project, p));
    }

    private void switchContext(@NotNull AnActionEvent e, @NotNull Project project, AssistantProfile profile) {
        if (profile == null) return;

        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return;

        AssistantType assistantType = chatBox.getAssistantType();
        DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
        AssistantSelectionState selectionState = assistantManager.getSelectionState();

        AIProviderId providerId = profile.getProviderId();
        AIModel model = selectionState.getSelectedModel(assistantType, providerId);
        String modelId = model == null ?  profile.getDefaultModelId() : model.getId();

        ChatContext currentContext = chatBox.getCurrentContext();
        ChatContext targetContext = new ChatContextImpl(
                currentContext.getAssistantType(),
                profile.getId(),
                providerId,
                modelId,
                currentContext.getActionId(),
                true);

        chatBox.attemptContextSwitch(targetContext);
    }

    private AssistantProfile getProfile() {
        return WeakRef.ensure(profile);
    }

    public String getProfileId() {
        return getProfile().getId();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        AssistantProfile profile = getProfile();

        Presentation presentation = e.getPresentation();
        presentation.setText(profile.getName(), false);
    }
}
