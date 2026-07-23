package com.dbn.liquibase.execution.action;

import com.dbn.common.icon.Icons;
import com.dbn.common.task.TaskStatus;
import com.dbn.execution.ExecutionManager;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.workflows.LiquibaseWorkflowResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Messages.OPTIONS_YES_NO;
import static com.dbn.common.util.Messages.showQuestionDialog;
import static com.dbn.common.util.Messages.whenOk;
import static com.dbn.nls.NlsResources.txt;

public class LiquibaseWorkflowCloseAction extends AbstractLiquibaseWorkflowResultAction {
    public LiquibaseWorkflowCloseAction() {
        super(txt("app.execution.action.Close"));
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull LiquibaseWorkflowResult target) {
        if (target.getContext().getStatus() == TaskStatus.RUNNING) {
            showQuestionDialog(
                    project,
                    txt("msg.liquibase.title.ExecutionActive"),
                    txt("msg.liquibase.question.ExecutionActive"),
                    OPTIONS_YES_NO, 0,
                    whenOk(() -> cancelAndClose(project, target)));
        } else {
            close(project, target);
        }
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable LiquibaseWorkflowResult target) {
        presentation.setEnabled(target != null);
        presentation.setText(txt("app.execution.action.Close"));
        presentation.setIcon(Icons.EXEC_RESULT_CLOSE);
    }

    private static void cancelAndClose(@NotNull Project project, @NotNull LiquibaseWorkflowResult target) {
        if (target.getContext().getStatus() != TaskStatus.RUNNING) {
            close(project, target);
            return;
        }

        target.addListener(() -> {
            if (target.getContext().getStatus() != TaskStatus.RUNNING) close(project, target);
        });
        if (target.getContext().getStatus() != TaskStatus.RUNNING) {
            close(project, target);
            return;
        }
        DatabaseLiquibaseManager.getInstance(project).cancelWorkflow(target);
    }

    private static void close(@NotNull Project project, @NotNull LiquibaseWorkflowResult target) {
        ExecutionManager.getInstance(project).removeResultTab(target);
    }
}
