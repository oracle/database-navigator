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

import com.dbn.common.task.TaskStatus;
import com.dbn.liquibase.execution.LiquibaseOperation;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Per-run state shared by a workflow and its operation results. */
@Getter
public class LiquibaseWorkflowContext {
    private final List<LiquibaseOperation> operations;
    private int operationIndex = -1;
    private TaskStatus status = TaskStatus.NEW;
    private volatile boolean cancellationRequested;

    public LiquibaseWorkflowContext(@NotNull LiquibaseWorkflow workflow) {
        this.operations = workflow.getOperations();
    }

    @Nullable
    public LiquibaseOperation getCurrentOperation() {
        return operationIndex >= 0 && operationIndex < operations.size() ? operations.get(operationIndex) : null;
    }

    public void startOperation(int index) {
        operationIndex = index;
        status = TaskStatus.RUNNING;
    }

    public void finish(@NotNull TaskStatus status) {
        this.status = status;
    }

    public void cancel() {
        cancellationRequested = true;
    }
}
