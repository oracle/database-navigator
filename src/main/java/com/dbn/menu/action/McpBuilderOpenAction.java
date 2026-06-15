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

package com.dbn.menu.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.action.AbstractConnectionAction;
import com.dbn.mcp.McpServerBuilderManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.common.ui.util.Popups.popupBuilder;
import static com.dbn.common.util.Actions.adjustActionName;
import static com.dbn.common.util.Lists.convert;
import static com.dbn.database.DatabaseFeature.MCP_SERVER_BUILDER;
import static com.dbn.nls.NlsResources.txt;

public class McpBuilderOpenAction extends ProjectAction {

    public McpBuilderOpenAction() {
        super(txt("app.menu.action.OpenMcpServerBuilder"));
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.menu.action.OpenMcpServerBuilder"));
        presentation.setVisible(isVisible(project));
    }

    private boolean isVisible(@NotNull Project project) {
        return MCP_SERVER_BUILDER.isSupported(project);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        List<ConnectionHandler> connections = connectionManager.getConnections(MCP_SERVER_BUILDER);

        if (connections.size() == 1) {
            openMcpBuilder(connections.get(0));
            return;
        }

        List<SelectConnectionAction> actions = convert(connections, SelectConnectionAction::new);
        popupBuilder(actions, e)
                .withTitle(txt("msg.mcp.title.SelectMcpServerConnection"))
                .withSpeedSearch()
                .buildAndShowCentered();
    }

    private static class SelectConnectionAction extends AbstractConnectionAction {
        SelectConnectionAction(ConnectionHandler connection) {
            super(connection);
        }

        @Override
        protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable ConnectionHandler target) {
            ConnectionHandler connection = getConnection();
            if (connection == null) return;
            presentation.setText(adjustActionName(connection.getName()));
            presentation.setIcon(connection.getIcon());
        }

        @Override
        protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull ConnectionHandler connection) {
            openMcpBuilder(connection);
        }
    }

    private static void openMcpBuilder(ConnectionHandler connection) {
        Project project = connection.getProject();
        McpServerBuilderManager builderManager = McpServerBuilderManager.getInstance(project);
        builderManager.openMCPBuilder(connection);
    }
}
