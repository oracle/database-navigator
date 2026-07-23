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
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.liquibase.operation.LiquibaseOperationContext;
import com.dbn.liquibase.operation.LiquibaseOperationResult;
import com.dbn.liquibase.workspace.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseCommands.SYNCHRONIZE_CHANGELOG;
import static com.dbn.nls.NlsResources.txt;

/** Marks workspace changesets as executed without applying their database changes. */
public class LiquibaseSynchronizeChangelogProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.SYNCHRONIZE_CHANGELOG;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        prepareChangelogContext(context, true);

        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        LiquibaseOperationResult result = context.getResult();
        Path changelogFile = paths.getMasterChangelogPath();
        DBSchema targetSchema = context.getTargetSchema();

        withLiquibaseDatabase(context, false, targetSchema, database ->
                withLiquibaseScope(context, contentRootAccessor(context), null,
                        output -> executeSynchronize(context, database, output)));

        notifySchemaObjectChanges(targetSchema);
        result.appendConsoleOutput(txt("log.liquibase.info.ChangelogSynchronized", changelogFile));
    }

    private void executeSynchronize(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        LiquibaseOperationResult result = context.getResult();
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        List<LiquibaseChangeSetItem> items = discoverChangeSetItems(
                context,
                database,
                changeSet -> txt("msg.liquibase.text.ChangeSetSyncPending"));

        executeCommand(SYNCHRONIZE_CHANGELOG, output, Map.of(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath(),
                "changeExecListener", new LiquibaseChangeSetSynchronizeListener(result)));

        completeChangeSetItems(
                context,
                items,
                item -> txt("msg.liquibase.text.ChangeSetSynchronized", "MARK_RAN"));
    }
}
