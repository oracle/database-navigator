package com.dbn.liquibase.execution.ui;

import com.dbn.common.ui.table.DBNDynamicTableModel;
import com.dbn.liquibase.execution.LiquibaseSnapshotItem;
import com.dbn.liquibase.operation.LiquibaseOperationResult;
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
