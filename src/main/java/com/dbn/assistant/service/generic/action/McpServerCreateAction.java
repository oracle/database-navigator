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
import com.dbn.assistant.mcp.AssistantMcpServerOptions;
import com.dbn.assistant.mcp.AssistantMcpServerSettings;
import com.dbn.assistant.mcp.ui.AssistantMcpServerEditDialog;
import com.dbn.assistant.mcp.ui.AssistantMcpServerEditRequest;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.util.Dialogs;
import com.dbn.options.ProjectSettings;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

@Setter
public class McpServerCreateAction extends AbstractChatBoxAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        AssistantState assistantState = getAssistantState(e);
        if (assistantState == null) return;

        AssistantMcpServerOptions serverOptions = assistantState.getMcpServerOptions();

        ProjectSettings projectSettings = ProjectSettings.get(project);
        AssistantMcpServerSettings serverSettings = projectSettings.getAssistantSettings().getMcpServerSettings();

        AssistantMcpServerEditRequest request = AssistantMcpServerEditRequest
                .builder()
                .mcpServers(serverSettings.getMcpServers())
                .saveConsumer(s -> {
                    serverSettings.getMcpServers().addMcpServer(s);
                    serverOptions.setSelected(s.getId(), true);
                })
                .build();

        Dialogs.show(() -> new AssistantMcpServerEditDialog(project, request));
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.assistant.action.NewMcpServer"));
        presentation.setIcon(AllIcons.General.Add);
    }
}
