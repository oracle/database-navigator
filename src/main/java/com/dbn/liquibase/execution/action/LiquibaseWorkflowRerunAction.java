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

import static com.dbn.liquibase.execution.LiquibaseFeature.RERUN_ON_SUCCESS;
import static com.dbn.nls.NlsResources.txt;

public class LiquibaseWorkflowRerunAction extends AbstractLiquibaseWorkflowResultAction {
    public LiquibaseWorkflowRerunAction() {
        super(txt("app.execution.action.ExecuteAgain"));
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull LiquibaseWorkflowResult target) {
        DatabaseLiquibaseManager.getInstance(project).rerunWorkflow(target);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable LiquibaseWorkflowResult target) {
        presentation.setEnabled(isEnabled(target));
        presentation.setText(txt("app.execution.action.ExecuteAgain"));
        presentation.setIcon(Icons.EXEC_RESULT_RERUN);
    }

    private static boolean isEnabled(@Nullable LiquibaseWorkflowResult target) {
        if (target == null) return false;
        TaskStatus status = target.getContext().getStatus();

        if(status == TaskStatus.CANCELLED) return true;
        if(status == TaskStatus.FAILED) return true;

        return status == TaskStatus.DONE && target.getInput().getSupport().supports(RERUN_ON_SUCCESS);
    }
}
