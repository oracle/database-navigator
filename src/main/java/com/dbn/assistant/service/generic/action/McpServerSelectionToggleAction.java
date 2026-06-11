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

import com.dbn.assistant.chat.window.action.AssistantActionSupport;
import com.dbn.assistant.mcp.AssistantMcpServerState;
import com.dbn.assistant.mcp.model.AssistantMcpServer;
import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.ToggleAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static com.dbn.common.icon.Icons.ACTION_CHECK;
import static com.dbn.common.util.Strings.truncateWithMiddleEllipsis;

@BackgroundUpdate
public class McpServerSelectionToggleAction extends ToggleAction implements AssistantActionSupport {
    private final AssistantMcpServer mcpServer;

    public McpServerSelectionToggleAction(AssistantMcpServer mcpServer) {
        this.mcpServer = mcpServer;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();

        Icon icon = isSelected(e) ? ACTION_CHECK : null;

        String name = mcpServer.getName();
        String endpoint = truncateWithMiddleEllipsis(mcpServer.getEndpoint(), 60);
        @NonNls String description = "<strong>" + name + "</strong><br>" + endpoint;

        presentation.setIcon(icon);
        presentation.setText(name);
        presentation.setDescription(description);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        AssistantMcpServerState mcpServerState = getMcpServerState(e);
        if (mcpServerState == null) return false;

        return mcpServerState.isSelected(mcpServer.getId());
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean selected) {
        AssistantMcpServerState mcpServerState = getMcpServerState(e);
        if (mcpServerState == null) return;

        mcpServerState.setSelected(mcpServer.getId(), selected);
    }
}
