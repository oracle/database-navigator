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

import com.dbn.migration.liquibase.execution.LiquibaseChangeSetItem;
import com.dbn.migration.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.migration.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.migration.liquibase.operation.LiquibaseOperation;
import com.dbn.migration.liquibase.operation.LiquibaseOperationContext;
import com.dbn.migration.liquibase.operation.LiquibaseOperationResult;
import com.dbn.migration.liquibase.workspace.LiquibaseWorkspacePaths;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.dbn.migration.liquibase.execution.LiquibaseCommands.SYNCHRONIZE_CHANGELOG_SQL;
import static com.dbn.nls.NlsResources.txt;

/** Generates the SQL for marking workspace changesets as executed without changing the target schema. */
public class LiquibaseSynchronizeChangelogSqlProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.SYNCHRONIZE_CHANGELOG_SQL;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        prepareChangelogContext(context, true);

        withLiquibaseDatabase(context, true, context.getTargetSchema(), database ->
                withLiquibaseScope(context, contentRootAccessor(context), sqlOutputBuilder(context),
                        output -> executeSynchronizeSql(context, database, output)));
    }

    private void executeSynchronizeSql(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        LiquibaseOperationResult result = context.getResult();
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        List<LiquibaseChangeSetItem> items = discoverChangeSetItems(
                context,
                database,
                changeSet -> txt("msg.liquibase.text.ChangeSetSyncSqlPending"));

        var listener = new LiquibaseChangeSetSynchronizeListener(result);
        var arguments = arguments(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath(),
                "changeExecListener", listener);
        executeCommand(SYNCHRONIZE_CHANGELOG_SQL, context, output, arguments);

        completeChangeSetItems(
                context,
                items,
                item -> txt("msg.liquibase.text.ChangeSetSyncSqlGenerated"));
    }
}
