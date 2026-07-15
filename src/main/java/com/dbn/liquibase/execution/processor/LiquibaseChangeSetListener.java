package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseChangeSetItem;
import com.dbn.liquibase.execution.LiquibaseExecutionItemStatus;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.visitor.AbstractChangeExecListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Shared changeset-item lifecycle handling for Liquibase execution listeners. */
abstract class LiquibaseChangeSetListener extends AbstractChangeExecListener {
    protected final LiquibaseExecutionResult result;

    LiquibaseChangeSetListener(@NotNull LiquibaseExecutionResult result) {
        this.result = result;
    }

    protected final void startProcessing(@NotNull ChangeSet changeSet) {
        LiquibaseChangeSetItem item = result.ensureChangeSetItem(changeSet);
        item.startProcessing();
        result.notifyItemsChanged();
    }

    protected final void finishProcessing(
            @NotNull ChangeSet changeSet,
            @NotNull LiquibaseExecutionItemStatus status,
            @Nullable String message) {
        LiquibaseChangeSetItem item = result.ensureChangeSetItem(changeSet);
        item.finishProcessing();
        item.updateStatus(status, message);
        result.notifyItemsChanged();
    }

    protected final void failProcessing(@NotNull ChangeSet changeSet, @NotNull Exception exception) {
        finishProcessing(changeSet, LiquibaseExecutionItemStatus.FAILED, exception.getMessage());
    }
}
