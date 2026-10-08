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
import com.dbn.migration.liquibase.execution.LiquibaseSnapshotItem;
import com.dbn.migration.liquibase.operation.LiquibaseOperationResult;
import liquibase.structure.core.Schema;
import lombok.Getter;

import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.nls.NlsResources.txt;

/** Table model for database objects discovered during a Liquibase snapshot. */
@Getter
public class LiquibaseSnapshotItemsTableModel extends DBNDynamicTableModel<LiquibaseSnapshotItem> {
    private final LiquibaseOperationResult result;

    public LiquibaseSnapshotItemsTableModel(LiquibaseOperationResult result) {
        super(LiquibaseSnapshotItem.class, result.getSnapshotItems());
        this.result = result;
        addColumn(txt("app.liquibase.column.DiscoveryOrder"), e -> getData().indexOf(e) + 1);
        addColumn(txt("app.liquibase.column.Schema"), e -> getSchemaName(e));
        addColumn(txt("app.liquibase.column.ObjectType"), e -> e.getDatabaseObject().getObjectTypeName());
        addColumn(txt("app.liquibase.column.ObjectName"), e -> e.getDatabaseObject().getName());
        addColumn(txt("app.liquibase.column.Duration"), e -> presentableDuration(e.getProcessingDuration(), true));
        addColumn(txt("app.liquibase.column.Status"), e -> e.getStatus().getName());
        addColumn(txt("app.liquibase.column.Details"), e -> e.getMessage());
    }

    public void refresh() {
        setData(result.getSnapshotItems());
    }

    private static String getSchemaName(LiquibaseSnapshotItem item) {
        Schema schema = item.getDatabaseObject().getSchema();
        return schema == null ? null : schema.getName();
    }
}
