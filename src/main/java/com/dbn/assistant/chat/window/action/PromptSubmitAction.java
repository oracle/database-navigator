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

import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.ChatContext;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.common.icon.Icons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static com.dbn.assistant.chat.ChatAvailability.AVAILABLE;
import static com.dbn.nls.NlsResources.txt;

/**
 * Action for sending the user prompt content to the AI-assistant engine
 *
 * @author Dan Cioca (Oracle)
 */
public class PromptSubmitAction extends AbstractChatBoxAction {
    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return;

        chatBox.submitPrompt();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        boolean enabled = isEnabled(e);
        Icon icon = getIcon(e);

        Presentation presentation = e.getPresentation();
        presentation.setIcon(icon);
        presentation.setText(txt("app.assistant.action.SubmitPrompt"));
        presentation.setEnabled(enabled);
    }

    private boolean isEnabled(@NotNull AnActionEvent e) {
        ChatAvailability availability = getChatAvailability(e);
        return availability == AVAILABLE;
    }

    private Icon getIcon(@NotNull AnActionEvent e) {
        ChatContext context = getCurrentChatContext(e);
        return context == null || context.isInteractive() ?
                Icons.ASSISTANT_PROMPT_INTERACTIVE :
                Icons.ASSISTANT_PROMPT_NON_INTERACTIVE;
    }
}
