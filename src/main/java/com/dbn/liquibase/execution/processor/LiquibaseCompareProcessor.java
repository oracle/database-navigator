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
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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
public class LiquibaseCompareProcessor extends LiquibaseExecutionProcessor {
    private static final String DIFF_TYPES =
            "catalogs,columns,foreignkeys,indexes,primarykeys,sequences,tables,uniqueconstraints,views";

    @Override
    public LiquibaseOperation getOperation() {
        return LiquibaseOperation.COMPARE;
    }

    @Override
    protected void executeOperation(@NotNull LiquibaseExecutionContext context) throws Exception {
        LiquibaseExecutionInput input = context.getInput();
        LiquibaseExecutionResult result = context.getResult();
        DBSchema sourceSchema = required("Source schema", input.getSourceSchema());
        DBSchema targetSchema = required("Target schema", input.getTargetSchema());
        LiquibaseWorkspacePaths paths = input.getWorkspacePaths();

        withLiquibaseDatabase(context, true, sourceSchema, sourceDatabase ->
                withLiquibaseDatabase(context, true, targetSchema, targetDatabase ->
                        withLiquibaseScope(context, paths.getContentRootPath(), output -> {
                            checkCanceled(context);
                            return executeCommand("diff", output, Map.of(
                                    "referenceDatabase", sourceDatabase,
                                    "database", targetDatabase,
                                    "referenceSchemas", sourceSchema.getName(),
                                    "schemas", targetSchema.getName(),
                                    "diffTypes", DIFF_TYPES));
                        })));
    }
}
