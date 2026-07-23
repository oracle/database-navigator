/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseChangeSetItem;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.liquibase.operation.LiquibaseOperationContext;
import com.dbn.liquibase.operation.LiquibaseOperationResult;
import com.dbn.liquibase.workspace.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import liquibase.change.CheckSum;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.RanChangeSet;
import liquibase.command.CommandResults;
import liquibase.command.core.CalculateChecksumCommandStep;
import liquibase.database.Database;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ResourceAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseCommands.CALCULATE_CHECKSUM;
import static com.dbn.liquibase.execution.LiquibaseExecutionItemStatus.PROCESSED;
import static com.dbn.liquibase.execution.LiquibaseExecutionItemStatus.PROCESSING;
import static com.dbn.nls.NlsResources.txt;

/** Calculates changelog checksums and compares them with the values recorded in the database. */
public class LiquibaseCalculateChecksumsProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.CALCULATE_CHECKSUMS;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        prepareChangelogContext(context, true);

        DBSchema targetSchema = context.getTargetSchema();
        withLiquibaseDatabase(context, true, targetSchema, database ->
                withLiquibaseScope(context, contentRootAccessor(context), null,
                        output -> executeCalculateChecksums(context, database, output)));
    }

    private void executeCalculateChecksums(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        ResourceAccessor resourceAccessor = contentRootAccessor(context);
        DatabaseChangeLog changeLog = ChangeLogParserFactory.getInstance()
                .getParser(paths.getMasterChangelogRelativePath(), resourceAccessor)
                .parse(paths.getMasterChangelogRelativePath(), new ChangeLogParameters(database), resourceAccessor);

        LiquibaseOperationResult result = context.getResult();
        List<LiquibaseChangeSetItem> items = new ArrayList<>();
        for (ChangeSet changeSet : changeLog.getChangeSets()) {
            checkCanceled(context);
            LiquibaseChangeSetItem item = result.ensureChangeSetItem(
                    changeSet,
                    PROCESSING,
                    txt("msg.liquibase.text.ChangeSetChecksumPending"));
            items.add(item);
            item.startProcessing();

            CommandResults commandResults = executeCommand(CALCULATE_CHECKSUM, output, Map.of(
                    "database", database,
                    "changelogFile", paths.getMasterChangelogRelativePath(),
                    "changesetIdentifier", changeSet.toString()));

            CheckSum calculatedChecksum = commandResults.getResult(CalculateChecksumCommandStep.CHECKSUM_RESULT);
            RanChangeSet ranChangeSet = database.getRanChangeSet(changeSet);
            boolean executed = ranChangeSet != null;
            CheckSum storedChecksum = executed ? ranChangeSet.getLastCheckSum() : null;
            item.updateChecksum(calculatedChecksum, storedChecksum, executed);
            item.finishProcessing();
            item.updateStatus(
                    PROCESSED,
                    getChecksumMessage(item));
        }
        if (!items.isEmpty()) result.notifyItemsChanged();
    }

    @NotNull
    private static String getChecksumMessage(@NotNull LiquibaseChangeSetItem item) {
        return switch (item.getChecksumStatus()) {
            case MATCHING -> txt("msg.liquibase.text.ChangeSetChecksumMatching");
            case CHANGED -> txt("msg.liquibase.text.ChangeSetChecksumChanged");
            case NOT_EXECUTED -> txt("msg.liquibase.text.ChangeSetChecksumNotExecuted");
            case NOT_RECORDED -> txt("msg.liquibase.text.ChangeSetChecksumNotRecorded");
        };
    }
}
