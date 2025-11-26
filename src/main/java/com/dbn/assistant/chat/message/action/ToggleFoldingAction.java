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

package com.dbn.assistant.chat.message.action;

import com.dbn.assistant.chat.message.ui.ChatMessageForm;
import com.dbn.assistant.chat.window.action.AssistantActionSupport;
import com.dbn.common.icon.Icons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

public class ToggleFoldingAction extends ChatMessageAction implements AssistantActionSupport {

    @Override
    public void update(@NotNull AnActionEvent e) {
        ChatMessageForm messageForm = getMessageForm(e);

        boolean enabled = messageForm != null;
        boolean folded = enabled && messageForm.getMessage().isFolded();

        Presentation presentation = e.getPresentation();
        presentation.setText(folded ? "Expand" : "Collapse");
        presentation.setIcon(folded ?
                Icons.ACTION_CONTENT_EXPAND :
                Icons.ACTION_CONTENT_COLLAPSE);
        presentation.setEnabled(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ChatMessageForm messageForm = getMessageForm(e);
        if (messageForm == null) return;

        messageForm.toggleContentFolding();
    }

}
