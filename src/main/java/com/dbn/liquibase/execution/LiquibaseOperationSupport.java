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

import static com.dbn.liquibase.execution.LiquibaseOperation.COMPARE_SCHEMAS;
import static com.dbn.liquibase.execution.LiquibaseOperation.GENERATE_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.GENERATE_DIFF_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.ROLLBACK_CHANGESETS;
import static com.dbn.liquibase.execution.LiquibaseOperation.ROLLBACK_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.SHOW_CHANGELOG_HISTORY;
import static com.dbn.liquibase.execution.LiquibaseOperation.SHOW_CHANGELOG_STATUS;
import static com.dbn.liquibase.execution.LiquibaseOperation.SYNCHRONIZE_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.SYNCHRONIZE_CHANGELOG_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.TAG_DATABASE;
import static com.dbn.liquibase.execution.LiquibaseOperation.UPDATE_DATABASE;
import static com.dbn.liquibase.execution.LiquibaseOperation.UPDATE_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.VALIDATE_CHANGELOG;

/** Defines the context and result capabilities of a Liquibase operation. */
public final class LiquibaseOperationSupport {
    private final LiquibaseOperation operation;

    LiquibaseOperationSupport(@NotNull LiquibaseOperation operation) {
        this.operation = operation;
    }

    public FieldState getSourceContextState() {
        if (operation.isOneOf(
                GENERATE_CHANGELOG,
                COMPARE_SCHEMAS,
                GENERATE_DIFF_CHANGELOG)) return FieldState.VISIBLE;

        return FieldState.HIDDEN;
    }

    public FieldState getTargetContextState() {
        if (operation.isOneOf(
                COMPARE_SCHEMAS,
                GENERATE_DIFF_CHANGELOG)) return FieldState.EDITABLE;

        if (operation.isOneOf(
                VALIDATE_CHANGELOG,
                SHOW_CHANGELOG_STATUS,
                SHOW_CHANGELOG_HISTORY,
                SYNCHRONIZE_CHANGELOG,
                SYNCHRONIZE_CHANGELOG_SQL,
                UPDATE_DATABASE,
                UPDATE_SQL,
                TAG_DATABASE,
                ROLLBACK_CHANGESETS,
                ROLLBACK_SQL)) return FieldState.VISIBLE;

        return FieldState.HIDDEN;
    }

    public boolean requiresSourceSchema() {
        return getSourceContextState().isVisible();
    }

    public boolean requiresTargetSchema() {
        return getTargetContextState().isVisible();
    }

    public boolean supportsSnapshotItems() {
        return operation == GENERATE_CHANGELOG;
    }

    public boolean supportsChangelogAuthor() {
        return operation == GENERATE_CHANGELOG;
    }

    public boolean supportsDatabaseTag() {
        return operation.isOneOf(
                GENERATE_CHANGELOG,
                GENERATE_DIFF_CHANGELOG,
                TAG_DATABASE);
    }

    public boolean requiresDatabaseTag() {
        return operation == TAG_DATABASE;
    }

    public boolean supportsCheckpointTag() {
        return operation == UPDATE_DATABASE;
    }

    public boolean supportsRollbackTag() {
        return operation.isOneOf(ROLLBACK_CHANGESETS, ROLLBACK_SQL);
    }

    public boolean supportsRollback() {
        return operation.isOneOf(ROLLBACK_CHANGESETS, ROLLBACK_SQL);
    }

    public boolean supportsChangeSetItems() {
        return operation.isOneOf(
                UPDATE_DATABASE,
                UPDATE_SQL,
                SHOW_CHANGELOG_HISTORY,
                SYNCHRONIZE_CHANGELOG,
                SYNCHRONIZE_CHANGELOG_SQL,
                ROLLBACK_CHANGESETS,
                ROLLBACK_SQL);
    }

    public boolean supportsSqlOutput() {
        return operation.isOneOf(UPDATE_SQL, SYNCHRONIZE_CHANGELOG_SQL, ROLLBACK_SQL);
    }

    public boolean supportsComparisonItems() {
        return operation.isOneOf(
                COMPARE_SCHEMAS,
                GENERATE_DIFF_CHANGELOG);
    }

    public boolean supportsTrackingTables() {
        return operation.isOneOf(
                SHOW_CHANGELOG_STATUS,
                SHOW_CHANGELOG_HISTORY,
                SYNCHRONIZE_CHANGELOG,
                UPDATE_DATABASE,
                TAG_DATABASE,
                ROLLBACK_CHANGESETS);
    }

    public boolean requiresWorkspace() {
        return operation.isOneOf(
                GENERATE_CHANGELOG,
                VALIDATE_CHANGELOG,
                GENERATE_DIFF_CHANGELOG,
                SHOW_CHANGELOG_STATUS,
                SHOW_CHANGELOG_HISTORY,
                SYNCHRONIZE_CHANGELOG,
                SYNCHRONIZE_CHANGELOG_SQL,
                UPDATE_DATABASE,
                ROLLBACK_CHANGESETS,
                UPDATE_SQL,
                ROLLBACK_SQL);
    }

    public boolean supportsWorkspaceCreation() {
        return operation.isOneOf(
                GENERATE_CHANGELOG,
                GENERATE_DIFF_CHANGELOG);
    }
}
