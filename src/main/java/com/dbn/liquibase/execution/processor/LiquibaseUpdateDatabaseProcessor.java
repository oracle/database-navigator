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
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.LiquibaseUpdateInstruction;
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.object.DBSchema;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;

import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.liquibase.execution.LiquibaseCommands.TAG;
import static com.dbn.nls.NlsResources.txt;

/**
 * Applies pending changesets from the workspace changelog to the selected target schema.
 *
 * <p>The processor executes Liquibase's {@code update} command against a writable database
 * connection. A change execution listener creates and updates changeset execution items so the
 * result form can show each processed changeset, its status, details, and duration while the
 * operation is running.</p>
 *
 * <p>After a successful update, schema-level object change events are emitted for the affected
 * browsable object types so the DBN browser can refresh its database model. The processor also
 * observes cancellation requests between database operations and delegates final result state
 * handling to the common execution processor.</p>
 */
public class LiquibaseUpdateDatabaseProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.UPDATE_DATABASE;
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
                        output -> executeUpdate(
                                context,
                                database,
                                output)));

        notifySchemaObjectChanges(targetSchema);
        result.appendConsoleOutput(txt("log.liquibase.info.ChangelogUpdated", changelogFile));
    }

    private void executeUpdate(
            @NotNull LiquibaseExecutionContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        var input = context.getInput();
        var result = context.getResult();
        var paths = input.getWorkspacePaths();

        DBSchema targetSchema = context.getTargetSchema();
        String checkpointTag = input.getCheckpointTag();
        if (isNotEmpty(checkpointTag)) {
            executeCommand(TAG, output, Map.of(
                    "database", database,
                    "tag", checkpointTag));

            rememberTag(context, targetSchema, checkpointTag);
        }
        LiquibaseUpdateInstruction instruction = input.getUpdateInstruction();
        executeCommand(instruction.getCommand(), output, Map.of(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath(),
                instruction.getParameter(), instruction.getValue(),
                "changeExecListener", new LiquibaseChangeSetRunListener(result)));

    }
}
