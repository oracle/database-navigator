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
import com.dbn.liquibase.model.LiquibaseArtifact;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

import static com.dbn.common.util.Actions.adjustActionName;
import static com.dbn.nls.NlsResources.txt;

/** Selects the Liquibase artifact used by a schema operation. */
@UtilityClass
public final class LiquibaseArtifactSelector {

    public static void selectLiquibaseArtifact(
            @NotNull AnActionEvent event,
            @NotNull Project project,
            @NotNull ConnectionHandler connection,
            @NotNull Consumer<LiquibaseArtifact> selectionConsumer) {
        DatabaseLiquibaseManager manager = DatabaseLiquibaseManager.getInstance(project);
        LiquibaseWorkspaceBundle workspace = manager.getWorkspace();
        ConnectionId connectionId = connection.getConnectionId();
        LiquibaseArtifact selectedArtifact = workspace.getArtifact(connectionId);

        DefaultActionGroup actionGroup = new DefaultActionGroup();
        for (LiquibaseArtifact artifact : workspace.getArtifacts().values()) {
            actionGroup.add(new SelectArtifactAction(workspace, connectionId, artifact, selectionConsumer));
        }
        if (actionGroup.getChildrenCount() > 0) actionGroup.addSeparator();
        actionGroup.add(new NewArtifactAction(manager, connection, selectionConsumer));

        Popups.popupBuilder(actionGroup, event)
                .withTitle(txt("msg.liquibase.title.SelectArtifact"))
                .withSpeedSearch()
                .withPreselectCondition(action ->
                        action instanceof SelectArtifactAction selected &&
                                selectedArtifact != null &&
                                Objects.equals(selected.getArtifact().getId(), selectedArtifact.getId()))
                .buildAndShowCentered();
    }

    private static String getArtifactName(LiquibaseArtifact artifact) {
        return Strings.isEmpty(artifact.getName())
                ? txt("app.shared.placeholder.Unnamed")
                : artifact.getName();
    }

    private static class SelectArtifactAction extends BasicAction {
        private final LiquibaseWorkspaceBundle workspace;
        private final ConnectionId connectionId;
        private final LiquibaseArtifact artifact;
        private final Consumer<LiquibaseArtifact> selectionConsumer;

        private SelectArtifactAction(
                LiquibaseWorkspaceBundle workspace,
                ConnectionId connectionId,
                LiquibaseArtifact artifact,
                Consumer<LiquibaseArtifact> selectionConsumer) {
            super(adjustActionName(getArtifactName(artifact)), null, Icons.DB_LIQUIBASE);
            this.workspace = workspace;
            this.connectionId = connectionId;
            this.artifact = artifact;
            this.selectionConsumer = selectionConsumer;
        }

        @NotNull
        public LiquibaseArtifact getArtifact() {
            return artifact;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            workspace.attachArtifact(connectionId, artifact.getId());
            selectionConsumer.accept(artifact);
        }
    }

    private static class NewArtifactAction extends BasicAction {
        private final DatabaseLiquibaseManager manager;
        private final ConnectionHandler connection;
        private final Consumer<LiquibaseArtifact> selectionConsumer;

        private NewArtifactAction(
                DatabaseLiquibaseManager manager,
                ConnectionHandler connection,
                Consumer<LiquibaseArtifact> selectionConsumer) {
            super(txt("app.liquibase.action.NewArtifact"), null, Icons.ACTION_ADD);
            this.manager = manager;
            this.connection = connection;
            this.selectionConsumer = selectionConsumer;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            manager.promptArtifactCreation(connection, selectionConsumer);
        }
    }
}
