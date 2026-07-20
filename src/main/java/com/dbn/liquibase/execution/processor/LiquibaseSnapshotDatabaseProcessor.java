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
import com.dbn.object.DBSchema;
import org.jetbrains.annotations.NotNull;

/**
 * Captures a Liquibase snapshot of the selected target schema without changing the database or
 * requiring a workspace changelog. Each object reported by Liquibase is added to the execution
 * result as it is discovered so the snapshot table can be inspected while the operation runs.
 */
public class LiquibaseSnapshotDatabaseProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.SNAPSHOT_DATABASE;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        DBSchema targetSchema = context.getTargetSchema();
        withLiquibaseDatabase(context, true, targetSchema, database ->
                withLiquibaseScope(context, classLoaderAccessor(), null,
                        output -> collectDatabaseObjects(context, database, targetSchema.getName())));
    }
}
