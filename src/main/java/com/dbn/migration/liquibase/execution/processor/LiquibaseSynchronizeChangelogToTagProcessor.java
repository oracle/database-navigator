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
import com.dbn.migration.liquibase.operation.LiquibaseOperationResult;
import com.dbn.migration.liquibase.workspace.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import static com.dbn.migration.liquibase.execution.LiquibaseCommands.SYNCHRONIZE_CHANGELOG_TO_TAG;
import static com.dbn.nls.NlsResources.txt;

/** Marks workspace changesets up to a changelog tag as executed without applying their changes. */
public class LiquibaseSynchronizeChangelogToTagProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.SYNCHRONIZE_CHANGELOG_TO_TAG;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        prepareChangelogContext(context, true);

        LiquibaseOperationResult result = context.getResult();
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        Path changelogFile = paths.getMasterChangelogPath();
        DBSchema targetSchema = context.getTargetSchema();
        String tag = context.getInput().getChangelogTag();

        withLiquibaseDatabase(context, false, targetSchema, database ->
                withLiquibaseScope(context, contentRootAccessor(context), null,
                        output -> executeSynchronize(context, database, output, tag)));

        notifySchemaObjectChanges(targetSchema);
        rememberTag(context, targetSchema, tag);
        result.appendConsoleOutput(txt("log.liquibase.info.ChangelogSynchronized", changelogFile));
    }

    private void executeSynchronize(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output,
            @NotNull String tag) throws Exception {
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();

        var arguments = arguments(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath(),
                "tag", tag,
                "changeExecListener", new LiquibaseChangeSetSynchronizeListener(context.getResult()));
        executeCommand(SYNCHRONIZE_CHANGELOG_TO_TAG, context, output, arguments);
    }
}
