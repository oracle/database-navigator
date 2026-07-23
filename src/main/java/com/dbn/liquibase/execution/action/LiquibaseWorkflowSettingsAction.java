package com.dbn.liquibase.execution.action;

import com.dbn.common.icon.Icons;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.workflows.LiquibaseWorkflowResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public class LiquibaseWorkflowSettingsAction extends AbstractLiquibaseWorkflowResultAction {
    public LiquibaseWorkflowSettingsAction() {
        super(txt("app.execution.action.Settings"));
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull LiquibaseWorkflowResult target) {
        DatabaseLiquibaseManager liquibaseManager = DatabaseLiquibaseManager.getInstance(project);
        liquibaseManager.openWorkspaceSettings();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable LiquibaseWorkflowResult target) {
        presentation.setEnabled(target != null);
        presentation.setText(txt("app.execution.action.Settings"));
        presentation.setIcon(Icons.EXEC_RESULT_OPTIONS);
    }
}
