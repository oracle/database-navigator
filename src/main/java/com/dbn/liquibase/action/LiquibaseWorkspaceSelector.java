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

package com.dbn.liquibase.action;

import com.dbn.common.action.BasicAction;
import com.dbn.common.action.DefaultActionGroup;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.util.Popups;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.model.LiquibaseWorkspace;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

import static com.dbn.common.util.Actions.adjustActionName;
import static com.dbn.nls.NlsResources.txt;

/** Selects the Liquibase workspace used by a schema operation. */
@UtilityClass
public final class LiquibaseWorkspaceSelector {

    public static void selectLiquibaseWorkspace(
            @NotNull AnActionEvent event,
            @NotNull Project project,
            @NotNull ConnectionHandler connection,
            @Nullable LiquibaseOperation operation,
            @NotNull Consumer<LiquibaseWorkspace> selectionConsumer) {
        DatabaseLiquibaseManager manager = DatabaseLiquibaseManager.getInstance(project);
        LiquibaseWorkspaceBundle workspaces = manager.getWorkspaces();
        ConnectionId connectionId = connection.getConnectionId();
        LiquibaseWorkspace selectedWorkspace = workspaces.getWorkspace(connectionId);

        DefaultActionGroup actionGroup = new DefaultActionGroup();
        for (LiquibaseWorkspace workspace : workspaces.getEntries().values()) {
            actionGroup.add(new SelectWorkspaceAction(workspaces, connectionId, workspace, selectionConsumer));
        }
        if (actionGroup.getChildrenCount() > 0) actionGroup.addSeparator();
        actionGroup.add(new CreateWorkspaceAction(manager, connection, operation, selectionConsumer));

        Popups.popupBuilder(actionGroup, event)
                .withTitle(txt("msg.liquibase.title.SelectWorkspace"))
                .withSpeedSearch()
                .withPreselectCondition(action ->
                        action instanceof SelectWorkspaceAction selected &&
                                selectedWorkspace != null &&
                                Objects.equals(selected.getWorkspace().getId(), selectedWorkspace.getId()))
                .buildAndShowCentered();
    }

    private static String getWorkspaceName(LiquibaseWorkspace workspace) {
        return Strings.isEmpty(workspace.getName())
                ? txt("app.shared.placeholder.Unnamed")
                : workspace.getName();
    }

    private static class SelectWorkspaceAction extends BasicAction {
        private final LiquibaseWorkspaceBundle workspaces;
        private final LiquibaseWorkspace workspace;
        private final ConnectionId connectionId;
        private final Consumer<LiquibaseWorkspace> selectionConsumer;

        private SelectWorkspaceAction(
                LiquibaseWorkspaceBundle workspaces,
                ConnectionId connectionId,
                LiquibaseWorkspace workspace,
                Consumer<LiquibaseWorkspace> selectionConsumer) {
            super(adjustActionName(getWorkspaceName(workspace)), null, Icons.DB_LIQUIBASE);
            this.workspaces = workspaces;
            this.connectionId = connectionId;
            this.workspace = workspace;
            this.selectionConsumer = selectionConsumer;
        }

        @NotNull
        public LiquibaseWorkspace getWorkspace() {
            return workspace;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            workspaces.attachWorkspace(connectionId, workspace.getId());
            selectionConsumer.accept(workspace);
        }
    }

    private static class CreateWorkspaceAction extends BasicAction {
        private final DatabaseLiquibaseManager manager;
        private final ConnectionHandler connection;
        private final LiquibaseOperation operation;
        private final Consumer<LiquibaseWorkspace> selectionConsumer;

        private CreateWorkspaceAction(
                DatabaseLiquibaseManager manager,
                ConnectionHandler connection,
                @Nullable LiquibaseOperation operation,
                Consumer<LiquibaseWorkspace> selectionConsumer) {
            super(txt("app.liquibase.action.NewWorkspace"), null, Icons.ACTION_ADD);
            this.manager = manager;
            this.connection = connection;
            this.operation = operation;
            this.selectionConsumer = selectionConsumer;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            manager.promptWorkspaceCreation(connection, operation, selectionConsumer);
        }
    }
}
