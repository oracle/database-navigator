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
import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.object.DBSchema;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.liquibase.execution.LiquibaseCommands.TAG;
import static com.dbn.nls.NlsResources.txt;

/** Applies a named Liquibase tag to the selected target database. */
public class LiquibaseTagDatabaseProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.TAG_DATABASE;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        var input = context.getInput();
        var result = context.getResult();

        DBSchema targetSchema = context.getTargetSchema();
        String tag = input.getDatabaseTag();
        if (!isNotEmpty(tag)) throw new IllegalStateException("Database tag not specified");

        withLiquibaseDatabase(context, false, targetSchema, database ->
                withLiquibaseScope(context, classLoaderAccessor(), null,
                        output -> executeTag(
                                context,
                                database,
                                output)));

        rememberTag(context, targetSchema, tag);
        result.appendConsoleOutput(txt("log.liquibase.info.DatabaseTagged", tag));
    }

    private void executeTag(
            @NotNull LiquibaseExecutionContext context,
            @NotNull Database database,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        String tag = context.getInput().getDatabaseTag();
        executeCommand(TAG, output, Map.of(
                "database", database,
                "tag", tag));
    }

}
