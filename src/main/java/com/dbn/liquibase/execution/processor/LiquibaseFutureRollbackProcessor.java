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
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseCommands.FUTURE_ROLLBACK_SQL;

/** Generates SQL for rolling back Liquibase changesets that have not yet been deployed. */
public class LiquibaseFutureRollbackProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.FUTURE_ROLLBACK;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        prepareChangelogContext(context, true);

        withLiquibaseDatabase(context, true, context.getTargetSchema(), database ->
                withLiquibaseScope(context, contentRootAccessor(context), sqlOutputBuilder(context),
                        output -> executeFutureRollback(context, database, output)));
    }

    private void executeFutureRollback(
            @NotNull LiquibaseExecutionContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        var paths = context.getInput().getWorkspacePaths();
        executeCommand(FUTURE_ROLLBACK_SQL, output, Map.of(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath(),
                "changeExecListener", new LiquibaseChangeSetRollbackListener(context.getResult(), "SQL generated")));
    }
}
