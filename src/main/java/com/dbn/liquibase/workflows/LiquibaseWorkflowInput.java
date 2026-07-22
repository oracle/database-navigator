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

import com.dbn.common.component.ProjectUnit;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.model.LiquibaseWorkspace;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Shared context used to initialize the inputs of operations in a workflow. */
@Getter
@Setter
public class LiquibaseWorkflowInput extends ProjectUnit {
    private final LiquibaseWorkflow workflow;
    private final LiquibaseWorkspaceBundle workspaces;
    private DBObjectRef<DBSchema> sourceSchema;
    private DBObjectRef<DBSchema> targetSchema;
    private LiquibaseWorkspace workspace;

    public LiquibaseWorkflowInput(@NotNull Project project, @NotNull LiquibaseWorkflow workflow) {
        super(project);
        this.workflow = workflow;
        this.workspaces = com.dbn.liquibase.DatabaseLiquibaseManager.getInstance(project).getWorkspaces();
    }

    @Nullable
    public DBSchema getSourceSchema() {
        return DBObjectRef.get(sourceSchema);
    }

    public void setSourceSchema(@Nullable DBSchema schema) {
        sourceSchema = DBObjectRef.of(schema);
    }

    @Nullable
    public DBSchema getTargetSchema() {
        return DBObjectRef.get(targetSchema);
    }

    public void setTargetSchema(@Nullable DBSchema schema) {
        targetSchema = DBObjectRef.of(schema);
    }

    public void setWorkspace(@Nullable LiquibaseWorkspace workspace) {
        this.workspace = workspace == null ? null : workspace.clone();
    }

    @NotNull
    public LiquibaseExecutionInput createExecutionInput(@NotNull LiquibaseOperation operation) {
        LiquibaseExecutionInput input = new LiquibaseExecutionInput(getProject(), operation);
        input.setSourceSchema(getSourceSchema());
        input.setTargetSchema(getTargetSchema());
        input.setWorkspace(workspace);
        return input;
    }
}
