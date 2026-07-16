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
import com.dbn.object.DBSchema;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseCommands.CLEAR_CHECKSUMS;

/** Clears stored Liquibase checksums so they can be recalculated on the next changelog run. */
public class LiquibaseClearChecksumsProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.CLEAR_CHECKSUMS;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        DBSchema targetSchema = context.getTargetSchema();
        withLiquibaseDatabase(context, false, targetSchema, database ->
                withLiquibaseScope(context, classLoaderAccessor(), null,
                        output -> executeClearChecksums(database, output)));
    }

    private void executeClearChecksums(
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        executeCommand(CLEAR_CHECKSUMS, output, Map.of("database", database));
    }
}
