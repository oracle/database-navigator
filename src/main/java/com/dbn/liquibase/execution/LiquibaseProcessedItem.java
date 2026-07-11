package com.dbn.liquibase.execution;

import liquibase.structure.DatabaseObject;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Database object discovered or processed by a Liquibase operation. */
@Getter
public class LiquibaseProcessedItem {
    public static final String DEFAULT_STATUS = "processing";
    public static final String DEFAULT_MESSAGE = "Reading database object";

    private DatabaseObject databaseObject;
    private String status;
    private String message;

    public LiquibaseProcessedItem(@NotNull DatabaseObject databaseObject) {
        this(databaseObject, DEFAULT_STATUS, DEFAULT_MESSAGE);
    }

    public LiquibaseProcessedItem(@NotNull DatabaseObject databaseObject, @NotNull String status, @Nullable String message) {
        this.databaseObject = databaseObject;
        this.status = status;
        this.message = message;
    }

    public void update(@NotNull DatabaseObject databaseObject, @NotNull String status, @Nullable String message) {
        this.databaseObject = databaseObject;
        this.status = status;
        this.message = message;
    }
}
