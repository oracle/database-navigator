package com.dbn.liquibase.execution.ui;

import com.dbn.common.ui.table.DBNDynamicTableModel;
import com.dbn.liquibase.execution.LiquibaseChangeSetItem;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import lombok.Getter;

import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.nls.NlsResources.txt;

/** Table model for change sets processed by a Liquibase operation. */
@Getter
public class LiquibaseChangeSetItemsTableModel extends DBNDynamicTableModel<LiquibaseChangeSetItem> {
    private final LiquibaseExecutionResult result;

    LiquibaseChangeSetItemsTableModel(LiquibaseExecutionResult result) {
        super(LiquibaseChangeSetItem.class, result.getChangeSetItems());
        this.result = result;
        addColumn(txt("app.liquibase.column.DiscoveryOrder"), e -> getData().indexOf(e) + 1);
        addColumn(txt("app.liquibase.column.ChangeSetId"), i -> i.getId());
        addColumn(txt("app.liquibase.column.ChangeSetAuthor"), i -> i.getAuthor());
        addColumn(txt("app.liquibase.column.ChangelogFile"), i -> i.getFilePath());
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
