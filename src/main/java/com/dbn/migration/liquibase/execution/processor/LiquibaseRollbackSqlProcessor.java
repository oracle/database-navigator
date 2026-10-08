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


/** Generates rollback SQL without modifying the target schema. */
public class LiquibaseRollbackSqlProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.ROLLBACK_SQL;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        prepareChangelogContext(context, true);

        withLiquibaseDatabase(context, true, context.getTargetSchema(), database ->
                withLiquibaseScope(context, contentRootAccessor(context), sqlOutputBuilder(context),
                        output -> executeRollbackSql(
                                context,
                                database,
                                output)));
    }

    private void executeRollbackSql(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        var input = context.getInput();
        var result = context.getResult();
        var paths = input.getWorkspacePaths();
        var instruction = input.getRollbackInstruction();

        LiquibaseChangeSetRollbackListener rollbackListener = new LiquibaseChangeSetRollbackListener(result, "SQL generated");
        var arguments = arguments(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath(),
                instruction.getParameter(), instruction.getValue(),
                "changeExecListener", rollbackListener);
        executeCommand(instruction.getSqlCommand(), context, output, arguments);
    }

}
