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
                skipOperations(operations, TaskStatus.CANCELLED, index);
                result.getContext().finish(TaskStatus.CANCELLED);
                result.notifyChanged();
                return;
            }

            LiquibaseOperationResult operationResult = executeOperation(index);
            TaskStatus status = operationResult.getStatus();
            if (status != TaskStatus.DONE) {
                skipOperations(operations, TaskStatus.BYPASSED, index + 1);
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
    private LiquibaseOperationResult executeOperation(int index) {
        LiquibaseWorkflowContext context = result.getContext();
        context.startOperation(index);

        LiquibaseWorkflowInput input = result.getInput();
        List<LiquibaseOperation> operations = input.getWorkflow().getOperations();
        LiquibaseOperation operation = operations.get(index);

        LiquibaseOperationInput operationInput = input.createExecutionInput(operation);
        LiquibaseOperationContext operationContext = new LiquibaseOperationContext(operationInput, context);
        currentContext = operationContext;

        LiquibaseOperationResult operationResult = operationContext.getResult();
        result.addResult(operationResult);
        DatabaseLiquibaseManager manager = DatabaseLiquibaseManager.getInstance(operationInput.getProject());
        try {
            LiquibaseExecutionProcessor processor = LiquibaseExecutionProcessors.get(operation);
            processor.execute(operationContext);
        } catch (Exception e) {
            operationResult.appendErrorOutput(e.getMessage());
            operationResult.notifyStarted();
            operationResult.notifyFinished(TaskStatus.FAILED);
        } finally {
            currentContext = null;
        }
        return operationResult;
    }

    private void skipOperations(
            @NotNull List<LiquibaseOperation> operations,
            @NotNull TaskStatus status,
            int fromIndex) {
        LiquibaseWorkflowContext context = result.getContext();
        for (int index = fromIndex; index < operations.size(); index++) {
            LiquibaseOperationInput operationInput = result.getInput().createExecutionInput(operations.get(index));
            LiquibaseOperationContext operationContext = new LiquibaseOperationContext(operationInput, context);
            LiquibaseOperationResult operationResult = operationContext.getResult();
            operationResult.notifyStarted();
            operationResult.notifyFinished(status);
            result.addResult(operationResult);
        }
    }
}
