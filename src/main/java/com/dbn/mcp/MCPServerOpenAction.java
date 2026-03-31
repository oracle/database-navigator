package com.dbn.mcp;

import com.dbn.common.action.ProjectAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.action.AbstractConnectionAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.common.ui.util.Popups.popupBuilder;
import static com.dbn.common.util.Actions.adjustActionName;
import static com.dbn.common.util.Lists.convert;

public class MCPServerOpenAction extends ProjectAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        List<ConnectionHandler> connections = ConnectionManager.getInstance(project).getConnections(DatabaseType.ORACLE);

        if (connections.size() == 1) {
            openMCPBuilder(connections.get(0));
            return;
        }

        List<SelectConnectionAction> actions = convert(connections, SelectConnectionAction::new);
        popupBuilder(actions, e)
                .withTitle("Select MCP Builder Connection")
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
            openMCPBuilder(connection);
        }
    }

    private static void openMCPBuilder(ConnectionHandler connection) {
        MCPServerManager.getInstance(connection.getProject()).openMCPBuilder(connection);
    }
}
