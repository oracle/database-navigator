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

import com.dbn.common.constant.Constant;
import com.dbn.migration.shared.operation.DatabaseMigrationOperation;
import lombok.Getter;

import static com.dbn.migration.flyway.operation.FlywayOperationCategory.DEPLOYMENT;
import static com.dbn.migration.flyway.operation.FlywayOperationCategory.INSPECTION;
import static com.dbn.migration.flyway.operation.FlywayOperationCategory.MAINTENANCE;
import static com.dbn.migration.flyway.operation.FlywayOperationCategory.PROJECT;
import static com.dbn.nls.NlsResources.txt;

/**
 * Flyway operation metadata used by DBN migration actions and dashboards.
 *
 * <p>The command name remains the Flyway command identifier while the enum name and
 * localized presentation text describe the operation in DBN terms.</p>
 */
@Getter
public enum FlywayOperation implements Constant<FlywayOperation>, DatabaseMigrationOperation {
    INITIALIZE_PROJECT("init", PROJECT, true, false),
    ADD_MIGRATION("add", PROJECT, true, false),
    SHOW_MIGRATION_INFO("info", INSPECTION, false, false),
    VALIDATE_MIGRATIONS("validate", INSPECTION, false, false),
    APPLY_MIGRATIONS("migrate", DEPLOYMENT, true, false),
    BASELINE_DATABASE("baseline", DEPLOYMENT, true, false),
    REPAIR_SCHEMA_HISTORY("repair", MAINTENANCE, true, false),
    CLEAN_DATABASE("clean", MAINTENANCE, true, true);

    private final String command;
    private final FlywayOperationCategory category;
    private final boolean mutating;
    private final boolean destructive;

    FlywayOperation(
            String command,
            FlywayOperationCategory category,
            boolean mutating,
            boolean destructive) {
        this.command = command;
        this.category = category;
        this.mutating = mutating;
        this.destructive = destructive;
    }

    @Override
    public String getName() {
        return txt("app.flyway.const.Operation_" + name());
    }

    public String getDescription() {
        return txt("app.flyway.text.OperationDescription_" + name());
    }

    public String getHint() {
        return txt("app.flyway.hint.Operation_" + name());
    }

    @Override
    public String getDashboardName() {
        return getName();
    }

    @Override
    public String getDashboardDescription() {
        return getHint();
    }
}
