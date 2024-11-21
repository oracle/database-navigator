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
import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.action.AbstractConnectionAction;
import com.dbn.editor.session.SessionBrowserManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SessionBrowserOpenAction extends ProjectAction {
    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText("Open Session Browser...");
        presentation.setIcon(Icons.SESSION_BROWSER);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        //FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.file");
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
        List<ConnectionHandler> connections = connectionBundle.getConnections();
        if (connections.size() == 0) {
            connectionManager.promptMissingConnection();
            return;
        }

        if (connections.size() == 1) {
            openSessionBrowser(connections.get(0));
            return;
        }

        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.addSeparator();
        for (ConnectionHandler connection : connections) {
            actionGroup.add(new SelectConnectionAction(connection));
        }

        ListPopup popupBuilder = JBPopupFactory.getInstance().createActionGroupPopup(
                "Select Session Browser Connection",
                actionGroup,
                e.getDataContext(),
                //JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false,
                true,
                true,
                null,
                actionGroup.getChildrenCount(), null);

        popupBuilder.showCenteredInCurrentWindow(project);
    }

    private static class SelectConnectionAction extends AbstractConnectionAction{

        SelectConnectionAction(ConnectionHandler connection) {
            super(connection);
        }

        @Override
        protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable ConnectionHandler target) {
            ConnectionHandler connection = getConnection();
            if (connection == null) return;

            presentation.setText(connection.getName());
            presentation.setIcon(connection.getIcon());

        }

        @Override
        protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull ConnectionHandler connection) {
            openSessionBrowser(connection);
        }
    }

    private static void openSessionBrowser(ConnectionHandler connection) {
        SessionBrowserManager sessionBrowserManager = SessionBrowserManager.getInstance(connection.getProject());
        sessionBrowserManager.openSessionBrowser(connection);
    }
}
