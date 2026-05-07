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

package com.dbn.assistant.service.generic.action;

import com.dbn.assistant.AssistantMode;
import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.window.action.AssistantActionSupport;
import com.dbn.assistant.mcp.AssistantMcpServerSettings;
import com.dbn.assistant.mcp.model.AssistantMcpServer;
import com.dbn.assistant.mcp.model.AssistantMcpServerBundle;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.ComboBoxAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions.ActionText;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

import java.util.List;

import static com.dbn.assistant.chat.ChatAvailability.AVAILABLE;

@BackgroundUpdate
public class McpServerSelectionAction extends ComboBoxAction implements AssistantActionSupport {

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(@NotNull JComponent component, @NotNull DataContext dataContext) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        AssistantState assistantState = getAssistantState(dataContext);
        if (assistantState == null) return actionGroup;

        AssistantMcpServerBundle mcpServerBundle = getMcpServers(assistantState);
        AssistantMcpServer ideMcpServer = mcpServerBundle.getIdeMcpServer();
        if (ideMcpServer != null) {
            actionGroup.add(new McpServerSelectionToggleAction(ideMcpServer));
            actionGroup.addSeparator();
        }

        List<AssistantMcpServer> mcpServers = mcpServerBundle.getElements();
        for (AssistantMcpServer mcpServer : mcpServers) {
            actionGroup.add(new McpServerSelectionToggleAction(mcpServer));
        }

        actionGroup.addSeparator();
        if (mcpServers.isEmpty()) {
            actionGroup.add(new McpServerCreateAction());
        }
        actionGroup.add(new AssistantSettingsAction());

        return actionGroup;
    }

    private static AssistantMcpServerBundle getMcpServers(AssistantState assistantState) {
        Project project = assistantState.getProject();
        AssistantSettings assistantSettings = AssistantSettings.getInstance(project);
        AssistantMcpServerSettings mcpServerSettings = assistantSettings.getMcpServerSettings();
        return mcpServerSettings.getMcpServers();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setText(getText(e));
        presentation.setEnabled(isEnabled(e));
        presentation.setVisible(isVisible(e));
    }

    private @ActionText String getText(@NotNull AnActionEvent e) {
        AssistantState assistantState = getAssistantState(e);
        if (assistantState == null) return "MCP Servers";

        AssistantMcpServerBundle mcpServers = getMcpServers(assistantState);
        int available = mcpServers.size();
        int selected = assistantState.getMcpServerState().countSelected();

        return "MCP Servers (" + selected + "/" + available + ")";
    }

    private boolean isEnabled(@NotNull AnActionEvent e) {
        ChatAvailability availability = getChatAvailability(e);
        return availability == AVAILABLE;
    }

    private boolean isVisible(@NotNull AnActionEvent e) {
        ChatContext chatContext = getCurrentChatContext(e);
        if (chatContext == null) return false;

        return chatContext.getAssistantMode() != AssistantMode.RAG;
    }
}
