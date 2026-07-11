package com.dbn.liquibase.execution.ui;

import com.dbn.common.ui.table.DBNDynamicTableModel;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseProcessedItem;
import liquibase.structure.core.Schema;

/** Table model for items processed by a Liquibase operation. */
public class LiquibaseProcessedItemsTableModel extends DBNDynamicTableModel<LiquibaseProcessedItem> {
    private final LiquibaseExecutionResult result;

    LiquibaseProcessedItemsTableModel(LiquibaseExecutionResult result) {
        super(LiquibaseProcessedItem.class, result.getProcessedItems());
        this.result = result;
        addColumn("Type", e -> e.getDatabaseObject().getObjectTypeName());
        addColumn("Schema", LiquibaseProcessedItemsTableModel::getSchemaName);
        addColumn("Object", e -> e.getDatabaseObject().getName());
        addColumn("Status", e -> e.getStatus());
        addColumn("Message", e -> e.getMessage());
    }

    public void refresh() {
        setData(result.getProcessedItems());
    }

    private static String getSchemaName(LiquibaseProcessedItem item) {
        Schema schema = item.getDatabaseObject().getSchema();
        return schema == null ? null : schema.getName();
    }
}
