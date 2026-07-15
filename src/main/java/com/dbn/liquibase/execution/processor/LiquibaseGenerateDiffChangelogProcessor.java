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

import com.dbn.liquibase.execution.LiquibaseExecutionContext;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import liquibase.diff.DiffResult;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseCommands.GENERATE_DIFF_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseDatabaseObjects.buildTrackingTableFilter;
import static com.dbn.nls.NlsResources.txt;

/**
 * Generates a migration changelog from the differences between a source and target schema.
 *
 * <p>The source database is passed to Liquibase as the reference database. The generated
 * changelog contains the changes required to align the target database with that reference and
 * is written to the selected workspace master changelog path.</p>
 */
public class LiquibaseGenerateDiffChangelogProcessor extends LiquibaseDiffExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.GENERATE_DIFF_CHANGELOG;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        prepareChangelogContext(context, false);
        prepareChangelogOutput(context);

        LiquibaseExecutionInput input = context.getInput();
        LiquibaseExecutionResult result = context.getResult();
        LiquibaseWorkspacePaths paths = input.getWorkspacePaths();

        Path changelogFile = paths.getMasterChangelogPath();
        DBSchema sourceSchema = context.getSourceSchema();
        DBSchema targetSchema = context.getTargetSchema();

        withLiquibaseDatabase(context, true, sourceSchema, sourceDatabase ->
                withLiquibaseDatabase(context, true, targetSchema, targetDatabase ->
                        withLiquibaseScope(context, paths.getContentRootPath(), output -> {
                            checkCanceled(context);
                            DiffResult diffResult = compareSchemas(sourceSchema, sourceDatabase, targetSchema, targetDatabase);
                            populateComparisonItems(result, diffResult);

                            checkCanceled(context);
                            executeCommand(GENERATE_DIFF_CHANGELOG, output, Map.of(
                                    "referenceDatabase", sourceDatabase,
                                    "database", targetDatabase,
                                    "diffTypes", DIFF_TYPES,
                                    "excludeObjects", buildTrackingTableFilter(targetDatabase),
                                    "changelogFile", changelogFile.toString(),
                                    "author", input.getChangelogAuthor()));
                            return null;
                        })));

        checkCanceled(context);
        if (!Files.isRegularFile(changelogFile)) {
            throw new IllegalStateException("Liquibase did not create the diff changelog file: " + changelogFile);
        }
        result.appendConsoleOutput(txt("log.liquibase.info.DiffChangelogGenerated", changelogFile));
    }
}
