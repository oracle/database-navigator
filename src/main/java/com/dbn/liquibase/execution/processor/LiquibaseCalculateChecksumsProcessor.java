/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseChangeSetItem;
import com.dbn.liquibase.execution.LiquibaseExecutionContext;
import com.dbn.liquibase.execution.LiquibaseExecutionItemStatus;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ResourceAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseCommands.CALCULATE_CHECKSUM;
import static com.dbn.nls.NlsResources.txt;

/** Calculates the current Liquibase checksum for every changeset in the workspace changelog. */
public class LiquibaseCalculateChecksumsProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.CALCULATE_CHECKSUMS;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        prepareChangelogContext(context, true);

        DBSchema targetSchema = context.getTargetSchema();
        withLiquibaseDatabase(context, true, targetSchema, database ->
                withLiquibaseScope(context, contentRootAccessor(context), null,
                        output -> executeCalculateChecksums(context, database, output)));
    }

    private void executeCalculateChecksums(
            @NotNull LiquibaseExecutionContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        ResourceAccessor resourceAccessor = contentRootAccessor(context);
        DatabaseChangeLog changeLog = ChangeLogParserFactory.getInstance()
                .getParser(paths.getMasterChangelogRelativePath(), resourceAccessor)
                .parse(paths.getMasterChangelogRelativePath(), new ChangeLogParameters(database), resourceAccessor);

        LiquibaseExecutionResult result = context.getResult();
        List<LiquibaseChangeSetItem> items = new ArrayList<>();
        for (ChangeSet changeSet : changeLog.getChangeSets()) {
            checkCanceled(context);
            LiquibaseChangeSetItem item = result.ensureChangeSetItem(
                    changeSet,
                    LiquibaseExecutionItemStatus.PROCESSING,
                    txt("msg.liquibase.text.ChangeSetChecksumPending"));
            items.add(item);

            executeCommand(CALCULATE_CHECKSUM, output, Map.of(
                    "database", database,
                    "changelogFile", paths.getMasterChangelogRelativePath(),
                    "changesetIdentifier", changeSet.toString()));

            item.updateStatus(LiquibaseExecutionItemStatus.EXECUTED,
                    txt("msg.liquibase.text.ChangeSetChecksumCalculated"));
        }
        if (!items.isEmpty()) result.notifyItemsChanged();
    }
}
