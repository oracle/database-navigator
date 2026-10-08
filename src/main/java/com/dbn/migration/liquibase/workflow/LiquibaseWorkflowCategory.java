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

package com.dbn.migration.liquibase.workflow;

import com.dbn.migration.shared.workflow.DatabaseMigrationWorkflowCategory;

import static com.dbn.nls.NlsResources.txt;

/**
 * Groups Liquibase workflows by their primary user intent.
 */
public enum LiquibaseWorkflowCategory implements DatabaseMigrationWorkflowCategory {
    PREPARE,
    REVIEW,
    DEPLOY,
    RECOVER;

    public String getName() {
        return txt("app.liquibase.const.WorkflowCategory_" + name());
    }
}
