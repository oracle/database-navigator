package com.dbn.menu.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.nls.NlsResources.txt;

/** Opens the project-level Liquibase workspace overview. */
public class LiquibaseWorkspacesOpenAction extends ProjectAction {
    public LiquibaseWorkspacesOpenAction() {
        this(txt("app.menu.action.LiquibaseWorkspaces"), null);
    }


    public LiquibaseWorkspacesOpenAction(@NotNull String text, @Nullable Icon icon) {
        super(text, null, icon);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        DatabaseLiquibaseManager liquibaseManager = DatabaseLiquibaseManager.getInstance(project);
        liquibaseManager.openWorkspaceSettings();
    }
}
