package com.dbn.liquibase.execution;

import com.dbn.common.util.ExecutionTiming;
import liquibase.structure.DatabaseObject;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/** Database object discovered or processed by a Liquibase operation. */
@Getter
public class LiquibaseExecutionItem {
    public static final String DEFAULT_STATUS = "processing";
    public static final String DEFAULT_MESSAGE = "Reading database object";

    private DatabaseObject databaseObject;
    private String status;
    private String message;
    private final ExecutionTiming timing = new ExecutionTiming();

    public LiquibaseExecutionItem(@NotNull DatabaseObject databaseObject) {
        this(databaseObject, DEFAULT_STATUS, DEFAULT_MESSAGE);
    }

    public LiquibaseExecutionItem(@NotNull DatabaseObject databaseObject, @NotNull String status, @Nullable String message) {
        this.databaseObject = databaseObject;
        this.status = status;
        this.message = message;
    }

    public void update(@NotNull DatabaseObject databaseObject, @NotNull String status, @Nullable String message) {
        this.databaseObject = databaseObject;
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
