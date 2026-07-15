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
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseCommands.SYNCHRONIZE_CHANGELOG;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.UNSPECIFIED;
import static com.dbn.object.type.DBObjectType.BROWSABLE_TYPES;

/** Marks workspace changesets as executed without applying their database changes. */
public class LiquibaseSynchronizeChangelogProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.SYNCHRONIZE_CHANGELOG;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        prepareChangelogContext(context, true);

        LiquibaseExecutionInput input = context.getInput();
        LiquibaseExecutionResult result = context.getResult();
        LiquibaseWorkspacePaths paths = input.getWorkspacePaths();
        Path changelogFile = paths.getMasterChangelogPath();
        DBSchema targetSchema = context.getTargetSchema();

        withLiquibaseDatabase(context, false, targetSchema, database -> {
            checkCanceled(context);
            withLiquibaseScope(context, paths.getContentRootPath(), output -> {
                executeCommand(SYNCHRONIZE_CHANGELOG, output, Map.of(
                        "database", database,
                        "changelogFile", paths.getRelativePath(changelogFile),
                        "changeExecListener", new LiquibaseChangeSetRunListener(result)));
                return null;
            });
            notifySchemaObjectChanges(targetSchema);
            checkCanceled(context);
            return null;
        });
        result.appendConsoleOutput(txt("log.liquibase.info.ChangelogSynchronized", changelogFile));
    }

    private static void notifySchemaObjectChanges(@NotNull DBSchema schema) {
        BROWSABLE_TYPES.stream()
                .filter(t -> t.isSchemaObject())
                .forEach(t -> ObjectChangeEvent.notify(
                        UNSPECIFIED,
                        t,
                        schema.getConnectionId(),
                        schema.getSchemaId()));
    }

}
