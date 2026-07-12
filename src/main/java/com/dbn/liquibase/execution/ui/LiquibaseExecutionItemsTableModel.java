package com.dbn.liquibase.execution.ui;

import com.dbn.common.ui.table.DBNDynamicTableModel;
import com.dbn.liquibase.execution.LiquibaseExecutionItem;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import liquibase.structure.core.Schema;

/** Table model for items processed by a Liquibase operation. */
public class LiquibaseExecutionItemsTableModel extends DBNDynamicTableModel<LiquibaseExecutionItem> {
    private final LiquibaseExecutionResult result;

    LiquibaseExecutionItemsTableModel(LiquibaseExecutionResult result) {
        super(LiquibaseExecutionItem.class, result.getExecutionItems());
        this.result = result;
        addColumn("Type", e -> e.getDatabaseObject().getObjectTypeName());
        addColumn("Schema", e -> getSchemaName(e));
        addColumn("Object", e -> e.getDatabaseObject().getName());
        addColumn("Status", e -> e.getStatus());
        addColumn("Message", e -> e.getMessage());
    }

    public void refresh() {
        setData(result.getExecutionItems());
    }

    private static String getSchemaName(LiquibaseExecutionItem item) {
        Schema schema = item.getDatabaseObject().getSchema();
        return schema == null ? null : schema.getName();
    }
}
