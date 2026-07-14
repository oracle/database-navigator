package com.dbn.liquibase.execution.ui;

import com.dbn.common.thread.Progress;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNDynamicTableCellRenderer;
import com.dbn.common.ui.table.DBNTableGutter;
import com.dbn.common.ui.table.DBNTableTransferHandler;
import com.dbn.common.ui.table.DBNTableWithGutter;
import com.dbn.common.ui.util.Keyboard;
import com.dbn.common.ui.util.Mouse;
import com.dbn.liquibase.execution.LiquibaseExecutionItem;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.TableModel;
import java.awt.event.KeyEvent;

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
        Mouse.onMouseDoubleClick(this, event -> navigateToRow(rowAtPoint(event.getPoint())));
        Keyboard.onKeyPress(this, KeyEvent.VK_SPACE, event -> navigateToSelectedRow());
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

    private void navigateToRow(int viewRow) {
        if (viewRow < 0) return;

        int modelRow = convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= getModel().getRowCount()) return;

        LiquibaseExecutionItem item = getModel().getData(modelRow);
        DBSchema schema = getModel().getResult().getRelevantSchema();
        Progress.prompt(
                schema.getProject(),
                schema,
                true,
                txt("prc.databaseBrowser.title.LoadingObjectReferences"),
                txt("prc.databaseBrowser.text.LoadingReferencesOf", item.getDatabaseObject().getName()),
                progress -> {
                    progress.checkCanceled();
                    DBObject object = item.resolveBrowserObject(schema);
                    progress.checkCanceled();
                    if (object != null) object.navigate(true);
                });
    }

    private void navigateToSelectedRow() {
        int viewRow = getSelectedRow();
        navigateToRow(viewRow);
    }
}
