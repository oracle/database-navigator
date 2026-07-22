/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.workflows;

import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.object.DBSchema;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/** Shared context used to initialize the inputs of operations in a workflow. */
@Getter
public class LiquibaseWorkflowInput extends LiquibaseExecutionInput {
    private final LiquibaseWorkflow workflow;

    public LiquibaseWorkflowInput(@NotNull Project project, @NotNull LiquibaseWorkflow workflow) {
        super(project, LiquibaseOperation.VALIDATE_CHANGELOG);
        this.workflow = workflow;
    }

    @Override
    @NotNull
    public LiquibaseWorkflowSupport getSupport() {
        return workflow.getSupport();
    }

    @Override
    public boolean containsOperation(@NotNull LiquibaseOperation operation) {
        return workflow.includesOperation(operation);
    }

    public void setInitialSchema(@NotNull DBSchema schema) {
        if (workflow.includesOperation(LiquibaseOperation.COMPARE_SCHEMAS)) {
            setSourceSchema(schema);
        } else {
            setTargetSchema(schema);
            if (workflow.includesOperation(LiquibaseOperation.GENERATE_CHANGELOG)) setSourceSchema(schema);
        }
    }

    @Override
    @NotNull
    public String getHint() {
        return workflow.getHint();
    }

    @Override
    @NotNull
    public String getDocumentationUrl() {
        return "";
    }

    @NotNull
    public LiquibaseExecutionInput createExecutionInput(@NotNull LiquibaseOperation operation) {
        LiquibaseExecutionInput input = new LiquibaseExecutionInput(getProject(), operation);
        input.setSourceSchema(getSourceSchema());
        input.setTargetSchema(getTargetSchema());
        input.setWorkspace(getWorkspace());
        input.getRollbackInstruction().setType(getRollbackInstruction().getType());
        input.getRollbackInstruction().setCount(getRollbackInstruction().getCount());
        input.getRollbackInstruction().setTag(getRollbackInstruction().getTag());
        input.getRollbackInstruction().setDate(getRollbackInstruction().getDate());
        input.getUpdateInstruction().setType(getUpdateInstruction().getType());
        input.getUpdateInstruction().setCount(getUpdateInstruction().getCount());
        input.getUpdateInstruction().setTag(getUpdateInstruction().getTag());
        input.setChangelogAuthor(getChangelogAuthor());
        input.setDatabaseTag(getDatabaseTag());
        input.setCheckpointTag(getCheckpointTag());
        return input;
    }
}
