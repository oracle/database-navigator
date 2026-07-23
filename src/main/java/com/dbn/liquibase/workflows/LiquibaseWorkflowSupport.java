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

import com.dbn.common.ui.form.field.FieldState;
import com.dbn.liquibase.execution.LiquibaseFeature;
import com.dbn.liquibase.execution.LiquibaseFeatureSupport;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.LiquibaseOperationSupport;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.dbn.liquibase.execution.LiquibaseFeature.DISTINCT_SCHEMAS;

/** Aggregates the context and result capabilities of the operations in a Liquibase workflow. */
public final class LiquibaseWorkflowSupport implements LiquibaseFeatureSupport {
    private final LiquibaseWorkflow workflow;
    private final List<LiquibaseOperationSupport> operationSupports;

    LiquibaseWorkflowSupport(@NotNull LiquibaseWorkflow workflow) {
        this.workflow = workflow;
        this.operationSupports = workflow.getOperations().stream()
                .map(LiquibaseOperation::getSupport)
                .toList();
    }

    public boolean includesOperation(@NotNull LiquibaseOperation operation) {
        return workflow.includesOperation(operation);
    }

    public FieldState getSourceContextState() {
        if (!supports(DISTINCT_SCHEMAS)) return FieldState.HIDDEN;
        return getContextState(LiquibaseOperationSupport::getSourceContextState);
    }

    public FieldState getTargetContextState() {
        if (supports(DISTINCT_SCHEMAS)) return FieldState.EDITABLE;
        return getContextState(LiquibaseOperationSupport::getTargetContextState);
    }

    public boolean supports(@NotNull LiquibaseFeature feature) {
        if (feature == LiquibaseFeature.RERUN_ON_SUCCESS) return workflow == LiquibaseWorkflow.DIAGNOSE_DATABASE;
        return any(support -> support.supports(feature));
    }

    public boolean requires(@NotNull LiquibaseFeature feature) {
        return any(support -> support.requires(feature));
    }

    private FieldState getContextState(@NotNull Function<LiquibaseOperationSupport, FieldState> stateProvider) {
        FieldState state = FieldState.HIDDEN;
        for (LiquibaseOperationSupport support : operationSupports) {
            state = max(state, stateProvider.apply(support));
        }
        return state;
    }

    private static FieldState max(@NotNull FieldState left, @NotNull FieldState right) {
        if (left == FieldState.EDITABLE || right == FieldState.EDITABLE) return FieldState.EDITABLE;
        if (left == FieldState.VISIBLE || right == FieldState.VISIBLE) return FieldState.VISIBLE;
        return FieldState.HIDDEN;
    }

    private boolean any(@NotNull Predicate<LiquibaseOperationSupport> predicate) {
        return operationSupports.stream().anyMatch(predicate);
    }
}
