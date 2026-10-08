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
import com.dbn.migration.liquibase.execution.LiquibaseExecutionItemStatus;
import com.dbn.migration.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.migration.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.migration.liquibase.operation.LiquibaseOperation;
import com.dbn.migration.liquibase.operation.LiquibaseOperationContext;
import com.dbn.migration.liquibase.operation.LiquibaseOperationResult;
import com.dbn.migration.liquibase.workspace.LiquibaseWorkspacePaths;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.dbn.migration.liquibase.execution.LiquibaseCommands.MARK_NEXT_CHANGESET_RAN;
import static com.dbn.nls.NlsResources.txt;

/** Marks the next pending Liquibase changeset as executed without applying its changes. */
public class LiquibaseMarkNextChangesetRanProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.MARK_NEXT_CHANGESET_RAN;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        prepareChangelogContext(context, true);

        withLiquibaseDatabase(context, false, context.getTargetSchema(), database ->
                withLiquibaseScope(context, contentRootAccessor(context), null,
                        output -> executeMarkNext(context, database, output)));
    }

    private void executeMarkNext(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        LiquibaseOperationResult result = context.getResult();
        LiquibaseWorkspacePaths paths = context.getInput().getWorkspacePaths();
        List<ChangeSet> pending = discoverPendingChangeSets(context, database);
        LiquibaseChangeSetItem item = pending.isEmpty() ? null : result.ensureChangeSetItem(
                pending.get(0),
                LiquibaseExecutionItemStatus.DISCOVERED,
                txt("msg.liquibase.text.ChangeSetMarkNextPending"));

        var arguments = arguments(
                "database", database,
                "changelogFile", paths.getMasterChangelogRelativePath());
        executeCommand(MARK_NEXT_CHANGESET_RAN, context, output, arguments);

        if (item != null) {
            completeChangeSetItems(
                    context,
                    List.of(item),
                    changeSetItem -> txt("msg.liquibase.text.ChangeSetMarkedRan", "MARK_RAN"));
            result.appendConsoleOutput(txt("log.liquibase.info.ChangeSetMarkedRan", item.getId()));
        }
    }
}
