package com.dbn.liquibase.action;

import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

/** Entry point for generating the initial Liquibase changelog for a connection. */
public class GenerateInitialChangelogAction extends LiquibaseConnectionAction {
    public GenerateInitialChangelogAction(@NotNull ConnectionHandler connection) {
        super(connection);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        getManager(project).generateInitialChangelog(getConnection());
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.liquibase.action.GenerateInitialChangelog"));
        presentation.setIcon(Icons.ACTION_ADD);
        presentation.setVisible(!isWorkspaceAttached(project));
    }
}
