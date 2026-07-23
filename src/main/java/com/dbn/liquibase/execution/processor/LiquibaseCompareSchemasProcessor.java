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

import com.dbn.liquibase.execution.logging.LiquibaseExecutionOutputStream;
import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.liquibase.operation.LiquibaseOperationContext;
import com.dbn.liquibase.operation.LiquibaseOperationResult;
import com.dbn.object.DBSchema;
import liquibase.database.Database;
import liquibase.diff.DiffResult;
import org.jetbrains.annotations.NotNull;

/**
 * Compares the source and target schemas supplied by the execution input through Liquibase's
 * {@code diff} command.
 *
 * <p>The source database is passed to Liquibase as the reference database and the target database
 * as the database under comparison. Both connections are opened read-only, and the comparison is
 * limited to the object categories currently supported by the DBN Liquibase integration, including
 * tables, columns, constraints, indexes, sequences, and views.</p>
 *
 * <p>The operation produces Liquibase's comparison output in the execution result console and does
 * not modify either database or the workspace changelog.</p>
 */
public class LiquibaseCompareSchemasProcessor extends LiquibaseDiffExecutionProcessor {
    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.COMPARE_SCHEMAS;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseOperationContext context) throws Exception {
        DBSchema sourceSchema = context.getSourceSchema();
        DBSchema targetSchema = context.getTargetSchema();

        withLiquibaseDatabase(context, true, sourceSchema, sourceDatabase ->
                withLiquibaseDatabase(context, true, targetSchema, targetDatabase ->
                        withLiquibaseScope(context, classLoaderAccessor(), null,
                                output -> executeComparison(
                                        context,
                                        sourceDatabase,
                                        targetDatabase,
                                        output))));
    }

    private void executeComparison(
            @NotNull LiquibaseOperationContext context,
            @NotNull Database sourceDatabase,
            @NotNull Database targetDatabase,
            @NotNull LiquibaseExecutionOutputStream output) throws Exception {
        LiquibaseOperationResult result = context.getResult();
        DBSchema sourceSchema = context.getSourceSchema();
        DBSchema targetSchema = context.getTargetSchema();

        DiffResult diffResult = compareSchemas(
                sourceSchema,
                sourceDatabase,
                targetSchema,
                targetDatabase);

        populateComparisonItems(result, diffResult);
    }
}
