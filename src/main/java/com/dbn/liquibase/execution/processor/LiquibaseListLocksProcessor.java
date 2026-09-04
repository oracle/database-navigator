/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.liquibase.operation.LiquibaseOperationContext;
import com.dbn.object.DBSchema;
import liquibase.command.core.ListLocksCommandStep;
import liquibase.database.Database;
import liquibase.lockservice.DatabaseChangeLogLock;
import org.jetbrains.annotations.NotNull;

import static com.dbn.liquibase.execution.LiquibaseCommands.LIST_LOCKS;

/** Lists Liquibase changelog locks currently held by the selected schema. */
public class LiquibaseListLocksProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.LIST_LOCKS;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        DBSchema targetSchema = context.getTargetSchema();
        withLiquibaseDatabase(context, true, targetSchema, database ->
                withLiquibaseScope(context, classLoaderAccessor(), null,
                        output -> executeListLocks(database, context, output)));
    }

    private void executeListLocks(
            @NotNull Database database,
            @NotNull LiquibaseOperationContext context,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {

        var arguments = arguments("database", database);
        executeCommand(LIST_LOCKS, context, output, arguments);
        for (DatabaseChangeLogLock lock : ListLocksCommandStep.listLocks(database)) {
            checkCanceled(context);
            context.getResult().ensureLockItem(lock);
        }
        context.getResult().notifyItemsChanged();
    }
}
