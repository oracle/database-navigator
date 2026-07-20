/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseExecutionContext;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.object.DBSchema;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseCommands.LIST_LOCKS;

/** Lists Liquibase changelog locks currently held by the selected schema. */
public class LiquibaseListLocksProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.LIST_LOCKS;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        DBSchema targetSchema = context.getTargetSchema();
        withLiquibaseDatabase(context, true, targetSchema, database ->
                withLiquibaseScope(context, classLoaderAccessor(), null,
                        output -> executeListLocks(database, output)));
    }

    private void executeListLocks(
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        executeCommand(LIST_LOCKS, output, Map.of("database", database));
    }
}
