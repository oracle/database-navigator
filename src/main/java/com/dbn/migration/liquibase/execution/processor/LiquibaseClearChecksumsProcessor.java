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
import com.dbn.object.DBSchema;
import liquibase.database.Database;
import org.jetbrains.annotations.NotNull;

import static com.dbn.migration.liquibase.execution.LiquibaseCommands.CLEAR_CHECKSUMS;

/** Clears stored Liquibase checksums so they can be recalculated on the next changelog run. */
public class LiquibaseClearChecksumsProcessor extends LiquibaseExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.CLEAR_CHECKSUMS;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        DBSchema targetSchema = context.getTargetSchema();
        withLiquibaseDatabase(context, false, targetSchema, database ->
                withLiquibaseScope(context, classLoaderAccessor(), null,
                        output -> executeClearChecksums(database, context, output)));
    }

    private void executeClearChecksums(
            @NotNull Database database,
            @NotNull LiquibaseOperationContext context,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {

        var arguments = arguments("database", database);
        executeCommand(CLEAR_CHECKSUMS, context, output, arguments);
    }
}
