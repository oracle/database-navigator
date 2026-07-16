package com.dbn.liquibase.execution;

import liquibase.changelog.ChangeSet;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** ChangeSet processed by a Liquibase changelog operation. */
@Getter
public class LiquibaseChangeSetItem extends LiquibaseExecutionItem {
    private static final String DEFAULT_MESSAGE = "Processing changeset";

    private final ChangeSet changeSet;
    private final String id;
    private final String author;
    private final String filePath;
    private final String description;

    public LiquibaseChangeSetItem(@NotNull ChangeSet changeSet) {
        this(changeSet, DEFAULT_STATUS, DEFAULT_MESSAGE);
    }

    public LiquibaseChangeSetItem(
            @NotNull ChangeSet changeSet,
            @NotNull LiquibaseExecutionItemStatus status,
            @Nullable String message) {
        super(status, message);
        this.changeSet = changeSet;
        this.id = changeSet.getId();
        this.author = changeSet.getAuthor();
        this.filePath = changeSet.getFilePath();
        this.description = changeSet.getDescription();
    }

    @NotNull
    public String getKey() {
        return filePath + ':' + author + ':' + id;
    }
}
