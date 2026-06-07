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

package com.dbn.assistant.service.selectai.action;

import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.window.action.AbstractChatBoxAction;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.service.selectai.ui.SelectAiContextActionsForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.assistant.chat.ChatAvailability.AVAILABLE;
import static com.dbn.assistant.chat.ChatAvailability.DISABLED_PROFILE_SELECTED;
import static com.dbn.assistant.chat.ChatAvailability.NOT_INITIALIZED;
import static com.dbn.assistant.chat.ChatAvailability.NO_PROFILE_AVAILABLE;
import static com.dbn.assistant.chat.ChatAvailability.NO_PROFILE_SELECTED;
import static com.dbn.nls.NlsResources.txt;

/**
 * Action for refreshing (reloading) the AI-assistant profiles
 *
 * @author Dan Cioca (Oracle)
 */
public class ProfilesRefreshAction extends AbstractChatBoxAction {
    public ProfilesRefreshAction() {
        super(txt("app.assistant.action.AssistantReloadProfiles"));
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return;

        SelectAiContextActionsForm contextActionsForm = chatBox.getContextActionsForm();
        contextActionsForm.reloadProfiles();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        boolean enabled = isEnabled(e);

        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.assistant.action.ReloadProfiles"));
        presentation.setEnabled(enabled);
    }

    private boolean isEnabled(@NotNull AnActionEvent e) {
        ChatAvailability availability = getChatAvailability(e);
        return availability.isOneOf(
                AVAILABLE,
                NOT_INITIALIZED,
                NO_PROFILE_AVAILABLE,
                NO_PROFILE_SELECTED,
                DISABLED_PROFILE_SELECTED);
    }
}
