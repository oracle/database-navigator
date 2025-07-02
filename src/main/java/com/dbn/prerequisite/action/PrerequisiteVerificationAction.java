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

package com.dbn.prerequisite.action;

import com.dbn.common.action.BasicAction;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.operation.DatabaseOperation;
import com.dbn.common.operation.DatabaseOperationType;
import com.dbn.common.ui.util.Popups;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBConsole;
import com.dbn.prerequisite.DatabasePrerequisiteManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.util.Actions.adjustActionName;
import static com.dbn.common.util.Lists.convert;
import static com.dbn.nls.NlsResources.txt;

public class PrerequisiteVerificationAction extends ProjectAction {

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.menu.action.PrerequisiteVerification"));
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
        Popups.popupBuilder(actions, e).
                withTitle("Select Console Connection").
                withSpeedSearch().
                buildAndShowCentered();
    }

    private static class SelectConnectionAction extends ActionGroup {
        private final ConnectionRef connection;

        private SelectConnectionAction(ConnectionHandler connection) {
            super(adjustActionName(connection.getName()), null, connection.getIcon());
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

            for (DatabaseOperationType operationType : DatabaseOperationType.values()) {
                actions.add(new OperationVerificationAction(connection, new DatabaseOperation(operationType)));
            }
            return actions.toArray(new AnAction[0]);
        }
    }

    private static class OperationVerificationAction extends BasicAction {
        private final ConnectionRef connection;
        private final DatabaseOperation operation;


        OperationVerificationAction(ConnectionHandler connection, DatabaseOperation operation) {
            super(operation.getType().getDescription());
            this.connection = ConnectionRef.of(connection);
            this.operation = operation;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            ConnectionHandler connection = this.connection.get();
            if (isNotValid(connection)) return;

            Project project = connection.getProject();
            DatabasePrerequisiteManager prerequisiteManager = DatabasePrerequisiteManager.getInstance(project);

            prerequisiteManager.evaluatePrerequisites(connection, operation);
        }
    }

    private static void openSQLConsole(ConnectionHandler connection) {
        DBConsole defaultConsole = connection.getConsoleBundle().getDefaultConsole();
        Editors.openFileEditor(connection.getProject(), defaultConsole.getVirtualFile(), true);
    }
}
