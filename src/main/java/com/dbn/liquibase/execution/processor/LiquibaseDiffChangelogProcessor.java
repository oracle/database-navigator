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

import com.dbn.liquibase.execution.LiquibaseCommands;
import com.dbn.liquibase.execution.LiquibaseExecutionContext;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;

import static com.dbn.liquibase.execution.LiquibaseDatabaseObjects.buildTrackingTableFilter;
import static com.dbn.nls.NlsResources.txt;

/**
 * Generates a migration changelog from the differences between a source and target schema.
 *
 * <p>The source database is passed to Liquibase as the reference database. The generated
 * changelog contains the changes required to align the target database with that reference and
 * is written to the selected workspace master changelog path.</p>
 */
public class LiquibaseDiffChangelogProcessor extends LiquibaseExecutionProcessor {
    private static final String DIFF_TYPES =
            "catalogs,columns,foreignkeys,indexes,primarykeys,sequences,tables,uniqueconstraints,views";

    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.GENERATE_DIFF_CHANGELOG;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        LiquibaseExecutionInput input = context.getInput();
        LiquibaseExecutionResult result = context.getResult();
        LiquibaseWorkspacePaths paths = input.getWorkspacePaths();
        Path changelogFile = paths.getMasterChangelogPath();
        result.setChangelogPath(changelogFile);

        if (Files.exists(changelogFile) && !input.isOverwriteConfirmed()) {
            if (!LiquibaseInitializationProcessor.confirmOverwrite(input)) {
                throw new CancellationException("Changelog overwrite canceled");
            }
        }

        Files.createDirectories(changelogFile.getParent());
        DBSchema sourceSchema = required("Source schema", input.getSourceSchema());
        DBSchema targetSchema = required("Target schema", input.getTargetSchema());

        withLiquibaseDatabase(context, true, sourceSchema, sourceDatabase ->
                withLiquibaseDatabase(context, true, targetSchema, targetDatabase ->
                        withLiquibaseScope(context, paths.getContentRootPath(), output -> {
                            checkCanceled(context);
                            Map<String, Object> arguments = new HashMap<>();
                            arguments.put("referenceDatabase", sourceDatabase);
                            arguments.put("database", targetDatabase);
                            arguments.put("diffTypes", DIFF_TYPES);
                            arguments.put("excludeObjects", buildTrackingTableFilter(targetDatabase));
                            arguments.put("changelogFile", changelogFile.toString());
                            arguments.put("author", input.getChangelogAuthor());
                            arguments.put("overwriteOutputFile", input.isOverwriteConfirmed());
                            executeCommand(LiquibaseCommands.GENERATE_DIFF_CHANGELOG, output, arguments);
                            return null;
                        })));

        checkCanceled(context);
        if (!Files.isRegularFile(changelogFile)) {
            throw new IllegalStateException("Liquibase did not create the diff changelog file: " + changelogFile);
        }
        result.appendConsoleOutput(txt("log.liquibase.info.DiffChangelogGenerated", changelogFile));
    }
}
