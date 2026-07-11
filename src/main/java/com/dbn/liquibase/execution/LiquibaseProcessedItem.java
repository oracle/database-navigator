package com.dbn.liquibase.execution;

import lombok.Getter;

/** Structured changeset or operation item reported by Liquibase. */
@Getter
public class LiquibaseProcessedItem {
    private final String id;
    private final String author;
    private final String filePath;
    private final String changeType;
    private final String status;
    private final String message;

    public LiquibaseProcessedItem(String id, String author, String filePath, String changeType, String status, String message) {
        this.id = id;
        this.author = author;
        this.filePath = filePath;
        this.changeType = changeType;
        this.status = status;
        this.message = message;
    }
}
