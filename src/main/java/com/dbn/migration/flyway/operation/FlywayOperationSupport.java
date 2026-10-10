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

package com.dbn.migration.flyway.operation;

import com.dbn.common.ui.form.field.FieldState;
import org.jetbrains.annotations.NotNull;

import static com.dbn.migration.flyway.operation.FlywayOperation.INITIALIZE_PROJECT;
import static com.dbn.migration.flyway.operation.FlywayOperation.SHOW_MIGRATION_INFO;
import static com.dbn.migration.flyway.operation.FlywayOperation.VALIDATE_MIGRATIONS;

/**
 * Defines the context and result capabilities of a Flyway operation.
 */
public final class FlywayOperationSupport implements FlywayFeatureSupport {
    private final FlywayOperation operation;

    FlywayOperationSupport(@NotNull FlywayOperation operation) {
        this.operation = operation;
    }

    @Override
    public FieldState getSchemaContextState() {
        return FieldState.VISIBLE;
    }

    @Override
    public FieldState getWorkspaceState() {
        return requires(FlywayFeature.WORKSPACE) ? FieldState.EDITABLE : FieldState.HIDDEN;
    }

    @Override
    public boolean supports(@NotNull FlywayFeature feature) {
        return switch (feature) {
            case SCHEMA, WORKSPACE -> requires(feature);
            case WORKSPACE_CREATION -> operation == INITIALIZE_PROJECT;
            case MIGRATION_ITEMS ->
                    operation == VALIDATE_MIGRATIONS ||
                    operation == SHOW_MIGRATION_INFO;
            case MIGRATION_INFO -> operation == SHOW_MIGRATION_INFO;
            case RERUN_ON_SUCCESS ->
                    operation == VALIDATE_MIGRATIONS ||
                    operation == SHOW_MIGRATION_INFO;
        };
    }

    @Override
    public boolean requires(@NotNull FlywayFeature feature) {
        return switch (feature) {
            case SCHEMA, WORKSPACE ->
                    operation == INITIALIZE_PROJECT ||
                    operation == VALIDATE_MIGRATIONS ||
                    operation == SHOW_MIGRATION_INFO;
            case WORKSPACE_CREATION -> false;
            case MIGRATION_ITEMS, MIGRATION_INFO, RERUN_ON_SUCCESS -> false;
        };
    }
}
