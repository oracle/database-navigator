package com.dbn.liquibase.execution.ui;

import com.dbn.common.ui.table.DBNDynamicTableModel;
import com.dbn.liquibase.execution.LiquibaseComparisonItem;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import liquibase.structure.DatabaseObject;
import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

/** Table model for structured object differences reported by Liquibase. */
@Getter
public class LiquibaseComparisonItemsTableModel extends DBNDynamicTableModel<LiquibaseComparisonItem> {
    private final LiquibaseExecutionResult result;

    LiquibaseComparisonItemsTableModel(LiquibaseExecutionResult result) {
        super(LiquibaseComparisonItem.class, result.getComparisonItems());
        this.result = result;
        addColumn(txt("app.liquibase.column.DiscoveryOrder"), e -> getData().indexOf(e) + 1);
        addColumn(txt("app.liquibase.column.ObjectType"), e -> getObjectType(e));
        addColumn(txt("app.liquibase.column.SourceObject"), e -> getObjectName(e.getSourceObject()));
        addColumn(txt("app.liquibase.column.TargetObject"), e -> getObjectName(e.getTargetObject()));
        addColumn(txt("app.liquibase.column.ComparisonStatus"), e -> e.getComparisonStatus().getName());
        addColumn(txt("app.liquibase.column.Details"), LiquibaseComparisonItem::getMessage);
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
