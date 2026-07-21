/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseExecutionContext;
import com.dbn.liquibase.execution.LiquibaseExecutionItemStatus;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.RanChangeSet;
import liquibase.command.core.UnexpectedChangesetsCommandStep;
import liquibase.database.Database;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ResourceAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseCommands.UNEXPECTED_CHANGESETS;
import static com.dbn.nls.NlsResources.txt;

/** Finds changesets recorded in the database but missing from the workspace changelog. */
public class LiquibaseUnexpectedChangesetsProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.UNEXPECTED_CHANGESETS;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        prepareChangelogContext(context, true);

        DBSchema targetSchema = context.getTargetSchema();
        withLiquibaseDatabase(context, true, targetSchema, database ->
                withLiquibaseScope(context, contentRootAccessor(context), null,
                        output -> executeUnexpectedChangesets(context, database, output)));
    }

    private void executeUnexpectedChangesets(
            @NotNull LiquibaseExecutionContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        ResourceAccessor resourceAccessor = contentRootAccessor(context);
        ChangeLogParameters parameters = new ChangeLogParameters(database);
        DatabaseChangeLog changeLog = ChangeLogParserFactory.getInstance()
                .getParser(paths.getMasterChangelogRelativePath(), resourceAccessor)
                .parse(paths.getMasterChangelogRelativePath(), parameters, resourceAccessor);

        Collection<RanChangeSet> unexpected = UnexpectedChangesetsCommandStep.listUnexpectedChangeSets(
                database,
                changeLog,
                parameters.getContexts(),
                parameters.getLabels());
        LiquibaseExecutionResult result = context.getResult();
        for (RanChangeSet ranChangeSet : unexpected) {
            checkCanceled(context);
            ChangeSet changeSet = new ChangeSet(
                    ranChangeSet.getId(),
                    ranChangeSet.getAuthor(),
                    false,
                    false,
                    ranChangeSet.getChangeLog(),
                    null,
                    null,
                    null);
            result.ensureChangeSetItem(
                    changeSet,
                    LiquibaseExecutionItemStatus.PROCESSED,
                    txt("msg.liquibase.text.UnexpectedChangeSet"));
        }

        executeCommand(
                UNEXPECTED_CHANGESETS,
                output,
                Map.of("verbose", true),
                Map.of(
                        Database.class, database,
                        DatabaseChangeLog.class, changeLog,
                        ChangeLogParameters.class, parameters));
        result.notifyItemsChanged();
    }
}
