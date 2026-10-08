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

/** Tracks changeset rollback callbacks emitted by rollback-style Liquibase commands. */
final class LiquibaseChangeSetRollbackListener extends LiquibaseChangeSetListener {
    private final String completionMessage;

    LiquibaseChangeSetRollbackListener(
            @NotNull LiquibaseOperationResult result,
            @NotNull String completionMessage) {
        super(result);
        this.completionMessage = completionMessage;
    }

    @Override
    public void willRollback(ChangeSet changeSet, DatabaseChangeLog changeLog, Database database) {
        startProcessing(changeSet);
    }

    @Override
    public void rolledBack(ChangeSet changeSet, DatabaseChangeLog changeLog, Database database) {
        finishProcessing(changeSet, LiquibaseExecutionItemStatus.PROCESSED, completionMessage);
    }

    @Override
    public void rollbackFailed(
            ChangeSet changeSet,
            DatabaseChangeLog changeLog,
            Database database,
            Exception exception) {
        failProcessing(changeSet, exception);
    }
}
