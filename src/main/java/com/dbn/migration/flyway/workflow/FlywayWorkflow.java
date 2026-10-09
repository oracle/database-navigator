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

package com.dbn.migration.flyway.workflow;

import com.dbn.common.constant.Constant;
import com.dbn.migration.flyway.operation.FlywayOperation;
import com.dbn.migration.shared.workflow.DatabaseMigrationWorkflow;
import lombok.Getter;

import java.util.List;

import static com.dbn.migration.flyway.operation.FlywayOperation.ADD_MIGRATION;
import static com.dbn.migration.flyway.operation.FlywayOperation.APPLY_MIGRATIONS;
import static com.dbn.migration.flyway.operation.FlywayOperation.BASELINE_DATABASE;
import static com.dbn.migration.flyway.operation.FlywayOperation.INITIALIZE_PROJECT;
import static com.dbn.migration.flyway.operation.FlywayOperation.REPAIR_SCHEMA_HISTORY;
import static com.dbn.migration.flyway.operation.FlywayOperation.SHOW_MIGRATION_INFO;
import static com.dbn.migration.flyway.operation.FlywayOperation.VALIDATE_MIGRATIONS;
import static com.dbn.migration.flyway.workflow.FlywayWorkflowCategory.DEPLOYMENT;
import static com.dbn.migration.flyway.workflow.FlywayWorkflowCategory.MAINTENANCE;
import static com.dbn.migration.flyway.workflow.FlywayWorkflowCategory.PREPARATION;
import static com.dbn.nls.NlsResources.txt;

/**
 * Defines a reusable sequence of Flyway operations.
 */
@Getter
public enum FlywayWorkflow implements Constant<FlywayWorkflow>, DatabaseMigrationWorkflow {
    PREPARE_PROJECT(PREPARATION,
            INITIALIZE_PROJECT,
            ADD_MIGRATION),
    VALIDATE_AND_MIGRATE(DEPLOYMENT,
            VALIDATE_MIGRATIONS,
            SHOW_MIGRATION_INFO,
            APPLY_MIGRATIONS),
    BASELINE_AND_MIGRATE(DEPLOYMENT,
            BASELINE_DATABASE,
            VALIDATE_MIGRATIONS,
            APPLY_MIGRATIONS),
    REPAIR_AND_VERIFY(MAINTENANCE,
            REPAIR_SCHEMA_HISTORY,
            VALIDATE_MIGRATIONS,
            SHOW_MIGRATION_INFO);

    private final FlywayWorkflowCategory category;
    private final List<FlywayOperation> operations;

    FlywayWorkflow(
            FlywayWorkflowCategory category,
            FlywayOperation... operations) {
        this.category = category;
        this.operations = List.of(operations);
    }

    @Override
    public String getName() {
        return txt("app.flyway.const.Workflow_" + name());
    }

    @Override
    public String getTitle() {
        return txt("app.flyway.title.Workflow_" + name());
    }

    @Override
    public String getDescription() {
        return txt("app.flyway.text.WorkflowDescription_" + name());
    }

    @Override
    public String getHint() {
        return txt("app.flyway.hint.Workflow_" + name());
    }

    @Override
    public String getDashboardName() {
        return txt("app.flyway.action.Workflow_" + name());
    }

    @Override
    public String getDashboardDescription() {
        return getHint();
    }
}
