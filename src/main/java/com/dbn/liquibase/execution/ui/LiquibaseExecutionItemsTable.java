package com.dbn.liquibase.execution.ui;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNDynamicTableCellRenderer;
import com.dbn.common.ui.table.DBNTableGutter;
import com.dbn.common.ui.table.DBNTableTransferHandler;
import com.dbn.common.ui.table.DBNTableWithGutter;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.TableModel;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.Borderless.markBorderless;
import static com.dbn.nls.NlsResources.txt;

/** Table displaying structured items processed by a Liquibase operation. */
public class LiquibaseExecutionItemsTable extends DBNTableWithGutter<LiquibaseExecutionItemsTableModel> {
    public LiquibaseExecutionItemsTable(@NotNull DBNComponent parent, LiquibaseExecutionItemsTableModel model) {
        super(parent, model, true);
        setCellSelectionEnabled(true);
        setDefaultRenderer(Object.class, new DBNDynamicTableCellRenderer());
        setTransferHandler(DBNTableTransferHandler.INSTANCE);
        initTableSorter();
        markBorderless(this);
        setAccessibleName(this, txt("app.liquibase.aria.ProcessedItems"));
    }

    @Override
    protected DBNTableGutter<?> createTableGutter() {
        return new DBNTableGutter<>(this);
    }

    @Override
    public void setModel(@NotNull TableModel dataModel) {
        super.setModel(dataModel);
        initTableSorter();
    }
}
