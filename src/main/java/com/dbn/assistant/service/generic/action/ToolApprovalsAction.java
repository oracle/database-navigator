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

import com.dbn.assistant.chat.window.action.AbstractChatBoxAction;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.config.AssistantToolSettings;
import com.dbn.assistant.tool.config.ui.AssistantToolApprovalDialog;
import com.dbn.common.util.Dialogs;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

@Setter
public class ToolApprovalsAction extends AbstractChatBoxAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return;

        AssistantState assistantState = getAssistantState(e);
        if (assistantState == null) return;

        AssistantToolSettings settings = AssistantToolSettings.get(assistantState);
        Dialogs.show(() -> new AssistantToolApprovalDialog(project, settings));
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.assistant.action.ToolSettings"));
        presentation.setIcon(AllIcons.General.GearPlain);
    }
}
