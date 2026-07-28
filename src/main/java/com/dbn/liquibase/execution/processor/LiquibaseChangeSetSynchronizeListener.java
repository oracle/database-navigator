/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseExecutionItemStatus;
import com.dbn.liquibase.operation.LiquibaseOperationResult;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

/** Tracks changesets marked as executed by a changelog synchronization operation. */
final class LiquibaseChangeSetSynchronizeListener extends LiquibaseChangeSetListener {
    LiquibaseChangeSetSynchronizeListener(@NotNull LiquibaseOperationResult result) {
        super(result);
    }

    @Override
    public void willRun(
            ChangeSet changeSet,
            DatabaseChangeLog changeLog,
            Database database,
            ChangeSet.RunStatus runStatus) {
        startProcessing(changeSet);
    }

    @Override
    public void ran(
            ChangeSet changeSet,
            DatabaseChangeLog changeLog,
            Database database,
            ChangeSet.ExecType execType) {
        finishProcessing(
                changeSet,
                getStatus(execType),
                txt("msg.liquibase.text.ChangeSetSynchronized", execType.value));
    }

    @Override
    public void runFailed(
            ChangeSet changeSet,
            DatabaseChangeLog changeLog,
            Database database,
            Exception exception) {
        failProcessing(changeSet, exception);
    }

    @NotNull
    private static LiquibaseExecutionItemStatus getStatus(@NotNull ChangeSet.ExecType execType) {
        return switch (execType) {
            case SKIPPED -> LiquibaseExecutionItemStatus.SKIPPED;
            case FAILED -> LiquibaseExecutionItemStatus.FAILED;
            default -> LiquibaseExecutionItemStatus.PROCESSED;
        };
    }
}
