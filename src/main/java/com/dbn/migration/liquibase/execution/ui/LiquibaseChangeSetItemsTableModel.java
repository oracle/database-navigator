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
import com.dbn.migration.liquibase.execution.LiquibaseChangeSetItem;
import com.dbn.migration.liquibase.operation.LiquibaseOperationResult;
import lombok.Getter;

import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.nls.NlsResources.txt;

/** Table model for change sets processed by a Liquibase operation. */
@Getter
public class LiquibaseChangeSetItemsTableModel extends DBNDynamicTableModel<LiquibaseChangeSetItem> {
    private final LiquibaseOperationResult result;

    public LiquibaseChangeSetItemsTableModel(LiquibaseOperationResult result) {
        super(LiquibaseChangeSetItem.class, result.getChangeSetItems());
        this.result = result;
        addColumn(txt("app.liquibase.column.DiscoveryOrder"), e -> getData().indexOf(e) + 1);
        addColumn(txt("app.liquibase.column.ChangeSetId"), i -> i.getId());
        addColumn(txt("app.liquibase.column.ChangeSetAuthor"), i -> i.getAuthor());
        //addColumn(txt("app.liquibase.column.ChangelogFile"), i -> i.getFilePath());
        addColumn(txt("app.shared.column.Description"), e -> e.getDescription());
        addColumn(txt("app.liquibase.column.CalculatedChecksum"), e -> e.getCalculatedChecksum());
        addColumn(txt("app.liquibase.column.StoredChecksum"), e -> e.getStoredChecksum());
        addColumn(txt("app.liquibase.column.ChecksumStatus"), e ->
                e.getChecksumStatus() == null ? null : e.getChecksumStatus().getName());
        addColumn(txt("app.liquibase.column.Duration"), e -> presentableDuration(e.getProcessingDuration(), true));
        addColumn(txt("app.liquibase.column.Status"), e -> e.getStatus().getName());
        addColumn(txt("app.liquibase.column.Details"), e -> e.getMessage());
    }

    public void refresh() {
        setData(result.getChangeSetItems());
    }
}
