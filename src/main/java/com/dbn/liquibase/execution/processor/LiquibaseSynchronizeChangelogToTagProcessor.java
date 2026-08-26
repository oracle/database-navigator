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

import static com.dbn.liquibase.execution.LiquibaseCommands.SYNCHRONIZE_CHANGELOG_TO_TAG;
import static com.dbn.nls.NlsResources.txt;

/** Marks workspace changesets up to a changelog tag as executed without applying their changes. */
public class LiquibaseSynchronizeChangelogToTagProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.SYNCHRONIZE_CHANGELOG_TO_TAG;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        prepareChangelogContext(context, true);

        LiquibaseOperationResult result = context.getResult();
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        Path changelogFile = paths.getMasterChangelogPath();
        DBSchema targetSchema = context.getTargetSchema();
        String tag = context.getInput().getChangelogTag();

        withLiquibaseDatabase(context, false, targetSchema, database ->
                withLiquibaseScope(context, contentRootAccessor(context), null,
                        output -> executeSynchronize(context, database, output, tag)));

        notifySchemaObjectChanges(targetSchema);
        rememberTag(context, targetSchema, tag);
        result.appendConsoleOutput(txt("log.liquibase.info.ChangelogSynchronized", changelogFile));
    }

    private void executeSynchronize(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output,
            @NotNull String tag) throws Exception {
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();

        var arguments = arguments(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath(),
                "tag", tag,
                "changeExecListener", new LiquibaseChangeSetSynchronizeListener(context.getResult()));
        executeCommand(SYNCHRONIZE_CHANGELOG_TO_TAG, context, output, arguments);
    }
}
