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
import com.dbn.migration.liquibase.execution.LiquibaseLockItem;
import com.dbn.migration.liquibase.operation.LiquibaseOperationResult;
import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

/** Table model for Liquibase changelog locks. */
@Getter
public class LiquibaseLockItemsTableModel extends DBNDynamicTableModel<LiquibaseLockItem> {
    private final LiquibaseOperationResult result;

    public LiquibaseLockItemsTableModel(LiquibaseOperationResult result) {
        super(LiquibaseLockItem.class, result.getLockItems());
        this.result = result;
        addColumn(txt("app.liquibase.column.DiscoveryOrder"), e -> getData().indexOf(e) + 1);
        addColumn(txt("app.liquibase.column.LockId"), e -> e.getId());
        addColumn(txt("app.liquibase.column.LockedBy"), e -> e.getLockedBy());
        addColumn(txt("app.liquibase.column.LockGranted"), e -> e.getLockGranted());
    }

    public void refresh() {
        setData(result.getLockItems());
    }
}
