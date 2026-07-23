/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.action;

import com.dbn.liquibase.workflows.LiquibaseWorkflow;
import com.dbn.liquibase.workflows.ui.LiquibaseWorkflowInputDialog;
import com.dbn.object.DBSchema;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Dialogs.show;
import static com.dbn.common.util.Dialogs.whenOk;
import static com.dbn.liquibase.execution.LiquibaseOperationConfirmations.confirmWorkspaceAvailable;
import static com.dbn.nls.NlsResources.txt;

/** Entry point for a reusable Liquibase workflow scoped to a database schema. */
public class LiquibaseWorkflowAction extends LiquibaseSchemaAction {
    private final LiquibaseWorkflow workflow;

    public LiquibaseWorkflowAction(
            @NotNull DBSchema schema,
            @NotNull LiquibaseWorkflow workflow) {
        super(schema);
        this.workflow = workflow;
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        if (!confirmWorkspaceAvailable(
                getSchema().getConnection(),
                workflow.getSupport())) return;

        show(() -> new LiquibaseWorkflowInputDialog(getSchema(), workflow),
                whenOk(dialog -> getManager(project).executeWorkflow(dialog.getWorkflowInput())));
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.liquibase.action.Workflow_" + workflow.name()));
        presentation.setDescription(workflow.getDescription());
    }
}
