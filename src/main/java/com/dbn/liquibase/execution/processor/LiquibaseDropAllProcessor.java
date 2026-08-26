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
import liquibase.CatalogAndSchema;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import static com.dbn.liquibase.execution.LiquibaseCommands.DROP_ALL;
import static com.dbn.nls.NlsResources.txt;
import static liquibase.command.core.DropAllCommandStep.CATALOG_AND_SCHEMAS_ARG;
import static liquibase.command.core.DropAllCommandStep.FORCE_ARG;

/** Drops all Liquibase-visible database objects owned by the selected target schema. */
public class LiquibaseDropAllProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.DROP_ALL;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        DBSchema targetSchema = context.getTargetSchema();

        withLiquibaseDatabase(context, false, targetSchema, database ->
                withLiquibaseScope(context, classLoaderAccessor(), null,
                        output -> executeDropAll(database, targetSchema, context, output)));

        notifySchemaObjectChanges(targetSchema);
        context.getResult().appendConsoleOutput(txt("log.liquibase.info.DropAllCompleted", targetSchema.getName()));
    }

    private static void executeDropAll(
            @NotNull Database database,
            @NotNull DBSchema schema,
            @NotNull LiquibaseOperationContext context,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        CatalogAndSchema catalogAndSchema = new CatalogAndSchema(
                database.getDefaultCatalogName(),
                schema.getName());

        var arguments = arguments(
                "database", database,
                CATALOG_AND_SCHEMAS_ARG.getName(), new CatalogAndSchema[]{catalogAndSchema},
                FORCE_ARG.getName(), true);
        executeCommand(DROP_ALL, context, output, arguments);
    }
}
