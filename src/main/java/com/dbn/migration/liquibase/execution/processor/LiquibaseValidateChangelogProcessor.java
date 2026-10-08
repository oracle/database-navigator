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

import com.dbn.migration.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.migration.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.migration.liquibase.operation.LiquibaseOperation;
import com.dbn.migration.liquibase.operation.LiquibaseOperationContext;
import com.dbn.migration.liquibase.workspace.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import static com.dbn.migration.liquibase.execution.LiquibaseCommands.VALIDATE_CHANGELOG;
import static com.dbn.nls.NlsResources.txt;

/**
 * Validates the structure and references of the workspace's existing Liquibase changelog.
 *
 * <p>The processor requires a target schema and an existing master changelog, then executes
 * Liquibase's {@code validate} command in the workspace content root. Validation checks that the
 * changelog can be parsed and that its changesets and referenced resources are internally valid;
 * it does not apply changes to the database or compare the database schema with the changelog.</p>
 *
 * <p>Liquibase output is forwarded to the execution result console, while failures are reported by
 * the common execution processor as a failed operation.</p>
 */
public class LiquibaseValidateChangelogProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.VALIDATE_CHANGELOG;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        prepareChangelogContext(context, true);

        var result = context.getResult();
        var paths = context.getInput().getWorkspacePaths();

        Path changelogFile = paths.getMasterChangelogPath();
        validateChangelog(context);
        result.appendConsoleOutput(txt("log.liquibase.info.ChangelogValidated", changelogFile));
    }

    private void validateChangelog(@NotNull LiquibaseOperationContext context) throws Exception {
        DBSchema targetSchema = context.getTargetSchema();
        withLiquibaseDatabase(context, true, targetSchema, database ->
                withLiquibaseScope(context, contentRootAccessor(context), null,
                        output -> executeValidation(
                                context,
                                database,
                                output)));
    }

    private void executeValidation(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();

        var arguments = arguments(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath());
        executeCommand(VALIDATE_CHANGELOG, context, output, arguments);
    }

}
