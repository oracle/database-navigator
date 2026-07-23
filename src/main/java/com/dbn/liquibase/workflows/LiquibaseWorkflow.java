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

import com.dbn.liquibase.execution.LiquibaseOperation;
import lombok.Getter;

import java.util.List;

import static com.dbn.liquibase.execution.LiquibaseOperation.CALCULATE_CHECKSUMS;
import static com.dbn.liquibase.execution.LiquibaseOperation.COMPARE_SCHEMAS;
import static com.dbn.liquibase.execution.LiquibaseOperation.GENERATE_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.GENERATE_DATABASE_DOCUMENTATION;
import static com.dbn.liquibase.execution.LiquibaseOperation.GENERATE_DIFF_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.LIST_LOCKS;
import static com.dbn.liquibase.execution.LiquibaseOperation.ROLLBACK_CHANGESETS;
import static com.dbn.liquibase.execution.LiquibaseOperation.ROLLBACK_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.SHOW_CHANGELOG_HISTORY;
import static com.dbn.liquibase.execution.LiquibaseOperation.SHOW_CHANGELOG_STATUS;
import static com.dbn.liquibase.execution.LiquibaseOperation.UNEXPECTED_CHANGESETS;
import static com.dbn.liquibase.execution.LiquibaseOperation.UPDATE_DATABASE;
import static com.dbn.liquibase.execution.LiquibaseOperation.UPDATE_SQL;
import static com.dbn.liquibase.execution.LiquibaseOperation.VALIDATE_CHANGELOG;
import static com.dbn.nls.NlsResources.txt;

/** Defines a reusable sequence of Liquibase operations. */
@Getter
public enum LiquibaseWorkflow {
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
}
