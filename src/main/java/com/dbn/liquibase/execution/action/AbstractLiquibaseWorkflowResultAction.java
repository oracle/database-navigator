package com.dbn.liquibase.execution.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.ContextAction;
import com.dbn.common.action.DataKeys;
import com.dbn.liquibase.workflows.LiquibaseWorkflowResult;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

@BackgroundUpdate
public abstract class AbstractLiquibaseWorkflowResultAction extends ContextAction<LiquibaseWorkflowResult> {
    protected AbstractLiquibaseWorkflowResultAction(String text) {
        super(text);
    }

    @Override
    protected LiquibaseWorkflowResult getContext(@NotNull AnActionEvent e) {
        return e.getData(DataKeys.LIQUIBASE_WORKFLOW_RESULT);
    }
}
