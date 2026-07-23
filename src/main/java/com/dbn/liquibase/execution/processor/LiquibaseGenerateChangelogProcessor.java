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

package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.liquibase.operation.LiquibaseOperationContext;
import com.dbn.liquibase.operation.LiquibaseOperationResult;
import com.dbn.object.DBSchema;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.liquibase.execution.LiquibaseCommands.GENERATE_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseDatabaseObjects.buildTrackingTableFilter;
import static com.dbn.nls.NlsResources.txt;

/**
 * Generates the baseline Liquibase changelog for the source schema selected in the execution input.
 *
 * <p>The processor opens the source database in read-only mode and asks Liquibase to snapshot the
 * supported schema objects, including tables, columns, constraints, indexes, sequences, and views.
 * Liquibase's own tracking tables and related objects are excluded from the generated model because
 * they describe Liquibase bookkeeping rather than the application schema.</p>
 *
 * <p>The resulting changelog is written to the workspace master changelog path. If that file already
 * exists, the user must explicitly approve overwriting it. Snapshot items are added to the execution
 * result while the snapshot is being collected so the result form can display the operation's progress.</p>
 */
public class LiquibaseGenerateChangelogProcessor extends LiquibaseExecutionProcessor {
    private static final String GENERATE_CHANGELOG_DIFF_TYPES =
            "catalogs,columns,foreignkeys,indexes,primarykeys,sequences,tables,uniqueconstraints,views";

    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.GENERATE_CHANGELOG;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        prepareChangelogContext(context, false);
        prepareChangelogOutput(context);

        LiquibaseOperationResult result = context.getResult();
        Path changelogFile = context.getInput().getWorkspacePaths().getMasterChangelogPath();
        generateChangelog(context);
        result.appendConsoleOutput(txt("log.liquibase.info.InitialChangelogGenerated", changelogFile));
    }

    private void generateChangelog(@NotNull LiquibaseOperationContext context) throws Exception {
        Path changelogFile = context.getInput().getWorkspacePaths().getMasterChangelogPath();

        DBSchema sourceSchema = context.getSourceSchema();
        withLiquibaseDatabase(context, true, sourceSchema, database ->
                withLiquibaseScope(context, contentRootAccessor(context), null, output ->
                        executeGeneration(
                                context,
                                database,
                                output)));

        if (!Files.isRegularFile(changelogFile)) {
            throw new IllegalStateException("Liquibase did not create the changelog file: " + changelogFile);
        }

    }

    private void executeGeneration(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {


        var input = context.getInput();
        var result = context.getResult();
        var paths = input.getWorkspacePaths();

        Path changelogFile = paths.getMasterChangelogPath();
        String schemaName = context.getSourceSchema().getName();
        database.setDefaultSchemaName(schemaName);


        collectDatabaseObjects(context, database, schemaName);
        checkCanceled(context);
        executeCommand(GENERATE_CHANGELOG, output, Map.of(
                "database", database,
                "schemas", schemaName,
                "diffTypes", GENERATE_CHANGELOG_DIFF_TYPES,
                "changelogFile", changelogFile.toString(),
                "excludeObjects", buildTrackingTableFilter(database),
                "author", input.getChangelogAuthor()));

        String databaseTag = input.getDatabaseTag();
        if (isNotEmpty(databaseTag)) {
            Path contentRootPath = paths.getContentRootPath();
            appendDatabaseTag(
                    contentRootPath,
                    changelogFile,
                    input.getChangelogAuthor(),
                    databaseTag);
        }
    }

}
