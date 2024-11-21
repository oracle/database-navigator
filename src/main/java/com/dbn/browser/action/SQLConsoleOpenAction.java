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

package com.dbn.browser.action;

import com.dbn.browser.DatabaseBrowserManager;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectPopupAction;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.action.AbstractConnectionAction;
import com.dbn.connection.console.DatabaseConsoleManager;
import com.dbn.database.DatabaseFeature;
import com.dbn.object.DBConsole;
import com.dbn.vfs.DBConsoleType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SQLConsoleOpenAction extends ProjectPopupAction {
    private static ConnectionHandler getConnection(@NotNull AnActionEvent e) {
        Project project = Lookups.getProject(e);
        if (project != null) {
            DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(project);
            return browserManager.getActiveConnection();
        }
        return null;
    }

    @Override
    public void update(@NotNull AnActionEvent e, Project project) {
        Presentation presentation = e.getPresentation();
        ConnectionHandler connection = getConnection(e);
        presentation.setEnabled(connection != null);
        presentation.setText("Open SQL Console");
        presentation.setIcon(Icons.SQL_CONSOLE);
    }

    @Override
    public AnAction[] getChildren(AnActionEvent e) {
        ConnectionHandler connection = getConnection(e);
        List<AnAction> actions = new ArrayList<>();
        if (connection != null) {
            Collection<DBConsole> consoles = connection.getConsoleBundle().getConsoles();
            for (DBConsole console : consoles) {
                actions.add(new SelectConsoleAction(console));
            }
            actions.add(Separator.getInstance());
            actions.add(new SelectConsoleAction(connection, DBConsoleType.STANDARD));
            if (DatabaseFeature.DEBUGGING.isSupported(connection)) {
                actions.add(new SelectConsoleAction(connection, DBConsoleType.DEBUG));
            }
        }
        return actions.toArray(new AnAction[0]);
    }


    private static class SelectConsoleAction extends AbstractConnectionAction{
        private DBConsole console;
        private DBConsoleType consoleType;

        SelectConsoleAction(@NotNull ConnectionHandler connection, @NotNull DBConsoleType consoleType) {
            super(connection);
            this.consoleType = consoleType;
        }

        SelectConsoleAction(DBConsole console) {
            super(console.getConnection());
            this.console = console;
        }

        @Override
        protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable ConnectionHandler target) {
            if (console == null) {
                presentation.setText("New " + consoleType.getName() + "...");
            } else {
                presentation.setText(Actions.adjustActionName(console.getName()));
                presentation.setIcon(console.getIcon());
            }
        }

        @Override
        protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull ConnectionHandler connection) {
            DBConsole console = this.console;
            if (console == null) {
                DatabaseConsoleManager databaseConsoleManager = DatabaseConsoleManager.getInstance(project);
                databaseConsoleManager.showCreateConsoleDialog(connection, consoleType);
            } else {
                Editors.openFileEditor(project, console.getVirtualFile(), true);
            }
        }
    }
}
