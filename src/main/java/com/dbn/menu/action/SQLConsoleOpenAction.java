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

import com.dbn.common.action.BasicAction;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.util.Popups;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.console.DatabaseConsoleManager;
import com.dbn.database.DatabaseFeature;
import com.dbn.object.DBConsole;
import com.dbn.vfs.DBConsoleType;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.dbn.common.util.Lists.convert;
import static com.dbn.nls.NlsResources.txt;

public class SQLConsoleOpenAction extends ProjectAction {

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.menu.action.OpenSqlConsole"));
        presentation.setIcon(Icons.SQL_CONSOLE);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        //FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.file");
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
        List<ConnectionHandler> connections = connectionBundle.getConnections();
        if (connections.isEmpty()) {
            connectionManager.promptMissingConnection();
            return;
        }

        if (connections.size() == 1) {
            openSQLConsole(connections.get(0));
            return;
        }

        List<SelectConnectionAction> actions = convert(connections, c -> new SelectConnectionAction(c));
        ListPopup popup = Popups.popupBuilder(actions, e).
                withTitle("Select Console Connection").
                withSpeedSearch().
                build();

        popup.showCenteredInCurrentWindow(project);

    }

    private static class SelectConnectionAction extends ActionGroup {
        private final ConnectionRef connection;

        private SelectConnectionAction(ConnectionHandler connection) {
            super(connection.getName(), null, connection.getIcon());
            this.connection = ConnectionRef.of(connection);
            setPopup(true);
        }
/*
        @Override
        public void actionPerformed(AnActionEvent e) {
            openSQLConsole(connection);
            latestSelection = connection;
        }*/

        @NotNull
        @Override
        public AnAction[] getChildren(AnActionEvent e) {
            ConnectionHandler connection = this.connection.ensure();
            List<AnAction> actions = new ArrayList<>();
            Collection<DBConsole> consoles = connection.getConsoleBundle().getConsoles();
            for (DBConsole console : consoles) {
                actions.add(new SelectConsoleAction(console));
            }
            actions.add(Separator.getInstance());
            actions.add(new SelectConsoleAction(connection, DBConsoleType.STANDARD));
            if (DatabaseFeature.DEBUGGING.isSupported(connection)) {
                actions.add(new SelectConsoleAction(connection, DBConsoleType.DEBUG));
            }

            return actions.toArray(new AnAction[0]);
        }
    }

    private static class SelectConsoleAction extends BasicAction {
        private ConnectionRef connection;
        private DBConsole console;
        private DBConsoleType consoleType;


        SelectConsoleAction(ConnectionHandler connection, DBConsoleType consoleType) {
            super(txt("app.editors.action.NewConsole",consoleType.getName()));
            this.connection = ConnectionRef.of(connection);
            this.consoleType = consoleType;
        }

        SelectConsoleAction(DBConsole console) {
            super(Actions.adjustActionName(console.getName()), null, console.getIcon());
            this.console = console;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (console == null) {
                ConnectionHandler connection = this.connection.ensure();
                DatabaseConsoleManager consoleManager = DatabaseConsoleManager.getInstance(connection.getProject());
                consoleManager.showCreateConsoleDialog(connection, consoleType);
            } else {
                ConnectionHandler connection = console.ensureConnection();
                Editors.openFileEditor(connection.getProject(), console.getVirtualFile(), true);
            }
        }
    }

    private static void openSQLConsole(ConnectionHandler connection) {
        DBConsole defaultConsole = connection.getConsoleBundle().getDefaultConsole();
        Editors.openFileEditor(connection.getProject(), defaultConsole.getVirtualFile(), true);
    }
}
