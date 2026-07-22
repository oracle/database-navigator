/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.workflows.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.liquibase.execution.ui.LiquibaseExecutionInputForm;
import com.dbn.liquibase.workflows.LiquibaseWorkflow;
import com.dbn.liquibase.workflows.LiquibaseWorkflowInput;
import com.dbn.object.DBSchema;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

/** Dialog for collecting the shared and operation-specific inputs of a Liquibase workflow. */
@Getter
public class LiquibaseWorkflowInputDialog extends DBNDialog<LiquibaseExecutionInputForm> {
    private final LiquibaseWorkflowInput workflowInput;

    public LiquibaseWorkflowInputDialog(
            @NotNull DBSchema schema,
            @NotNull LiquibaseWorkflow workflow) {
        super(schema.getProject(), workflow.getTitle(), true);
        workflowInput = new LiquibaseWorkflowInput(getProject(), workflow);
        workflowInput.setInitialSchema(schema);
        setDefaultSize(700, 520);
        init();
    }

    @Override
    protected String getDimensionServiceKey() {
        return createDimensionSeviceKey(workflowInput.getWorkflow());
    }

    @NotNull
    @Override
    protected LiquibaseExecutionInputForm createForm() {
        return new LiquibaseExecutionInputForm(this, workflowInput);
    }

    @Override
    @NotNull
    protected Action[] initializeActions() {
        return actions(getOKAction(), getCancelAction());
    }
}
