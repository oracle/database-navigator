package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseExecutionItemStatus;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import static com.dbn.liquibase.execution.LiquibaseExecutionItemStatus.FAILED;
import static com.dbn.liquibase.execution.LiquibaseExecutionItemStatus.PROCESSED;
import static com.dbn.liquibase.execution.LiquibaseExecutionItemStatus.SKIPPED;

/** Tracks changeset execution callbacks emitted by update-style Liquibase commands. */
final class LiquibaseChangeSetRunListener extends LiquibaseChangeSetListener {
    LiquibaseChangeSetRunListener(@NotNull LiquibaseExecutionResult result) {
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
        finishProcessing(changeSet, getStatus(execType), execType.value);
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
            case SKIPPED -> SKIPPED;
            case FAILED -> FAILED;
            default -> PROCESSED;
        };
    }
}
