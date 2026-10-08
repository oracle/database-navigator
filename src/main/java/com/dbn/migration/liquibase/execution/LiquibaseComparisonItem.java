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

package com.dbn.migration.liquibase.execution;

import liquibase.diff.ObjectDifferences;
import liquibase.structure.DatabaseObject;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.migration.liquibase.execution.LiquibaseExecutionItemStatus.DISCOVERED;

/** Database object difference reported by a Liquibase schema comparison. */
@Getter
public class LiquibaseComparisonItem extends LiquibaseExecutionItem {
    private final DatabaseObject sourceObject;
    private final DatabaseObject targetObject;
    private final LiquibaseComparisonItemStatus comparisonStatus;

    public LiquibaseComparisonItem(
            @Nullable DatabaseObject sourceObject,
            @Nullable DatabaseObject targetObject,
            @NotNull LiquibaseComparisonItemStatus comparisonStatus,
            @Nullable ObjectDifferences differences) {
        super(DISCOVERED, formatDetails(differences));
        this.sourceObject = sourceObject;
        this.targetObject = targetObject;
        this.comparisonStatus = comparisonStatus;
    }

    @Nullable
    private static String formatDetails(@Nullable ObjectDifferences differences) {
        if (differences == null || !differences.hasDifferences()) return null;
        return differences.getDifferences().stream()
                .map(String::valueOf)
                .sorted()
                .reduce((left, right) -> left + "; " + right)
                .orElse(null);
    }

    @NotNull
    public String getKey() {
        return buildObjectKey(sourceObject) + ':' + buildObjectKey(targetObject);
    }

    @NotNull
    private static String buildObjectKey(@Nullable DatabaseObject object) {
        if (object == null) return "";
        String schema = object.getSchema() == null ? "" : object.getSchema().getName();
        return object.getObjectTypeName() + ':' + schema + ':' + object.getName();
    }
}
