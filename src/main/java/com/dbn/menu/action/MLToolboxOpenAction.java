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

package com.dbn.menu.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.action.AbstractConnectionAction;
import com.dbn.ml.DatabaseMLManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.common.ui.util.Popups.popupBuilder;
import static com.dbn.common.util.Actions.adjustActionName;
import static com.dbn.common.util.Lists.convert;

public class MLToolboxOpenAction extends ProjectAction {
    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText("Open ML Toolbox...");
        presentation.setIcon(Icons.DBO_AI_MODEL);
        presentation.setVisible(true);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
        List<ConnectionHandler> connections = connectionBundle.getConnections();

        if (connections.isEmpty()) {
            Messages.showWarningDialog(project, "No Connections", 
                "No database connections are configured. Please create a connection first.");
            return;
        }

        if (connections.size() == 1) {
            openMLToolbox(connections.get(0));
            return;
        }

        List<SelectConnectionAction> actions = convert(connections, c -> new SelectConnectionAction(c));

        popupBuilder(actions, e).
                withTitle("Select ML Toolbox Connection").
                withSpeedSearch().
                buildAndShowCentered();
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
            openMLToolbox(connection);
        }
    }

    private static void openMLToolbox(ConnectionHandler connection) {
        Project project = connection.getProject();
        DatabaseMLManager mlManager = DatabaseMLManager.getInstance(project);
        mlManager.openToolbox(connection);
    }
}
