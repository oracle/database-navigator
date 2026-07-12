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

public class LiquibaseExecutionStopAction extends AbstractLiquibaseExecutionResultAction {
    public LiquibaseExecutionStopAction() {
        super(txt("app.execution.action.StopExecution"));
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull LiquibaseExecutionResult target) {
        DatabaseLiquibaseManager.getInstance(project).cancelExecution(target);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable LiquibaseExecutionResult target) {
        presentation.setEnabled(isEnabled(target));
        presentation.setText(txt("app.execution.action.StopExecution"));
        presentation.setIcon(Icons.ACTION_STOP);
    }

    private static boolean isEnabled(@Nullable LiquibaseExecutionResult target) {
        return target != null && target.getStatus() == TaskStatus.RUNNING;
    }
}
