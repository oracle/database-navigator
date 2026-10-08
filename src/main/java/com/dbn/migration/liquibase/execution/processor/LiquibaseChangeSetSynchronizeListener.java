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

package com.dbn.migration.liquibase.execution.processor;

import com.dbn.migration.liquibase.execution.LiquibaseExecutionItemStatus;
import com.dbn.migration.liquibase.operation.LiquibaseOperationResult;
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
