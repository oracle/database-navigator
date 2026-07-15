package com.dbn.liquibase.execution.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.ContextAction;
import com.dbn.common.action.DataKeys;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@BackgroundUpdate
public abstract class AbstractLiquibaseExecutionResultAction extends ContextAction<LiquibaseExecutionResult> {
    protected AbstractLiquibaseExecutionResultAction(String text) {
        super(text);
    }

    protected static @NotNull DatabaseLiquibaseManager getLiquibaseManager(@NotNull Project project) {
        return DatabaseLiquibaseManager.getInstance(project);
    }

    protected LiquibaseExecutionResult getContext(@NotNull AnActionEvent e) {
        return e.getData(DataKeys.LIQUIBASE_EXECUTION_RESULT);
    }
}
