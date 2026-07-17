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
import com.dbn.object.event.ObjectChangeEvent;
import liquibase.CatalogAndSchema;
import liquibase.command.core.DropAllCommandStep;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseCommands.DROP_ALL;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.UNSPECIFIED;
import static com.dbn.object.type.DBObjectType.BROWSABLE_TYPES;

/** Drops all Liquibase-visible database objects owned by the selected target schema. */
public class LiquibaseDropAllProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.DROP_ALL;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        DBSchema targetSchema = context.getTargetSchema();

        withLiquibaseDatabase(context, false, targetSchema, database ->
                withLiquibaseScope(context, classLoaderAccessor(), null,
                        output -> executeDropAll(database, targetSchema, output)));

        notifySchemaObjectChanges(targetSchema);
        context.getResult().appendConsoleOutput(txt("log.liquibase.info.DropAllCompleted", targetSchema.getName()));
    }

    private static void executeDropAll(
            @NotNull Database database,
            @NotNull DBSchema schema,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        CatalogAndSchema catalogAndSchema = new CatalogAndSchema(
                database.getDefaultCatalogName(),
                schema.getName());
        executeCommand(DROP_ALL, output, Map.of(
                "database", database,
                DropAllCommandStep.CATALOG_AND_SCHEMAS_ARG.getName(), new CatalogAndSchema[]{catalogAndSchema},
                DropAllCommandStep.FORCE_ARG.getName(), true));
    }

    private static void notifySchemaObjectChanges(@NotNull DBSchema schema) {
        BROWSABLE_TYPES.stream()
                .filter(type -> type.isSchemaObject())
                .forEach(type -> ObjectChangeEvent.notify(
                        UNSPECIFIED,
                        type,
                        schema.getConnectionId(),
                        schema.getSchemaId()));
    }
}
