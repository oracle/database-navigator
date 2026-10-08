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
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import static com.dbn.migration.liquibase.execution.LiquibaseCommands.UPDATE_SQL;

/** Generates the SQL for pending Liquibase changesets without modifying the target schema. */
public class LiquibaseUpdateSqlProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.UPDATE_SQL;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        prepareChangelogContext(context, true);

        withLiquibaseDatabase(context, true, context.getTargetSchema(), database ->
                withLiquibaseScope(context, contentRootAccessor(context), sqlOutputBuilder(context),
                        output -> executeUpdateSql(
                                context,
                                database,
                                output)));
    }

    private void executeUpdateSql(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        var result = context.getResult();
        var paths = context.getInput().getWorkspacePaths();

        var arguments = arguments(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath(),
                "changeExecListener", new LiquibaseChangeSetRunListener(result));
        executeCommand(UPDATE_SQL, context, output, arguments);
    }

}
