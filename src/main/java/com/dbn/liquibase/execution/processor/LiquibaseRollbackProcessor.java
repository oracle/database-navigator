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

import com.dbn.liquibase.execution.LiquibaseChangeSetItem;
import com.dbn.liquibase.execution.LiquibaseExecutionContext;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionItemStatus;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.visitor.AbstractChangeExecListener;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static com.dbn.liquibase.execution.LiquibaseCommands.ROLLBACK_COUNT;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.UNSPECIFIED;
import static com.dbn.object.type.DBObjectType.BROWSABLE_TYPES;

/** Rolls back a selected number of previously applied Liquibase changesets. */
public class LiquibaseRollbackProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.ROLLBACK;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        LiquibaseExecutionInput input = context.getInput();
        LiquibaseExecutionResult result = context.getResult();
        LiquibaseWorkspacePaths paths = input.getWorkspacePaths();
        Path changelogFile = paths.getMasterChangelogPath();
        result.setChangelogPath(changelogFile);
        if (!Files.isRegularFile(changelogFile)) {
            throw new IllegalStateException("Changelog file does not exist: " + changelogFile);
        }

        DBSchema targetSchema = required("Target schema", input.getTargetSchema());
        String relativeChangelog = paths.getRelativePath(changelogFile);
        withLiquibaseDatabase(context, false, targetSchema, database -> {
            checkCanceled(context);
            withLiquibaseScope(context, paths.getContentRootPath(), output ->
                    executeCommand(ROLLBACK_COUNT, output, Map.of(
                            "database", database,
                            "changelogFile", relativeChangelog,
                            "count", input.getRollbackCount(),
                            "changeExecListener", new ChangeSetListener(result))));
            notifySchemaObjectChanges(targetSchema);
            checkCanceled(context);
            return null;
        });
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

    private static class ChangeSetListener extends AbstractChangeExecListener {
        private final LiquibaseExecutionResult result;

        private ChangeSetListener(@NotNull LiquibaseExecutionResult result) {
            this.result = result;
        }

        @Override
        public void willRollback(
                ChangeSet changeSet,
                DatabaseChangeLog changeLog,
                Database database) {
            LiquibaseChangeSetItem item = result.ensureChangeSetItem(changeSet);
            item.startProcessing();
            result.notifyItemsChanged();
        }

        @Override
        public void rolledBack(
                ChangeSet changeSet,
                DatabaseChangeLog changeLog,
                Database database) {
            LiquibaseChangeSetItem item = result.ensureChangeSetItem(changeSet);
            item.finishProcessing();
            item.updateStatus(LiquibaseExecutionItemStatus.EXECUTED, "Rolled back");
            result.notifyItemsChanged();
        }

        @Override
        public void rollbackFailed(
                ChangeSet changeSet,
                DatabaseChangeLog changeLog,
                Database database,
                Exception exception) {
            LiquibaseChangeSetItem item = result.ensureChangeSetItem(changeSet);
            item.finishProcessing();
            item.updateStatus(LiquibaseExecutionItemStatus.FAILED, exception.getMessage());
            result.notifyItemsChanged();
        }
    }
}
