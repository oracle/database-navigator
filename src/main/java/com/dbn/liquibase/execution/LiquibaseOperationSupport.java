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

import static com.dbn.liquibase.execution.LiquibaseOperation.CALCULATE_CHECKSUMS;
import static com.dbn.liquibase.execution.LiquibaseOperation.CLEAR_CHECKSUMS;
import static com.dbn.liquibase.execution.LiquibaseOperation.COMPARE_SCHEMAS;
import static com.dbn.liquibase.execution.LiquibaseOperation.DROP_ALL;
import static com.dbn.liquibase.execution.LiquibaseOperation.FUTURE_ROLLBACK;
import static com.dbn.liquibase.execution.LiquibaseOperation.GENERATE_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.GENERATE_DATABASE_DOCUMENTATION;
import static com.dbn.liquibase.execution.LiquibaseOperation.GENERATE_DIFF_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.LIST_LOCKS;
import static com.dbn.liquibase.execution.LiquibaseOperation.MARK_NEXT_CHANGESET_RAN;
import static com.dbn.liquibase.execution.LiquibaseOperation.RELEASE_LOCKS;
import static com.dbn.liquibase.execution.LiquibaseOperation.ROLLBACK_CHANGESETS;
import static com.dbn.liquibase.execution.LiquibaseOperation.ROLLBACK_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.SHOW_CHANGELOG_HISTORY;
import static com.dbn.liquibase.execution.LiquibaseOperation.SHOW_CHANGELOG_STATUS;
import static com.dbn.liquibase.execution.LiquibaseOperation.SNAPSHOT_DATABASE;
import static com.dbn.liquibase.execution.LiquibaseOperation.SYNCHRONIZE_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.SYNCHRONIZE_CHANGELOG_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.SYNCHRONIZE_CHANGELOG_TO_TAG;
import static com.dbn.liquibase.execution.LiquibaseOperation.TAG_DATABASE;
import static com.dbn.liquibase.execution.LiquibaseOperation.UNEXPECTED_CHANGESETS;
import static com.dbn.liquibase.execution.LiquibaseOperation.UPDATE_DATABASE;
import static com.dbn.liquibase.execution.LiquibaseOperation.UPDATE_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.UPDATE_TESTING_ROLLBACK;
import static com.dbn.liquibase.execution.LiquibaseOperation.VALIDATE_CHANGELOG;

/** Defines the context and result capabilities of a Liquibase operation. */
public final class LiquibaseOperationSupport implements LiquibaseFeatureSupport {
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
                GENERATE_DATABASE_DOCUMENTATION,
                SNAPSHOT_DATABASE,
                SHOW_CHANGELOG_STATUS,
                SHOW_CHANGELOG_HISTORY,
                UNEXPECTED_CHANGESETS,
                SYNCHRONIZE_CHANGELOG,
                SYNCHRONIZE_CHANGELOG_TO_TAG,
                SYNCHRONIZE_CHANGELOG_SQL,
                UPDATE_DATABASE,
                UPDATE_TESTING_ROLLBACK,
                UPDATE_SQL,
                FUTURE_ROLLBACK,
                TAG_DATABASE,
                MARK_NEXT_CHANGESET_RAN,
                RELEASE_LOCKS,
                CLEAR_CHECKSUMS,
                LIST_LOCKS,
                CALCULATE_CHECKSUMS,
                DROP_ALL,
                ROLLBACK_CHANGESETS,
                ROLLBACK_SQL)) return FieldState.VISIBLE;

        return FieldState.HIDDEN;
    }

    public boolean supports(@NotNull LiquibaseFeature feature) {
        return switch (feature) {
            case SOURCE_SCHEMA, TARGET_SCHEMA, WORKSPACE -> requires(feature);
            case WORKSPACE_CREATION -> operation.isOneOf(
                    GENERATE_CHANGELOG,
                    GENERATE_DIFF_CHANGELOG);
            case SNAPSHOT_ITEMS -> operation.isOneOf(
                GENERATE_CHANGELOG,
                SNAPSHOT_DATABASE);
            case CHANGELOG_AUTHOR -> operation == GENERATE_CHANGELOG;
            case DATABASE_TAG -> operation.isOneOf(
                GENERATE_CHANGELOG,
                GENERATE_DIFF_CHANGELOG,
                TAG_DATABASE);
            case CHANGELOG_TAG -> operation == SYNCHRONIZE_CHANGELOG_TO_TAG;
            case CHECKPOINT_TAG, UPDATE_INSTRUCTION -> operation == UPDATE_DATABASE;
            case ROLLBACK_TAG, ROLLBACK -> operation.isOneOf(
                ROLLBACK_CHANGESETS,
                ROLLBACK_SQL);
            case CHANGESET_ITEMS -> operation.isOneOf(
                UPDATE_DATABASE,
                UPDATE_SQL,
                SHOW_CHANGELOG_HISTORY,
                UNEXPECTED_CHANGESETS,
                UPDATE_TESTING_ROLLBACK,
                MARK_NEXT_CHANGESET_RAN,
                SYNCHRONIZE_CHANGELOG,
                SYNCHRONIZE_CHANGELOG_TO_TAG,
                SYNCHRONIZE_CHANGELOG_SQL,
                ROLLBACK_CHANGESETS,
                ROLLBACK_SQL,
                FUTURE_ROLLBACK,
                CALCULATE_CHECKSUMS);
            case LOCK_ITEMS -> operation == LIST_LOCKS;
            case RERUN_ON_SUCCESS -> operation.isOneOf(
                GENERATE_CHANGELOG,
                GENERATE_DATABASE_DOCUMENTATION,
                SNAPSHOT_DATABASE,
                VALIDATE_CHANGELOG,
                COMPARE_SCHEMAS,
                GENERATE_DIFF_CHANGELOG,
                SHOW_CHANGELOG_STATUS,
                SHOW_CHANGELOG_HISTORY,
                UNEXPECTED_CHANGESETS,
                UPDATE_TESTING_ROLLBACK,
                SYNCHRONIZE_CHANGELOG_SQL,
                UPDATE_SQL,
                FUTURE_ROLLBACK,
                LIST_LOCKS,
                CALCULATE_CHECKSUMS,
                ROLLBACK_SQL);
            case SQL_OUTPUT -> operation.isOneOf(
                UPDATE_SQL,
                SYNCHRONIZE_CHANGELOG_SQL,
                ROLLBACK_SQL,
                FUTURE_ROLLBACK);
            case COMPARISON_ITEMS -> operation.isOneOf(
                COMPARE_SCHEMAS,
                GENERATE_DIFF_CHANGELOG);
            case DISTINCT_SCHEMAS -> operation.isOneOf(
                COMPARE_SCHEMAS,
                GENERATE_DIFF_CHANGELOG);
            case TRACKING_TABLES -> operation.isOneOf(
                SHOW_CHANGELOG_STATUS,
                SHOW_CHANGELOG_HISTORY,
                UNEXPECTED_CHANGESETS,
                SYNCHRONIZE_CHANGELOG,
                UPDATE_DATABASE,
                UPDATE_TESTING_ROLLBACK,
                MARK_NEXT_CHANGESET_RAN,
                TAG_DATABASE,
                SYNCHRONIZE_CHANGELOG_TO_TAG,
                RELEASE_LOCKS,
                CLEAR_CHECKSUMS,
                LIST_LOCKS,
                CALCULATE_CHECKSUMS,
                ROLLBACK_CHANGESETS);
        };
    }

    public boolean requires(@NotNull LiquibaseFeature feature) {
        return switch (feature) {
            case SOURCE_SCHEMA -> getSourceContextState().isVisible();
            case TARGET_SCHEMA -> getTargetContextState().isVisible();
            case WORKSPACE -> operation.isOneOf(
                GENERATE_CHANGELOG,
                GENERATE_DATABASE_DOCUMENTATION,
                VALIDATE_CHANGELOG,
                GENERATE_DIFF_CHANGELOG,
                SHOW_CHANGELOG_STATUS,
                SHOW_CHANGELOG_HISTORY,
                UNEXPECTED_CHANGESETS,
                SYNCHRONIZE_CHANGELOG,
                SYNCHRONIZE_CHANGELOG_SQL,
                UPDATE_DATABASE,
                UPDATE_TESTING_ROLLBACK,
                MARK_NEXT_CHANGESET_RAN,
                ROLLBACK_CHANGESETS,
                SYNCHRONIZE_CHANGELOG_TO_TAG,
                UPDATE_SQL,
                ROLLBACK_SQL,
                FUTURE_ROLLBACK,
                CALCULATE_CHECKSUMS);
            case DATABASE_TAG -> operation == TAG_DATABASE;
            case CHANGELOG_TAG -> operation == SYNCHRONIZE_CHANGELOG_TO_TAG;
            default -> false;
        };
    }
}
