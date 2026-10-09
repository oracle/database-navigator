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
            case WORKSPACE_CREATION -> operation == FlywayOperation.INITIALIZE_PROJECT;
            case MIGRATION_ITEMS -> operation == FlywayOperation.VALIDATE_MIGRATIONS;
            case RERUN_ON_SUCCESS -> operation == FlywayOperation.VALIDATE_MIGRATIONS;
        };
    }

    @Override
    public boolean requires(@NotNull FlywayFeature feature) {
        return switch (feature) {
            case SCHEMA, WORKSPACE -> operation == FlywayOperation.INITIALIZE_PROJECT ||
                    operation == FlywayOperation.VALIDATE_MIGRATIONS;
            case WORKSPACE_CREATION -> false;
            case MIGRATION_ITEMS, RERUN_ON_SUCCESS -> false;
        };
    }
}
