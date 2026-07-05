/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.assistant.tool.action;

import com.dbn.assistant.chat.message.ui.ChatMessageToolSectionForm;
import com.dbn.assistant.mcp.model.AssistantMcpServer;
import com.dbn.assistant.mcp.ui.AssistantMcpToolApprovalDialog;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.config.AssistantToolSettings;
import com.dbn.assistant.tool.config.ui.AssistantToolApprovalDialog;
import com.dbn.common.util.Dialogs;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

public class ToolApprovalSettingsAction extends AssistantToolAction {
    public ToolApprovalSettingsAction() {
        super(txt("app.assistant.action.AssistantToolSettings"));
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        AssistantState assistantState = getAssistantState(e);
        if (assistantState == null) return;

        ChatMessageToolSectionForm toolSectionForm = getToolSectionForm(e);
        if (toolSectionForm == null) return;

        if (toolSectionForm.isExternalTool())  {
            AssistantMcpServer mcpServer = toolSectionForm.getMcpServer();
            if (mcpServer == null) return;

            Dialogs.show(() -> new AssistantMcpToolApprovalDialog(project, mcpServer));
        } else {
            AssistantToolSettings settings = assistantState.getToolSettings();
            Dialogs.show(() -> new AssistantToolApprovalDialog(project, settings));
        }
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.assistant.action.ToolSettings"));
        presentation.setIcon(AllIcons.General.GearPlain);
        presentation.setVisible(!isInteractive(e));
    }
}
