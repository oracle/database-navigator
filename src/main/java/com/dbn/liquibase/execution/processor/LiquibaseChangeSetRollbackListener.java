package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseExecutionItemStatus;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

/** Tracks changeset rollback callbacks emitted by rollback-style Liquibase commands. */
final class LiquibaseChangeSetRollbackListener extends LiquibaseChangeSetListener {
    private final String completionMessage;

    LiquibaseChangeSetRollbackListener(
            @NotNull LiquibaseExecutionResult result,
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
