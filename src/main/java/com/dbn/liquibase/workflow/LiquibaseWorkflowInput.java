/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.workflow;

import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.liquibase.operation.LiquibaseOperationInput;
import com.dbn.object.DBSchema;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import static com.dbn.liquibase.operation.LiquibaseFeature.DISTINCT_SCHEMAS;
import static com.dbn.liquibase.operation.LiquibaseFeature.SOURCE_SCHEMA;

/** Shared context used to initialize the inputs of operations in a workflow. */
@Getter
public class LiquibaseWorkflowInput extends LiquibaseOperationInput {
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
        LiquibaseWorkflowSupport support = getSupport();
        if (support.supports(DISTINCT_SCHEMAS)) {
            setSourceSchema(schema);
        } else {
            setTargetSchema(schema);
            if (support.supports(SOURCE_SCHEMA)) setSourceSchema(schema);
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
    public LiquibaseOperationInput createExecutionInput(@NotNull LiquibaseOperation operation) {
        LiquibaseOperationInput input = new LiquibaseOperationInput(getProject(), operation);
        return input.copyFrom(this);
    }
}
