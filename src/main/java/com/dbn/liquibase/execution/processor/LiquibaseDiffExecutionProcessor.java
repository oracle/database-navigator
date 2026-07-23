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

import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import com.dbn.liquibase.operation.LiquibaseOperationResult;
import com.dbn.object.DBSchema;
import liquibase.CatalogAndSchema;
import liquibase.database.Database;
import liquibase.diff.DiffGeneratorFactory;
import liquibase.diff.DiffResult;
import liquibase.diff.compare.CompareControl;
import org.jetbrains.annotations.NotNull;

import static com.dbn.liquibase.execution.LiquibaseComparisonItemStatus.CHANGED;
import static com.dbn.liquibase.execution.LiquibaseComparisonItemStatus.MISSING;
import static com.dbn.liquibase.execution.LiquibaseComparisonItemStatus.UNEXPECTED;

/**
 * Base processor for Liquibase operations that compare two database schemas.
 *
 * <p>Besides running the Liquibase comparison, this processor provides the shared mapping from
 * Liquibase's {@link DiffResult} to DBN comparison execution items. Concrete processors can use
 * the comparison either as their primary result or as input for another operation, such as
 * generating a diff changelog.</p>
 */
public abstract class LiquibaseDiffExecutionProcessor extends LiquibaseExecutionProcessor {
    protected static final String DIFF_TYPES =
            "catalogs,columns,foreignkeys,indexes,primarykeys,sequences,tables,uniqueconstraints,views";

    protected final DiffResult compareSchemas(
            @NotNull DBSchema sourceSchema,
            @NotNull Database sourceDatabase,
            @NotNull DBSchema targetSchema,
            @NotNull Database targetDatabase) throws Exception {
        CompareControl compareControl = new CompareControl(
                new CompareControl.SchemaComparison[]{new CompareControl.SchemaComparison(
                        new CatalogAndSchema(sourceDatabase.getDefaultCatalogName(), sourceSchema.getName()),
                        new CatalogAndSchema(targetDatabase.getDefaultCatalogName(), targetSchema.getName()))},
                DIFF_TYPES);
        DiffGeneratorFactory diffGeneratorFactory = DiffGeneratorFactory.getInstance();
        return diffGeneratorFactory.compare(sourceDatabase, targetDatabase, compareControl);
    }

    protected final void populateComparisonItems(
            @NotNull LiquibaseOperationResult result,
            @NotNull DiffResult diffResult) {
        diffResult.getMissingObjects().forEach(o -> result.ensureComparisonItem(o, null, MISSING, null));
        diffResult.getUnexpectedObjects().forEach(o -> result.ensureComparisonItem(null, o, UNEXPECTED, null));
        diffResult.getChangedObjects().forEach((o, d) -> result.ensureComparisonItem(o, null, CHANGED, d));
    }
}
