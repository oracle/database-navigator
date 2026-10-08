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

package com.dbn.migration.liquibase.execution.ui;

import com.dbn.common.ui.table.DBNDynamicTableModel;
import com.dbn.migration.liquibase.execution.LiquibaseComparisonItem;
import com.dbn.migration.liquibase.operation.LiquibaseOperationResult;
import liquibase.structure.DatabaseObject;
import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

/** Table model for structured object differences reported by Liquibase. */
@Getter
public class LiquibaseComparisonItemsTableModel extends DBNDynamicTableModel<LiquibaseComparisonItem> {
    private final LiquibaseOperationResult result;

    public LiquibaseComparisonItemsTableModel(LiquibaseOperationResult result) {
        super(LiquibaseComparisonItem.class, result.getComparisonItems());
        this.result = result;
        addColumn(txt("app.liquibase.column.DiscoveryOrder"), e -> getData().indexOf(e) + 1);
        addColumn(txt("app.liquibase.column.ObjectType"), e -> getObjectType(e));
        addColumn(txt("app.liquibase.column.SourceObject"), e -> getObjectName(e.getSourceObject()));
        addColumn(txt("app.liquibase.column.TargetObject"), e -> getObjectName(e.getTargetObject()));
        addColumn(txt("app.liquibase.column.ComparisonStatus"), e -> e.getComparisonStatus().getName());
        addColumn(txt("app.liquibase.column.Details"), e -> e.getMessage());
    }

    public void refresh() {
        setData(result.getComparisonItems());
    }

    private static String getObjectType(LiquibaseComparisonItem item) {
        DatabaseObject object = item.getSourceObject() == null ? item.getTargetObject() : item.getSourceObject();
        return object == null ? null : object.getObjectTypeName();
    }

    private static String getObjectName(DatabaseObject object) {
        return object == null ? null : object.getName();
    }
}
