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

package com.dbn.assistant.selectai.action;

import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.context.ChatContextImpl;
import com.dbn.assistant.chat.window.action.AbstractChatBoxAction;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.object.DBAIProfile;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Action for selecting one individual AI-assistant profile
 *
 * @author Dan Cioca (Oracle)
 */
public class ProfileSelectAction extends AbstractChatBoxAction {
    private final DBObjectRef<DBAIProfile> profile;
    ProfileSelectAction(DBAIProfile profile) {
        this.profile = DBObjectRef.of(profile);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return;

        // preserve action from the current context
        DBAIProfile profile = getProfile();
        ChatContext currentContext = chatBox.getCurrentContext();
        ChatContext targetContext = new ChatContextImpl(
                profile.getName(),
                profile.getProviderId(),
                profile.getModelId(),
                currentContext.getActionId(),
                profile.isInteractive());

        chatBox.attemptContextSwitch(targetContext);
    }

    private DBAIProfile getProfile() {
        return DBObjectRef.ensure(profile);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        DBAIProfile profile = getProfile();

        Presentation presentation = e.getPresentation();
        presentation.setText(profile.getName(), false);
        presentation.setIcon(profile.getIcon());
    }
}
