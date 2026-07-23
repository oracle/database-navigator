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
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.execution.LiquibaseExecutionContext;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.processor.LiquibaseExecutionProcessors;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Executes the operations of a Liquibase workflow sequentially and records every operation result. */
public class LiquibaseWorkflowExecutor {
    private final LiquibaseWorkflowResult result;
    private volatile LiquibaseExecutionContext currentContext;

    public LiquibaseWorkflowExecutor(@NotNull LiquibaseWorkflowResult result) {
        this.result = result;
    }

    public void execute() {
        List<LiquibaseOperation> operations = result.getContext().getInput().getWorkflow().getOperations();
        result.getContext().finish(TaskStatus.RUNNING);

        for (int index = 0; index < operations.size(); index++) {
            if (result.getContext().isCancellationRequested()) {
                skipOperations(operations, index, TaskStatus.CANCELLED);
                result.getContext().finish(TaskStatus.CANCELLED);
                result.notifyChanged();
                return;
            }

            LiquibaseExecutionResult operationResult = executeOperation(index, operations.get(index));
            TaskStatus status = operationResult.getStatus();
            if (status == TaskStatus.FAILED || status == TaskStatus.CANCELLED) {
                skipOperations(operations, index + 1, TaskStatus.SKIPPED);
                result.getContext().finish(status);
                result.notifyChanged();
                return;
            }
        }

        result.getContext().finish(TaskStatus.DONE);
        result.notifyChanged();
    }

    public void cancel() {
        result.getContext().cancel();
        LiquibaseExecutionContext context = currentContext;
        if (context != null) context.cancel();
    }

    @NotNull
    private LiquibaseExecutionResult executeOperation(
            int index,
            @NotNull LiquibaseOperation operation) {
        result.getContext().startOperation(index);

        LiquibaseExecutionInput input = result.getContext().getInput().createExecutionInput(operation);
        LiquibaseExecutionContext context = new LiquibaseExecutionContext(input);
        currentContext = context;

        LiquibaseExecutionResult operationResult = context.prepareExecutionResult();
        result.addResult(operationResult);
        DatabaseLiquibaseManager manager = DatabaseLiquibaseManager.getInstance(input.getProject());
        manager.registerExecutionContext(operationResult, context);
        try {
            LiquibaseExecutionProcessor processor = LiquibaseExecutionProcessors.get(operation);
            processor.execute(context);
        } catch (Exception e) {
            operationResult.appendErrorOutput(e.getMessage());
            operationResult.notifyStarted();
            operationResult.notifyFinished(TaskStatus.FAILED);
        } finally {
            manager.unregisterExecutionContext(operationResult);
            currentContext = null;
        }
        return operationResult;
    }

    private void skipOperations(
            @NotNull List<LiquibaseOperation> operations,
            int fromIndex,
            @NotNull TaskStatus status) {
        for (int index = fromIndex; index < operations.size(); index++) {
            LiquibaseExecutionInput input = result.getContext().getInput().createExecutionInput(operations.get(index));
            LiquibaseExecutionContext context = new LiquibaseExecutionContext(input);
            LiquibaseExecutionResult operationResult = context.prepareExecutionResult();
            operationResult.notifyStarted();
            operationResult.notifyFinished(status);
            result.addResult(operationResult);
        }
    }
}
