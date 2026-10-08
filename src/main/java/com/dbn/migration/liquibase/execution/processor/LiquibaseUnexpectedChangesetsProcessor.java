/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.migration.liquibase.execution.processor;

import com.dbn.migration.liquibase.execution.LiquibaseExecutionItemStatus;
import com.dbn.migration.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.migration.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.migration.liquibase.operation.LiquibaseOperation;
import com.dbn.migration.liquibase.operation.LiquibaseOperationContext;
import com.dbn.migration.liquibase.operation.LiquibaseOperationResult;
import com.dbn.migration.liquibase.workspace.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.RanChangeSet;
import liquibase.database.Database;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ResourceAccessor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.dbn.nls.NlsResources.txt;
import static liquibase.command.core.UnexpectedChangesetsCommandStep.listUnexpectedChangeSets;

/** Finds changesets recorded in the database but missing from the workspace changelog. */
public class LiquibaseUnexpectedChangesetsProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.UNEXPECTED_CHANGESETS;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        prepareChangelogContext(context, true);

        DBSchema targetSchema = context.getTargetSchema();
        withLiquibaseDatabase(context, true, targetSchema, database ->
                withLiquibaseScope(context, contentRootAccessor(context), null,
                        output -> executeUnexpectedChangesets(context, database, output)));
    }

    private void executeUnexpectedChangesets(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        ResourceAccessor resourceAccessor = contentRootAccessor(context);
        ChangeLogParameters parameters = new ChangeLogParameters(database);
        DatabaseChangeLog changeLog = ChangeLogParserFactory.getInstance()
                .getParser(paths.getMasterChangelogRelativePath(), resourceAccessor)
                .parse(paths.getMasterChangelogRelativePath(), parameters, resourceAccessor);

        Collection<RanChangeSet> unexpected = listUnexpectedChangeSets(
                database,
                changeLog,
                parameters.getContexts(),
                parameters.getLabels());
        LiquibaseOperationResult result = context.getResult();
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

/*        executeCommand(
                UNEXPECTED_CHANGESETS,
                output,
                Map.of("verbose", true),
                Map.of(
                        Database.class, database,
                        DatabaseChangeLog.class, changeLog,
                        ChangeLogParameters.class, parameters));*/
        result.notifyItemsChanged();
    }
}
