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

package com.dbn.liquibase.workflow;

import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.liquibase.task.LiquibaseTask;
import lombok.Getter;

import java.util.List;

import static com.dbn.liquibase.operation.LiquibaseOperation.CALCULATE_CHECKSUMS;
import static com.dbn.liquibase.operation.LiquibaseOperation.COMPARE_SCHEMAS;
import static com.dbn.liquibase.operation.LiquibaseOperation.GENERATE_CHANGELOG;
import static com.dbn.liquibase.operation.LiquibaseOperation.GENERATE_DATABASE_DOCUMENTATION;
import static com.dbn.liquibase.operation.LiquibaseOperation.GENERATE_DIFF_CHANGELOG;
import static com.dbn.liquibase.operation.LiquibaseOperation.LIST_LOCKS;
import static com.dbn.liquibase.operation.LiquibaseOperation.ROLLBACK_CHANGESETS;
import static com.dbn.liquibase.operation.LiquibaseOperation.ROLLBACK_SQL;
import static com.dbn.liquibase.operation.LiquibaseOperation.SHOW_CHANGELOG_HISTORY;
import static com.dbn.liquibase.operation.LiquibaseOperation.SHOW_CHANGELOG_STATUS;
import static com.dbn.liquibase.operation.LiquibaseOperation.UNEXPECTED_CHANGESETS;
import static com.dbn.liquibase.operation.LiquibaseOperation.UPDATE_DATABASE;
import static com.dbn.liquibase.operation.LiquibaseOperation.UPDATE_SQL;
import static com.dbn.liquibase.operation.LiquibaseOperation.VALIDATE_CHANGELOG;
import static com.dbn.nls.NlsResources.txt;

/** Defines a reusable sequence of Liquibase operations. */
@Getter
public enum LiquibaseWorkflow implements LiquibaseTask {
    VALIDATE_AND_APPLY(
            VALIDATE_CHANGELOG,
            SHOW_CHANGELOG_STATUS,
            UPDATE_SQL,
            UPDATE_DATABASE),
    COMPARE_AND_GENERATE(
            COMPARE_SCHEMAS,
            GENERATE_DIFF_CHANGELOG),
    COMPARE_GENERATE_AND_APPLY(
            COMPARE_SCHEMAS,
            GENERATE_DIFF_CHANGELOG,
            VALIDATE_CHANGELOG,
            UPDATE_DATABASE),
    GENERATE_AND_DOCUMENT(
            GENERATE_CHANGELOG,
            VALIDATE_CHANGELOG,
            GENERATE_DATABASE_DOCUMENTATION),
    ROLLBACK_SAFELY(
            SHOW_CHANGELOG_HISTORY,
            ROLLBACK_SQL,
            ROLLBACK_CHANGESETS),
    DIAGNOSE_DATABASE(
            VALIDATE_CHANGELOG,
            SHOW_CHANGELOG_STATUS,
            UNEXPECTED_CHANGESETS,
            CALCULATE_CHECKSUMS,
            LIST_LOCKS);

    private final List<LiquibaseOperation> operations;
    private final LiquibaseWorkflowSupport support;

    LiquibaseWorkflow(LiquibaseOperation... operations) {
        this.operations = List.of(operations);
        this.support = new LiquibaseWorkflowSupport(this);
    }

    public boolean includesOperation(LiquibaseOperation operation) {
        return operations.contains(operation);
    }

    public String getTitle() {
        return txt("app.liquibase.title.Workflow_" + name());
    }

    public String getDescription() {
        return txt("app.liquibase.text.WorkflowDescription_" + name());
    }

    public String getHint() {
        return txt("app.liquibase.hint.Workflow_" + name());
    }

    @Override
    public String getDashboardName() {
        return txt("app.liquibase.action.Workflow_" + name());
    }

    @Override
    public String getDashboardDescription() {
        return getHint();
    }

}
