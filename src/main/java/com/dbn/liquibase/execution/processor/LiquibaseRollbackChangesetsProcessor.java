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
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.LiquibaseRollbackInstruction;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseRollbackType.TAG;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.UNSPECIFIED;
import static com.dbn.object.type.DBObjectType.BROWSABLE_TYPES;

/** Rolls back a selected number of previously applied Liquibase changesets. */
public class LiquibaseRollbackChangesetsProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.ROLLBACK_CHANGESETS;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        prepareChangelogContext(context, true);

        LiquibaseExecutionInput input = context.getInput();
        LiquibaseExecutionResult result = context.getResult();
        LiquibaseWorkspacePaths paths = input.getWorkspacePaths();

        Path changelogFile = paths.getMasterChangelogPath();
        DBSchema targetSchema = context.getTargetSchema();
        String relativeChangelog = paths.getRelativePath(changelogFile);

        withLiquibaseDatabase(context, false, targetSchema, database -> {
            checkCanceled(context);
            LiquibaseRollbackInstruction instruction = input.getRollbackInstruction();

            withLiquibaseScope(context, paths.getContentRootPath(), output ->
                    executeCommand(instruction.command(), output, Map.of(
                            "database", database,
                            "changelogFile", relativeChangelog,
                            instruction.parameter(), instruction.value(),
                            "changeExecListener", new LiquibaseChangeSetRollbackListener(result, "Rolled back"))));
            notifySchemaObjectChanges(targetSchema);
            checkCanceled(context);
            return null;
        });
        if (input.getRollbackType() == TAG) {
            DatabaseLiquibaseManager liquibaseManager = context.getLiquibaseManager();
            liquibaseManager.removeTag(
                    targetSchema.getConnectionId(),
                    targetSchema.getSchemaId(),
                    input.getRollbackTag());
        }
        result.appendConsoleOutput(txt("log.liquibase.info.ChangelogRolledBack", changelogFile, input.getRollbackCount()));
    }

    private static void notifySchemaObjectChanges(@NotNull DBSchema schema) {
        BROWSABLE_TYPES.stream()
                .filter(t -> t.isSchemaObject())
                .forEach(t -> ObjectChangeEvent.notify(
                        UNSPECIFIED,
                        t,
                        schema.getConnectionId(),
                        schema.getSchemaId()));
    }

}
