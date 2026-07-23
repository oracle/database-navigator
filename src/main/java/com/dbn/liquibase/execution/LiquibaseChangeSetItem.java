package com.dbn.liquibase.execution;

import liquibase.change.CheckSum;
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
    private String calculatedChecksum;
    private String storedChecksum;
    private LiquibaseChecksumStatus checksumStatus;

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

    public void updateChecksum(
            @NotNull CheckSum calculatedChecksum,
            @Nullable CheckSum storedChecksum,
            boolean executed) {
        this.calculatedChecksum = calculatedChecksum.toString();
        this.storedChecksum = storedChecksum == null ? null : storedChecksum.toString();
        if (!executed) {
            this.checksumStatus = LiquibaseChecksumStatus.NOT_EXECUTED;
        } else if (storedChecksum == null) {
            this.checksumStatus = LiquibaseChecksumStatus.NOT_RECORDED;
        } else if (calculatedChecksum.equals(storedChecksum)) {
            this.checksumStatus = LiquibaseChecksumStatus.MATCHING;
        } else {
            this.checksumStatus = LiquibaseChecksumStatus.CHANGED;
        }
    }
}
