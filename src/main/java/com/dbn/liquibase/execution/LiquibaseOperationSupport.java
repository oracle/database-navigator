/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.liquibase.execution;

import com.dbn.common.ui.form.field.FieldState;
import org.jetbrains.annotations.NotNull;

/** Defines the context and result capabilities of a Liquibase operation. */
public final class LiquibaseOperationSupport {
    private final LiquibaseOperation operation;

    LiquibaseOperationSupport(@NotNull LiquibaseOperation operation) {
        this.operation = operation;
    }

    public FieldState getSourceContextState() {
        if (operation.isOneOf(LiquibaseOperation.INITIALIZE, LiquibaseOperation.COMPARE)) return FieldState.VISIBLE;
        return FieldState.HIDDEN;
    }

    public FieldState getTargetContextState() {
        if (operation == LiquibaseOperation.COMPARE) return FieldState.EDITABLE;
        if (operation.isOneOf(
                LiquibaseOperation.VALIDATE,
                LiquibaseOperation.STATUS,
                LiquibaseOperation.UPDATE,
                LiquibaseOperation.ROLLBACK)) return FieldState.VISIBLE;
        return FieldState.HIDDEN;
    }

    public boolean requiresSourceSchema() {
        return getSourceContextState().isVisible();
    }

    public boolean requiresTargetSchema() {
        return getTargetContextState().isVisible();
    }

    public boolean supportsSnapshotItems() {
        return operation == LiquibaseOperation.INITIALIZE;
    }

    public boolean supportsChangeSetItems() {
        return operation.isOneOf(LiquibaseOperation.UPDATE, LiquibaseOperation.ROLLBACK);
    }

    public boolean supportsComparisonItems() {
        return operation == LiquibaseOperation.COMPARE;
    }

    public boolean supportsTrackingTables() {
        return operation.isOneOf(
                LiquibaseOperation.STATUS,
                LiquibaseOperation.UPDATE,
                LiquibaseOperation.ROLLBACK);
    }

    public boolean supportsWorkspaceCreation() {
        return operation.isOneOf(LiquibaseOperation.INITIALIZE, LiquibaseOperation.COMPARE);
    }
}
