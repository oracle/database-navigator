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
import com.dbn.object.DBSchema;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import static com.dbn.liquibase.execution.LiquibaseCommands.UPDATE_TESTING_ROLLBACK;

/**
 * Tests the rollback definitions of all pending changesets in the selected workspace.
 *
 * <p>Liquibase performs an update, rolls the changesets back sequentially, and performs
 * the update again. The operation is exposed as a single processor because Liquibase
 * owns the ordering and failure semantics of the three phases.</p>
 */
public class LiquibaseUpdateTestingRollbackProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.UPDATE_TESTING_ROLLBACK;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        prepareChangelogContext(context, true);

        DBSchema targetSchema = context.getTargetSchema();
        withLiquibaseDatabase(context, false, targetSchema, database ->
                withLiquibaseScope(context, contentRootAccessor(context), null,
                        output -> executeUpdateTestingRollback(context, database, output)));
    }

    private void executeUpdateTestingRollback(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {

        var paths = context.getInput().getWorkspacePaths();
        var listener = new LiquibaseChangeSetTestingRollbackListener(context.getResult());
        var arguments = arguments(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath(),
                "changeExecListener", listener);
        executeCommand(UPDATE_TESTING_ROLLBACK, context, output, arguments);
    }
}
