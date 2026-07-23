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

import com.dbn.common.task.TaskStatus;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.processor.LiquibaseExecutionProcessors;
import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.liquibase.operation.LiquibaseOperationContext;
import com.dbn.liquibase.operation.LiquibaseOperationInput;
import com.dbn.liquibase.operation.LiquibaseOperationResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Executes the operations of a Liquibase workflow sequentially and records every operation result. */
public class LiquibaseWorkflowExecutor {
    private final LiquibaseWorkflowResult result;
    private volatile LiquibaseOperationContext currentContext;

    public LiquibaseWorkflowExecutor(@NotNull LiquibaseWorkflowResult result) {
        this.result = result;
    }

    public void execute() {
        List<LiquibaseOperation> operations = result.getInput().getWorkflow().getOperations();
        result.getContext().finish(TaskStatus.RUNNING);

        for (int index = 0; index < operations.size(); index++) {
            if (result.getContext().isCancellationRequested()) {
                skipOperations(operations, index, TaskStatus.CANCELLED);
                result.getContext().finish(TaskStatus.CANCELLED);
                result.notifyChanged();
                return;
            }

            LiquibaseOperationResult operationResult = executeOperation(index, operations.get(index));
            TaskStatus status = operationResult.getStatus();
            if (status != TaskStatus.DONE) {
                skipOperations(operations, index + 1, TaskStatus.BYPASSED);
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
        LiquibaseOperationContext context = currentContext;
        if (context != null) context.cancel();
    }

    @NotNull
    private LiquibaseOperationResult executeOperation(
            int index,
            @NotNull LiquibaseOperation operation) {
        result.getContext().startOperation(index);

        LiquibaseOperationInput input = result.getInput().createExecutionInput(operation);
        LiquibaseOperationContext context = new LiquibaseOperationContext(input);
        currentContext = context;

        LiquibaseOperationResult operationResult = context.prepareExecutionResult();
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
            LiquibaseOperationInput input = result.getInput().createExecutionInput(operations.get(index));
            LiquibaseOperationContext context = new LiquibaseOperationContext(input);
            LiquibaseOperationResult operationResult = context.prepareExecutionResult();
            operationResult.notifyStarted();
            operationResult.notifyFinished(status);
            result.addResult(operationResult);
        }
    }
}
