package com.dbn.liquibase.execution.action;

import com.dbn.common.icon.Icons;
import com.dbn.common.task.TaskStatus;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public class LiquibaseExecutionRerunAction extends AbstractLiquibaseExecutionResultAction {
    public LiquibaseExecutionRerunAction() {
        super(txt("app.execution.action.ExecuteAgain"));
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull LiquibaseExecutionResult target) {
        DatabaseLiquibaseManager liquibaseManager = getLiquibaseManager(project);
        liquibaseManager.rerunOperation(target);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable LiquibaseExecutionResult target) {
        presentation.setEnabled(isEnabled(target));
        presentation.setText(txt("app.execution.action.ExecuteAgain"));
        presentation.setIcon(Icons.EXEC_RESULT_RERUN);
    }

    private static boolean isEnabled(@Nullable LiquibaseExecutionResult target) {
        if (target == null) return false;

        TaskStatus status = target.getStatus();
        return status == TaskStatus.CANCELLED || status == TaskStatus.FAILED;
    }
}
