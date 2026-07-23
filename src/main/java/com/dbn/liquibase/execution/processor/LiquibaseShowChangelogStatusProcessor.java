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
import com.dbn.liquibase.operation.LiquibaseOperationInput;
import com.dbn.liquibase.operation.LiquibaseOperationResult;
import com.dbn.liquibase.workspace.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseCommands.SHOW_CHANGELOG_STATUS;
import static com.dbn.nls.NlsResources.txt;

/**
 * Reports the pending changesets in the workspace changelog for the selected target schema.
 *
 * <p>The processor executes Liquibase's {@code status} command in read-only mode. It resolves the
 * master changelog relative to the workspace content root and forwards Liquibase's detailed status
 * output to the execution result console.</p>
 *
 * <p>This operation is informational: it does not modify the database, changelog files, or
 * Liquibase tracking tables. The target schema is still required because status is evaluated against
 * the database's changelog history.</p>
 */
public class LiquibaseShowChangelogStatusProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.SHOW_CHANGELOG_STATUS;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        prepareChangelogContext(context, true);

        LiquibaseOperationInput input = context.getInput();
        LiquibaseOperationResult result = context.getResult();
        LiquibaseWorkspacePaths paths = input.getWorkspacePaths();

        Path changelogFile = paths.getMasterChangelogPath();
        DBSchema targetSchema = context.getTargetSchema();

        withLiquibaseDatabase(context, true, targetSchema, database ->
                withLiquibaseScope(context, contentRootAccessor(context), null,
                        output -> executeStatus(
                                context,
                                database,
                                output)));

        result.appendConsoleOutput(txt("log.liquibase.info.ChangelogStatusDisplayed", changelogFile));
    }

    private void executeStatus(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        executeCommand(SHOW_CHANGELOG_STATUS, output, Map.of(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath(),
                "verbose", true));
    }
}
