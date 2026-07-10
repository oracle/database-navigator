package com.dbn.menu.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.common.icon.Icons;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

/** Opens the project-level Liquibase workspace overview. */
public class LiquibaseWorkspacesOpenAction extends ProjectAction {
    public LiquibaseWorkspacesOpenAction() {
        super(txt("app.menu.action.LiquibaseWorkspaces"), null, Icons.DB_LIQUIBASE);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        DatabaseLiquibaseManager.getInstance(project).openWorkspaceSettings();
    }
}
