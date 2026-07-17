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

import com.dbn.liquibase.execution.LiquibaseChangeSetItem;
import com.dbn.liquibase.execution.LiquibaseExecutionContext;
import com.dbn.liquibase.execution.LiquibaseExecutionItemStatus;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseCommands.MARK_NEXT_CHANGESET_RAN;
import static com.dbn.nls.NlsResources.txt;

/** Marks the next pending Liquibase changeset as executed without applying its changes. */
public class LiquibaseMarkNextChangesetRanProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.MARK_NEXT_CHANGESET_RAN;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        prepareChangelogContext(context, true);

        withLiquibaseDatabase(context, false, context.getTargetSchema(), database ->
                withLiquibaseScope(context, contentRootAccessor(context), null,
                        output -> executeMarkNext(context, database, output)));
    }

    private void executeMarkNext(
            @NotNull LiquibaseExecutionContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        LiquibaseExecutionResult result = context.getResult();
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        List<ChangeSet> pending = discoverPendingChangeSets(context, database);
        LiquibaseChangeSetItem item = pending.isEmpty() ? null : result.ensureChangeSetItem(
                pending.get(0),
                LiquibaseExecutionItemStatus.DISCOVERED,
                txt("msg.liquibase.text.ChangeSetMarkNextPending"));

        executeCommand(MARK_NEXT_CHANGESET_RAN, output, Map.of(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath()));

        if (item != null) {
            completeChangeSetItems(
                    context,
                    List.of(item),
                    changeSetItem -> txt("msg.liquibase.text.ChangeSetMarkedRan", "MARK_RAN"));
            result.appendConsoleOutput(txt("log.liquibase.info.ChangeSetMarkedRan", item.getId()));
        }
    }
}
