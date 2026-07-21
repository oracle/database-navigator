/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseExecutionContext;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionItemStatus;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.RanChangeSet;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseCommands.SHOW_CHANGELOG_HISTORY;
import static com.dbn.nls.NlsResources.txt;

/**
 * Displays the changesets recorded in the target schema's Liquibase history.
 *
 * <p>The processor is read-only. It uses Liquibase's native {@code history} command for the
 * console output and exposes the recorded changesets as execution items for structured browsing.</p>
 */
public class LiquibaseShowChangelogHistoryProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.SHOW_CHANGELOG_HISTORY;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        prepareChangelogContext(context, true);

        LiquibaseExecutionInput input = context.getInput();
        LiquibaseExecutionResult result = context.getResult();
        LiquibaseWorkspacePaths paths = input.getWorkspacePaths();
        Path changelogFile = paths.getMasterChangelogPath();
        DBSchema targetSchema = context.getTargetSchema();

        withLiquibaseDatabase(context, true, targetSchema, database ->
                withLiquibaseScope(context, contentRootAccessor(context), null,
                        output -> executeHistory(context, database, output)));

        result.appendConsoleOutput(txt("log.liquibase.info.ChangelogHistoryDisplayed", changelogFile));
    }

    private void executeHistory(
            @NotNull LiquibaseExecutionContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        LiquibaseExecutionResult result = context.getResult();
        for (RanChangeSet ranChangeSet : database.getRanChangeSetList()) {
            ChangeSet changeSet = new ChangeSet(
                    ranChangeSet.getId(),
                    ranChangeSet.getAuthor(),
                    false,
                    false,
                    ranChangeSet.getChangeLog(),
                    null,
                    null,
                    null);
            LiquibaseExecutionItemStatus status = switch (ranChangeSet.getExecType()) {
                case FAILED -> LiquibaseExecutionItemStatus.FAILED;
                case SKIPPED -> LiquibaseExecutionItemStatus.SKIPPED;
                default -> LiquibaseExecutionItemStatus.PROCESSED;
            };
            result.ensureChangeSetItem(changeSet, status, ranChangeSet.getDescription());
        }

        executeCommand(SHOW_CHANGELOG_HISTORY, output, Map.of(
                "database", database,
                "showTags", true));
    }
}
