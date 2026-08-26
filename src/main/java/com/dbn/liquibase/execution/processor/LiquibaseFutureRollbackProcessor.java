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
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import static com.dbn.liquibase.execution.LiquibaseCommands.FUTURE_ROLLBACK_SQL;

/** Generates SQL for rolling back Liquibase changesets that have not yet been deployed. */
public class LiquibaseFutureRollbackProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.FUTURE_ROLLBACK;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        prepareChangelogContext(context, true);

        withLiquibaseDatabase(context, true, context.getTargetSchema(), database ->
                withLiquibaseScope(context, contentRootAccessor(context), sqlOutputBuilder(context),
                        output -> executeFutureRollback(context, database, output)));
    }

    private void executeFutureRollback(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {

        var paths = context.getInput().getWorkspacePaths();
        var listener = new LiquibaseChangeSetRollbackListener(context.getResult(), "SQL generated");
        var arguments = arguments(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath(),
                "changeExecListener", listener);
        executeCommand(FUTURE_ROLLBACK_SQL, context, output, arguments);
    }
}
