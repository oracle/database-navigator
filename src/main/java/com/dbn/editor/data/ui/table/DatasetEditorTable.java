/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.editor.data.ui.table;

import com.dbn.common.Pair;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.property.PropertyHolder;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.table.DBNTableGutter;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Messages;
import com.dbn.data.grid.options.DataGridAuditColumnSettings;
import com.dbn.data.grid.ui.table.basic.BasicTableCellRenderer;
import com.dbn.data.grid.ui.table.basic.BasicTableGutter;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.model.DataModelCell;
import com.dbn.data.preview.LargeValuePreviewPopup;
import com.dbn.data.record.RecordViewInfo;
import com.dbn.data.sorting.SortDirection;
import com.dbn.data.value.ArrayValue;
import com.dbn.data.value.LargeObjectValue;
import com.dbn.data.value.ValueAdapter;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.editor.EditorProviderId;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.editor.data.DatasetLoadInstructions;
import com.dbn.editor.data.action.DatasetEditorTableActionGroup;
import com.dbn.editor.data.model.DatasetEditorModel;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.model.RecordStatus;
import com.dbn.editor.data.options.DataEditorGeneralSettings;
import com.dbn.editor.data.ui.DatasetEditorErrorForm;
import com.dbn.editor.data.ui.table.cell.DatasetTableCellEditor;
import com.dbn.editor.data.ui.table.cell.DatasetTableCellEditorFactory;
import com.dbn.editor.data.ui.table.listener.DatasetEditorHeaderMouseListener;
import com.dbn.editor.data.ui.table.listener.DatasetEditorKeyListener;
import com.dbn.editor.data.ui.table.listener.DatasetEditorMouseListener;
import com.dbn.editor.data.ui.table.renderer.DatasetEditorTableCellRenderer;
import com.dbn.editor.data.ui.table.renderer.DatasetEditorTableHeaderRenderer;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.ui.awt.RelativePoint;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.EventObject;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.editor.data.DatasetLoadInstruction.DELIBERATE_ACTION;
import static com.dbn.editor.data.DatasetLoadInstruction.PRESERVE_CHANGES;
import static com.dbn.editor.data.DatasetLoadInstruction.USE_CURRENT_FILTER;
import static com.dbn.editor.data.model.RecordStatus.INSERTING;
import static com.dbn.editor.data.model.RecordStatus.UPDATING;

public class DatasetEditorTable extends ResultSetTable<DatasetEditorModel> {
    private static final DatasetLoadInstructions SORT_LOAD_INSTRUCTIONS = new DatasetLoadInstructions(USE_CURRENT_FILTER, PRESERVE_CHANGES, DELIBERATE_ACTION);
    private final WeakRef<DatasetEditor> datasetEditor;

    private final DatasetTableCellEditorFactory cellEditorFactory = new DatasetTableCellEditorFactory();
    private final DatasetEditorMouseListener tableMouseListener = new DatasetEditorMouseListener(this);

    private @Getter @Setter boolean editingEnabled = true;

    public DatasetEditorTable(DBNForm parent, DatasetEditor datasetEditor) throws SQLException {
        super(parent, createModel(datasetEditor), false,
                new RecordViewInfo(
                    datasetEditor.getDataset().getQualifiedName(),
                    datasetEditor.getDataset().getIcon()));
        JTableHeader tableHeader = getTableHeader();
        tableHeader.setDefaultRenderer(new DatasetEditorTableHeaderRenderer());
        setName(datasetEditor.getDataset().getName());
        this.datasetEditor = WeakRef.of(datasetEditor);

        getSelectionModel().addListSelectionListener(getModel());
        addKeyListener(new DatasetEditorKeyListener(this));
        addMouseListener(tableMouseListener);

        tableHeader.addMouseListener(new DatasetEditorHeaderMouseListener(this));

        Disposer.register(this, cellEditorFactory);
        /*
        DataProvider dataProvider = datasetEditor.getDataProvider();
        ActionUtil.registerDataProvider(this, dataProvider, false);
        ActionUtil.registerDataProvider(getTableHeader(), dataProvider, false);
*/
        setAccessibleName(this, "Dataset Editor");
    }

    @Override
    protected BasicTableCellRenderer createCellRenderer() {
        return new DatasetEditorTableCellRenderer();
    }

    private static DatasetEditorModel createModel(DatasetEditor datasetEditor) throws SQLException {
        return new DatasetEditorModel(datasetEditor);
    }

    @NotNull
    public DBDataset getDataset() {
        return getModel().getDataset();
    }

    @Override
    protected BasicTableGutter<?> createTableGutter() {
        return new DatasetEditorTableGutter(this);
    }

    public boolean isInserting() {
        return getModel().is(INSERTING);
    }

    public void hideColumn(int columnIndex) {
        super.hideColumn(columnIndex);

        ColumnInfo columnInfo = getColumnInfo(columnIndex);
        getDatasetEditor().getColumnSetup().getColumnState(columnInfo.getName()).setVisible(false);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        return getCellRenderer();
    }

    @Override
    public void columnMoved(TableColumnModelEvent e) {
        int fromIndex = e.getFromIndex();
        int toIndex = e.getToIndex();
        if (fromIndex != toIndex) {
            getDatasetEditor().getColumnSetup().moveColumn(fromIndex, toIndex);
        }
        super.columnMoved(e);
    }

    @Override
    public void moveColumn(int column, int targetColumn) {
        super.moveColumn(column, targetColumn);
    }

    @Override
    public void editingStopped(ChangeEvent e) {
        try {
            DatasetTableCellEditor cellEditor = getCellEditor();
            if (cellEditor == null || !cellEditor.isEditable()) return;

            int rowIndex = editingRow;
            int columnIndex = editingColumn;

            DatasetEditorModelCell cell = cellEditor.getCell();
            if (cell == null) return;

            String editorTextValue = cellEditor.getCellEditorTextValue();
            Pair<Object, Throwable> result = Pair.create();
            try {
                result.first(cellEditor.getCellEditorValue());
            } catch (Throwable t) {
                conditionallyLog(t);
                result.first(editorTextValue);
                result.second(t);
            }

            performUpdate(rowIndex, columnIndex, () -> {
                cell.setTemporaryUserValue(editorTextValue);
                Throwable exception = result.second();
                Object value = result.first();
                if (exception == null) {
                    setValueAt(value, rowIndex, columnIndex);
                } else {
                    setValueAt(value, exception.getMessage(), rowIndex, columnIndex);
                }
            });
        } finally {
            removeEditor();
        }
    }

    public void performUpdate(int rowIndex, int columnIndex, Runnable runnable) {
        PropertyHolder<RecordStatus> scope = getUpdateScope(rowIndex, columnIndex);
        if (scope != null) {
            scope.set(UPDATING, true);
            Background.run(() -> {
                try {
                    runnable.run();
                } finally {
                    scope.set(UPDATING, false);
                    Dispatch.run(() -> {
                        DBNTableGutter tableGutter = getTableGutter();
                        UserInterface.repaint(tableGutter);
                        UserInterface.repaint(DatasetEditorTable.this);
                    });
                }
            });
        }
    }

    @Nullable
    private PropertyHolder<RecordStatus> getUpdateScope(int rowIndex, int columnIndex) {
        DatasetEditorModel model = getModel();
        if (rowIndex != -1 && columnIndex != -1) {
            return model.getCellAt(rowIndex, columnIndex);
        } else if (rowIndex > -1) {
            return model.getRowAtIndex(rowIndex);
        }
        return model;
    }

    public void showErrorPopup(@NotNull DatasetEditorModelCell cell) {
        Dispatch.run(() -> {
            checkDisposed();

            if (!isShowing()) {
                DBDataset dataset = getDataset();
                DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(getProject());
                editorManager.connectAndOpenEditor(dataset, EditorProviderId.DATA, false, true);
            }
            if (cell.getError() != null) {
                DatasetEditorErrorForm errorForm = new DatasetEditorErrorForm(cell);
                errorForm.show();
            }
        });
    }

    @Override
    public DatasetTableCellEditor getCellEditor() {
        TableCellEditor cellEditor = super.getCellEditor();
        return cellEditor instanceof DatasetTableCellEditor ? (DatasetTableCellEditor) cellEditor : null;
    }

    @Override
    public void clearSelection() {
        Dispatch.run(true, () -> DatasetEditorTable.super.clearSelection());
    }

    @Override
    public void removeEditor() {
        Dispatch.run(true, () -> DatasetEditorTable.super.removeEditor());
    }

    public void updateTableGutter() {
        Dispatch.run(true, () -> {
            DBNTableGutter tableGutter = getTableGutter();
            UserInterface.repaint(tableGutter);
        });
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        int modelRowIndex = rowIndex;//convertRowIndexToModel(rowIndex);
        int modelColumnIndex = convertColumnIndexToModel(columnIndex);
        if (modelRowIndex > -1 && modelColumnIndex > -1) {
            getModel().setValueAt(value, modelRowIndex, modelColumnIndex);
        }
    }

    public void setValueAt(Object value, String errorMessage, int rowIndex, int columnIndex) {
        int modelRowIndex = rowIndex;//convertRowIndexToModel(rowIndex);
        int modelColumnIndex = convertColumnIndexToModel(columnIndex);
        if (modelRowIndex > -1 && modelColumnIndex > -1) {
            getModel().setValueAt(value, errorMessage, modelRowIndex, modelColumnIndex);
        }
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int rowIndex, int columnIndex) {
        Component component = super.prepareEditor(editor, rowIndex, columnIndex);
        selectCell(rowIndex, columnIndex);

        if (editor instanceof DatasetTableCellEditor) {
            DatasetTableCellEditor cellEditor = (DatasetTableCellEditor) editor;
            int modelRowIndex = convertRowIndexToModel(rowIndex);
            int modelColumnIndex = convertColumnIndexToModel(columnIndex);
            DatasetEditorModelCell cell = (DatasetEditorModelCell) getCellAtPosition(modelRowIndex, modelColumnIndex);
            if (cell != null) {
                cellEditor.prepareEditor(cell);
            }
        }
        return component;
    }

    @Override
    public boolean editCellAt(final int row, final int column, final EventObject e) {
        return super.editCellAt(row, column, e);
    }

    @Override
    public TableCellEditor getCellEditor(int rowIndex, int columnIndex) {
        if (isLoading()) {
            return null;
        }

        int modelColumnIndex = convertColumnIndexToModel(columnIndex);
        ColumnInfo columnInfo = getColumnInfo(modelColumnIndex);

        DataGridAuditColumnSettings auditColumnSettings = getDataGridSettings().getAuditColumnSettings();
        if (!auditColumnSettings.isAllowEditing()) {
            boolean auditColumn = auditColumnSettings.isAuditColumn(columnInfo.getName());
            if (auditColumn) return null;
        }

        return cellEditorFactory.getCellEditor(columnInfo, this);
    }

    @Override
    public TableCellEditor getDefaultEditor(Class<?> columnClass) {
        return super.getDefaultEditor(columnClass);
    }

    @Override
    protected void initLargeValuePopup(LargeValuePreviewPopup viewer) {
        super.initLargeValuePopup(viewer);
    }

    @Override
    protected boolean isLargeValuePopupActive() {
        DataEditorGeneralSettings generalSettings = getDatasetEditor().getSettings().getGeneralSettings();
        return generalSettings.getLargeValuePreviewActive().value();
    }

    @Override
    public int getColumnWidthBuffer() {
        return isReadonly() || getModel().isReadonly() ? 22 : 36;
    }

    private boolean isReadonly() {
        return datasetEditor != null && getDatasetEditor().isReadonly();
    }

    @Override
    public String getToolTipText(@NotNull MouseEvent e) {
        DataModelCell cell = getCellAtLocation(e.getPoint());
        if (cell instanceof DatasetEditorModelCell) {
            DatasetEditorModelCell editorTableCell = (DatasetEditorModelCell) cell;
/*            if (event.isControlDown() && isNavigableCellAtMousePosition()) {
                DBColumn column = editorTableCell.getColumnInfo().getColumn();
                DBColumn foreignKeyColumn = column.getForeignKeyColumn();
                if (foreignKeyColumn != null) {
                    StringBuilder text = new StringBuilder("<html>");
                    text.append("Show ");
                    text.append(foreignKeyColumn.getDataset().getName());
                    text.append(" record");
                    text.append("</html>");
                    return text.toString();
                }
            }*/

            if (editorTableCell.hasError()) {
                StringBuilder text = new StringBuilder("<html>");

                if (editorTableCell.hasError()) {
                    text.append(editorTableCell.getError().getMessage());
                    text.append("<br>");
                }

                if (editorTableCell.isModified() && !(editorTableCell.getUserValue() instanceof ValueAdapter)) {
                    text.append("<br>Original value: <b>");
                    text.append(editorTableCell.getOriginalUserValue());
                    text.append("</b>");
                }

                text.append("</html>");

                return text.toString();
            }

            if (editorTableCell.isModified() && !e.isControlDown()) {
                Object userValue = editorTableCell.getUserValue();
                if (userValue instanceof ArrayValue) {
                    return "ARRAY value has changed";
                } else  if (userValue instanceof LargeObjectValue) {
                    LargeObjectValue largeObjectValue = (LargeObjectValue) userValue;
                    return largeObjectValue.getGenericDataType() + " content has changed";
                } else {
                    return "<html>Original value: <b>" + editorTableCell.getOriginalUserValue() + "</b></html>";
                }

            }
        }
        return super.getToolTipText(e);
    }

    public void fireEditingCancel() {
        if (isEditing()) {
            Dispatch.run(true, () -> cancelEditing());
        }
    }

    public void cancelEditing() {
        if (isEditing()) {
            TableCellEditor cellEditor = getCellEditor();
            if (cellEditor != null) {
                cellEditor.cancelCellEditing();
            }
        }
    }

    @Override
    protected void regionalSettingsChanged() {
        cancelEditing();
        super.regionalSettingsChanged();
    }

    @Override
    public void sort() {
        DatasetEditorModel model = getModel();
        if (!isLoading() && !model.is(UPDATING)) {
            super.sort();
            if (!model.isResultSetExhausted()) {
                getDatasetEditor().loadData(SORT_LOAD_INSTRUCTIONS);
            }
            resizeAndRepaint();
        }
    }

    @Override
    public boolean sort(int columnIndex, SortDirection sortDirection, boolean keepExisting) {
        int modelColumnIndex = convertColumnIndexToModel(columnIndex);
        DatasetEditorModel model = getModel();
        ColumnInfo columnInfo = model.getColumnInfo(modelColumnIndex);
        if (columnInfo.isSortable()) {
            if (!isLoading() && !model.is(UPDATING)) {
                boolean sorted = super.sort(columnIndex, sortDirection, keepExisting);

                if (sorted && !model.isResultSetExhausted()) {
                    getDatasetEditor().loadData(SORT_LOAD_INSTRUCTIONS);
                }
                return sorted;
            }
        }
        return false;
    }

    @NotNull
    public DatasetEditor getDatasetEditor() {
        return datasetEditor.ensure();
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (e.isControlDown() && isNavigableCellAtMousePosition()) {
            Mouse.processMouseEvent(e, tableMouseListener);
        } else {
            super.processMouseEvent(e);
        }
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (e.isControlDown() && e.getID() != MouseEvent.MOUSE_DRAGGED && isNavigableCellAtMousePosition()) {
            setCursor(Cursors.handCursor());
            DatasetEditorModelCell cell = (DatasetEditorModelCell) getCellAtMouseLocation();
            if (cell != null) {
                DBColumn column = cell.getColumn();
                DBColumn foreignKeyColumn = column.getForeignKeyColumn();
                if (foreignKeyColumn != null) {
                    setToolTipText("<html>Show referenced <b>" + foreignKeyColumn.getDataset().getQualifiedName() + "</b> record<html>");
                }
            }
        } else {
            super.processMouseMotionEvent(e);
            setCursor(Cursors.defaultCursor());
            setToolTipText(null);
        }
    }

    private boolean isNavigableCellAtMousePosition() {
        DatasetEditorModelCell cell = (DatasetEditorModelCell) getCellAtMouseLocation();
        return cell != null && cell.isNavigable();
    }

    /**********************************************************
     *                  ListSelectionListener                 *
     **********************************************************/
    @Override
    public void valueChanged(ListSelectionEvent e) {
        super.valueChanged(e);
        if (e.getValueIsAdjusting()) return;

        DatasetEditorModel model = getModel();
        if (model.is(INSERTING)) {
            int insertRowIndex = getModel().getInsertRowIndex();
            if (insertRowIndex != -1 && (insertRowIndex == e.getFirstIndex() || insertRowIndex == e.getLastIndex()) && getSelectedRow() != insertRowIndex) {
                DBDataset dataset = getDataset();
                Progress.prompt(getProject(), dataset, false,
                        "Refreshing data",
                        "Refreshing data for " + dataset.getQualifiedNameWithType(),
                        progress -> {
                            try {
                                model.postInsertRecord(false, true, false);
                            } catch (SQLException e1) {
                                Messages.showErrorDialog(getProject(), "Could not create row in " + dataset.getQualifiedNameWithType() + ".", e1);
                            }
                        });
            }
        }
        startCellEditing(e);

    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
        JTableHeader tableHeader = getTableHeader();
        if (tableHeader != null && tableHeader.getDraggedColumn() == null) {
            super.columnSelectionChanged(e);
            if (!e.getValueIsAdjusting()) {
                startCellEditing(e);
            }
        }
    }

    private void startCellEditing(ListSelectionEvent e) {
        if (!isEditing()) {
            int[] selectedRows = getSelectedRows();
            int[] selectedColumns = getSelectedColumns();

            if (selectedRows.length == 1 && selectedColumns.length == 1) {
                int selectedRow = selectedRows[0];
                int selectedColumn = selectedColumns[0];
                if (getModel().isCellEditable(selectedRow, selectedColumn)) {
                    editCellAt(selectedRow, selectedColumn);
                }
            }
        }
    }

    public RelativePoint getColumnHeaderLocation(DBColumn column) {
        int columnIndex = convertColumnIndexToView(getModel().getHeader().indexOfColumn(column));
        Rectangle rectangle = getTableHeader().getHeaderRect(columnIndex);
        Point point = new Point(
                (int) (rectangle.getX() + rectangle.getWidth() - 20),
                (int) (rectangle.getY() + rectangle.getHeight()) + 20);
        return new RelativePoint(getTableHeader(), point);
    }

    /********************************************************
     *                        Popup                         *
     ********************************************************/
    public void showPopupMenu(
            MouseEvent e,
            DatasetEditorModelCell cell,
            ColumnInfo columnInfo) {

        DBDataset dataset = getDataset();
        DBColumn column = dataset.getColumn(columnInfo.getName());
        if (isNotValid(column)) return;

        Progress.prompt(getProject(), dataset, true,
                "Loading column information",
                "Loading details of " + column.getQualifiedNameWithType(),
                progress -> {
                    ActionGroup actionGroup = new DatasetEditorTableActionGroup(getDatasetEditor(), cell, columnInfo);
                    progress.checkCanceled();

                    ActionPopupMenu actionPopupMenu = Actions.createActionPopupMenu(DatasetEditorTable.this, actionGroup);
                    JPopupMenu popupMenu = actionPopupMenu.getComponent();
                    Dispatch.run(() -> {
                        Component component = (Component) e.getSource();
                        if (!component.isShowing()) return;

                        int x = e.getX();
                        int y = e.getY();
                        if (x >= 0 && x < component.getWidth() && y >= 0 && y < component.getHeight()) {
                            popupMenu.show(component, x, y);
                        }
                    });
                });
    }
}
