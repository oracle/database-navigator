package com.dbn.liquibase.execution.action;

import com.dbn.common.icon.Icons;
import com.dbn.common.task.TaskStatus;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.workflows.LiquibaseWorkflowResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public class LiquibaseWorkflowStopAction extends AbstractLiquibaseWorkflowResultAction {
    public LiquibaseWorkflowStopAction() {
        super(txt("app.execution.action.StopExecution"));
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull LiquibaseWorkflowResult target) {
        DatabaseLiquibaseManager liquibaseManager = DatabaseLiquibaseManager.getInstance(project);
        liquibaseManager.cancelWorkflow(target);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable LiquibaseWorkflowResult target) {
        presentation.setEnabled(target != null && target.getContext().getStatus() == TaskStatus.RUNNING);
        presentation.setText(txt("app.execution.action.StopExecution"));
        presentation.setIcon(Icons.ACTION_STOP);
    }
}
