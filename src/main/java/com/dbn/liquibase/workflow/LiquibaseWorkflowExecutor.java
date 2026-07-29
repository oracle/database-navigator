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
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.processor.LiquibaseExecutionProcessors;
import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.liquibase.operation.LiquibaseOperationContext;
import com.dbn.liquibase.operation.LiquibaseOperationInput;
import com.dbn.liquibase.operation.LiquibaseOperationResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.dbn.common.task.TaskStatus.BYPASSED;
import static com.dbn.common.task.TaskStatus.CANCELLED;
import static com.dbn.common.task.TaskStatus.DONE;
import static com.dbn.common.task.TaskStatus.FAILED;
import static com.dbn.common.task.TaskStatus.PAUSED;
import static com.dbn.common.task.TaskStatus.RUNNING;

/** Executes the operations of a Liquibase workflow sequentially and records every operation result. */
public class LiquibaseWorkflowExecutor {
    private final LiquibaseWorkflowResult result;
    private volatile LiquibaseOperationContext currentContext;

    public LiquibaseWorkflowExecutor(@NotNull LiquibaseWorkflowResult result) {
        this.result = result;
    }

    public void execute() {
        executeFrom(0);
    }

    private void executeFrom(int startIndex) {
        List<LiquibaseOperation> operations = result.getInput().getWorkflow().getOperations();
        LiquibaseWorkflowContext context = result.getContext();
        context.finish(RUNNING);

        for (int index = startIndex; index < operations.size(); index++) {
            if (context.isCancellationRequested()) {
                skipOperations(operations, CANCELLED, index);
                context.finish(CANCELLED);
                result.notifyChanged();
                return;
            }

            LiquibaseOperationResult operationResult = executeOperation(index);
            TaskStatus status = operationResult.getStatus();
            if (status == PAUSED) {
                context.finish(PAUSED);
                result.notifyChanged();
                return;
            }
            if (status != DONE) {
                skipOperations(operations, BYPASSED, index + 1);
                context.finish(status);
                result.notifyChanged();
                return;
            }
        }

        context.finish(DONE);
        result.notifyChanged();
    }

    public void resume(@NotNull LiquibaseOperationResult operationResult) {
        int index = result.getOperationResults().indexOf(operationResult);
        if (index < 0) return;
        executeFrom(index);
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

        LiquibaseOperationContext operationContext;
        LiquibaseOperationResult operationResult;
        List<LiquibaseOperationResult> operationResults = result.getOperationResults();
        if (index < operationResults.size()) {
            operationResult = operationResults.get(index);
            operationContext = operationResult.getContext();
        } else {
            LiquibaseOperationInput operationInput = input.createExecutionInput(operation);
            operationContext = new LiquibaseOperationContext(operationInput, context);
            operationResult = operationContext.getResult();
            result.addResult(operationResult);
        }
        currentContext = operationContext;
        try {
            LiquibaseExecutionProcessor processor = LiquibaseExecutionProcessors.get(operation);
            processor.execute(operationContext);
        } catch (Exception e) {
            operationResult.appendErrorOutput(e.getMessage());
            operationResult.notifyStarted();
            operationResult.notifyFinished(FAILED);
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
