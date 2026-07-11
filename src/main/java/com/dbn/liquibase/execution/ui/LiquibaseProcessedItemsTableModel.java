package com.dbn.liquibase.execution.ui;

import com.dbn.common.ui.table.DBNDynamicTableModel;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseProcessedItem;

/** Table model for items processed by a Liquibase operation. */
public class LiquibaseProcessedItemsTableModel extends DBNDynamicTableModel<LiquibaseProcessedItem> {
    LiquibaseProcessedItemsTableModel(LiquibaseExecutionResult result) {
        super(LiquibaseProcessedItem.class, result.getProcessedItems());
        addColumn("ID", e -> e.getId());
        addColumn("Author", e -> e.getAuthor());
        addColumn("File", e -> e.getFilePath());
        addColumn("Change Type", e -> e.getChangeType());
        addColumn("Status", e -> e.getStatus());
        addColumn("Message", e -> e.getMessage());
    }
}
