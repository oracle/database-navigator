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

import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.execution.LiquibaseExecutionContext;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.LiquibaseRollbackInstruction;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.object.DBSchema;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseRollbackType.TAG;
import static com.dbn.nls.NlsResources.txt;

/** Rolls back a selected number of previously applied Liquibase changesets. */
public class LiquibaseRollbackChangesetsProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.ROLLBACK_CHANGESETS;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        prepareChangelogContext(context, true);

        var input = context.getInput();
        var result = context.getResult();
        var paths = input.getWorkspacePaths();

        Path changelogFile = paths.getMasterChangelogPath();
        DBSchema targetSchema = context.getTargetSchema();

        withLiquibaseDatabase(context, false, targetSchema, database ->
                withLiquibaseScope(context, contentRootAccessor(context), null,
                        output -> executeRollback(
                                context, 
                                database, 
                                output)));

        notifySchemaObjectChanges(targetSchema);
        removeTagHistory(context);
        result.appendConsoleOutput(txt("log.liquibase.info.ChangelogRolledBack", changelogFile,
                input.getRollbackInstruction().getCount()));
    }

    private static void removeTagHistory(@NotNull LiquibaseExecutionContext context) {
        LiquibaseExecutionInput input = context.getInput();
        LiquibaseRollbackInstruction rollbackInstruction = input.getRollbackInstruction();
        if (rollbackInstruction.getType() == TAG) {
            DBSchema targetSchema = context.getTargetSchema();
            DatabaseLiquibaseManager liquibaseManager = context.getLiquibaseManager();
            liquibaseManager.removeTag(
                    targetSchema.getConnectionId(),
                    targetSchema.getSchemaId(),
                    rollbackInstruction.getTag());
        }
    }

    private void executeRollback(
            @NotNull LiquibaseExecutionContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        var input = context.getInput();
        var result = context.getResult();
        var paths = input.getWorkspacePaths();
        var instruction = input.getRollbackInstruction();

        executeCommand(instruction.getCommand(), output, Map.of(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath(),
                instruction.getParameter(), instruction.getValue(),
                "changeExecListener", new LiquibaseChangeSetRollbackListener(result, "Rolled back")));

    }
}
