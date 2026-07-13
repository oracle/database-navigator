package com.dbn.liquibase.execution.ui;

import com.dbn.common.ui.table.DBNDynamicTableModel;
import com.dbn.liquibase.execution.LiquibaseExecutionItem;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.object.DBSchema;
import liquibase.structure.core.Schema;

import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.nls.NlsResources.txt;

/** Table model for items processed by a Liquibase operation. */
public class LiquibaseExecutionItemsTableModel extends DBNDynamicTableModel<LiquibaseExecutionItem> {
    private final LiquibaseExecutionResult result;

    LiquibaseExecutionItemsTableModel(LiquibaseExecutionResult result) {
        super(LiquibaseExecutionItem.class, result.getExecutionItems());
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
        setData(result.getExecutionItems());
    }

    DBSchema getSchema() {
        return result.getSchema();
    }

    private static String getSchemaName(LiquibaseExecutionItem item) {
        Schema schema = item.getDatabaseObject().getSchema();
        return schema == null ? null : schema.getName();
    }
}
