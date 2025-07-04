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
import com.dbn.common.operation.DatabaseOperation;
import com.dbn.common.operation.DatabaseOperationType;
import com.dbn.common.ui.util.Popups;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.ConnectionRef;
import com.dbn.prerequisite.DatabasePrerequisiteManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.util.Actions.adjustActionName;
import static com.dbn.common.util.Lists.convert;

public class PrerequisiteVerificationAction extends ActionGroup {


    public AnAction[] getChildren(AnActionEvent e) {
        List<AnAction> actions = new ArrayList<>();

        for (DatabaseOperationType operationType : DatabaseOperationType.values()) {
            actions.add(new SelectOperationAction(new DatabaseOperation(operationType)));
        }
        return actions.toArray(new AnAction[0]);
    }

    private static class SelectOperationAction extends BasicAction {
        private final DatabaseOperation operation;


        SelectOperationAction(DatabaseOperation operation) {
            super(operation.getType().getDescription());
            this.operation = operation;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            Project project = e.getProject();
            if (isNotValid(project)) return;

            //FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.file");
            ConnectionManager connectionManager = ConnectionManager.getInstance(project);
            ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
            List<ConnectionHandler> connections = connectionBundle.getConnections();
            if (connections.isEmpty()) {
                connectionManager.promptMissingConnection();
                return;
            }

            if (connections.size() == 1) {
                openPrerequisiteEvaluator(connections.get(0), operation);
                return;
            }

            List<SelectTargetConnection> actions = convert(connections, c -> new SelectTargetConnection(c, operation));
            Popups.popupBuilder(actions, e).
                    withTitle("Prerequisite Verification Target").
                    withSpeedSearch().
                    buildAndShowCentered();
        }
    }


    private static class SelectTargetConnection extends BasicAction {
        private final ConnectionRef connection;
        private final DatabaseOperation operation;


        SelectTargetConnection(ConnectionHandler connection, DatabaseOperation operation) {
            super(adjustActionName(connection.getName()), null, connection.getIcon());
            this.connection = ConnectionRef.of(connection);
            this.operation = operation;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            ConnectionHandler connection = this.connection.get();
            openPrerequisiteEvaluator(connection, operation);
        }
    }

    private static void openPrerequisiteEvaluator(ConnectionHandler connection, DatabaseOperation operation) {
        if (isNotValid(connection)) return;

        Project project = connection.getProject();
        DatabasePrerequisiteManager prerequisiteManager = DatabasePrerequisiteManager.getInstance(project);

        prerequisiteManager.evaluatePrerequisites(connection, operation);
    }
}
