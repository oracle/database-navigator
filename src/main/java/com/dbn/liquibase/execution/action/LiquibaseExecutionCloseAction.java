package com.dbn.liquibase.execution.action;

import com.dbn.common.icon.Icons;
import com.dbn.common.task.TaskStatus;
import com.dbn.execution.ExecutionManager;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public class LiquibaseExecutionCloseAction extends AbstractLiquibaseExecutionResultAction {
    public LiquibaseExecutionCloseAction() {
        super(txt("app.execution.action.Close"));
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull LiquibaseExecutionResult target) {
        ExecutionManager.getInstance(project).removeResultTab(target);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable LiquibaseExecutionResult target) {
        presentation.setEnabled(isEnabled(target));
        presentation.setText(txt("app.execution.action.Close"));
        presentation.setIcon(Icons.EXEC_RESULT_CLOSE);
    }

    private static boolean isEnabled(@Nullable LiquibaseExecutionResult target) {
        return target != null && target.getStatus() != TaskStatus.RUNNING;
    }
}
