package com.dbn.liquibase.execution;

import com.dbn.common.util.ExecutionTiming;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/** Shared lifecycle state for an item processed by a Liquibase operation. */
@Getter
public abstract class LiquibaseExecutionItem {
    public static final LiquibaseExecutionItemStatus DEFAULT_STATUS = LiquibaseExecutionItemStatus.PROCESSING;

    private LiquibaseExecutionItemStatus status;
    private String message;
    private final ExecutionTiming timing = new ExecutionTiming();

    protected LiquibaseExecutionItem(
            @NotNull LiquibaseExecutionItemStatus status,
            @Nullable String message) {
        this.status = status;
        this.message = message;
    }

    public void updateStatus(
            @NotNull LiquibaseExecutionItemStatus status,
            @Nullable String message) {
        this.status = status;
        this.message = message;
    }

    public void startProcessing() {
        timing.start();
    }

    public void finishProcessing() {
        timing.finish();
    }

    @NotNull
    public Duration getProcessingDuration() {
        return timing.getDuration();
    }
}
