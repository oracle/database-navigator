/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.editor.json.ui.table;

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
import com.dbn.common.util.Messages;
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
import com.dbn.editor.data.DatasetLoadInstructions;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.model.RecordStatus;
import com.dbn.editor.json.JsonDataEditor;
import com.dbn.editor.json.model.JsonDataEditorModel;
import com.dbn.editor.json.model.JsonDataEditorModelCell;
import com.dbn.editor.json.model.JsonDataEditorModelRow;
import com.dbn.editor.json.ui.JsonDataEditorErrorForm;
import com.dbn.editor.json.ui.table.listener.JsonDataEditorMouseListener;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.dbn.object.DBJsonView;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.EventObject;
import java.util.Objects;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.editor.data.DatasetLoadInstruction.DELIBERATE_ACTION;
import static com.dbn.editor.data.DatasetLoadInstruction.PRESERVE_CHANGES;
import static com.dbn.editor.data.DatasetLoadInstruction.USE_CURRENT_FILTER;
import static com.dbn.editor.data.model.RecordStatus.INSERTING;
import static com.dbn.editor.data.model.RecordStatus.UPDATING;
import static com.dbn.nls.NlsResources.txt;

@Getter
@Setter
public class JsonDataEditorTable extends ResultSetTable<JsonDataEditorModel> {
    private static final DatasetLoadInstructions SORT_LOAD_INSTRUCTIONS = new DatasetLoadInstructions(USE_CURRENT_FILTER, PRESERVE_CHANGES, DELIBERATE_ACTION);
    private final WeakRef<JsonDataEditor> jsonDataEditor;

    private final JsonDataEditorMouseListener tableMouseListener = new JsonDataEditorMouseListener(this);

    private boolean editingEnabled = true;

    public JsonDataEditorTable(DBNForm parent, JsonDataEditor jsonDataEditor) throws SQLException {
        super(parent, createModel(jsonDataEditor), false,
                new RecordViewInfo(
                    jsonDataEditor.getJsonView().getQualifiedName(),
                    jsonDataEditor.getJsonView().getIcon()));
        JTableHeader tableHeader = getTableHeader();
        //tableHeader.setDefaultRenderer(new DatasetEditorTableHeaderRenderer());
        setName(jsonDataEditor.getJsonView().getName());
        this.jsonDataEditor = WeakRef.of(jsonDataEditor);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setFillsViewportHeight(true);

        getSelectionModel().addListSelectionListener(getModel());
        addMouseListener(tableMouseListener);

        /*
        DataProvider dataProvider = datasetEditor.getDataProvider();
        ActionUtil.registerDataProvider(this, dataProvider, false);
        ActionUtil.registerDataProvider(getTableHeader(), dataProvider, false);
*/
        setAccessibleName(this, "Json Data Editor");
        setFocusable(true);
        setRequestFocusEnabled(true);
    }

    @Override
    protected BasicTableCellRenderer createCellRenderer() {
        return new JsonDataEditorTableCellRenderer();
    }

    private static JsonDataEditorModel createModel(JsonDataEditor jsonDataEditor) throws SQLException {
        return new JsonDataEditorModel(jsonDataEditor);
    }

    @Override
    public void adjustColumnWidths() {
        // auto-resize to full width (override default behavior)
    }

    @NotNull
    public DBJsonView getJsonView() {
        return getModel().getJsonView();
    }

    @Override
    protected BasicTableGutter<?> createTableGutter() {
        return new JsonDataEditorTableGutter(this);
    }

    public boolean isInserting() {
        return getModel().is(INSERTING);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        return getCellRenderer();
    }

    @Override
    public void moveColumn(int column, int targetColumn) {}

    @Override
    public void editingStopped(ChangeEvent e) {}

    public void performUpdate(int rowIndex, int columnIndex, Runnable runnable) {
        PropertyHolder<RecordStatus> scope = getUpdateScope(rowIndex, columnIndex);
        if (scope != null) {
            scope.set(UPDATING, true);
            Background.run(() -> {
                try {
                    runnable.run();
                } finally {
                    scope.set(UPDATING, false);
                    dispatch(() -> {
                        DBNTableGutter tableGutter = getTableGutter();
                        UserInterface.repaint(tableGutter);
                        UserInterface.repaint(JsonDataEditorTable.this);
                    });
                }
            });
        }
    }

    @Nullable
    private PropertyHolder<RecordStatus> getUpdateScope(int rowIndex, int columnIndex) {
        JsonDataEditorModel model = getModel();
        if (rowIndex != -1 && columnIndex != -1) {
            return model.getCellAt(rowIndex, columnIndex);
        } else if (rowIndex > -1) {
            return model.getRowAtIndex(rowIndex);
        }
        return model;
    }

    public void showErrorPopup(@NotNull JsonDataEditorModelCell cell) {
        dispatch(() -> {
            checkDisposed();

            if (!isShowing()) {
                DBJsonView jsonView = getJsonView();
                DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(getProject());
                editorManager.connectAndOpenEditor(jsonView, EditorProviderId.DATA, false, true);
            }
            if (cell.getError() != null) {
                JsonDataEditorErrorForm errorForm = new JsonDataEditorErrorForm(cell);
                errorForm.show();
            }
        });
    }

    @Override
    public TableCellEditor getCellEditor() {
        return null;
    }

    @Override
    public void clearSelection() {
        Dispatch.run(true, () -> JsonDataEditorTable.super.clearSelection());
    }

    @Override
    public void removeEditor() {}

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
        return null;
    }

    @Override
    public boolean editCellAt(final int row, final int column, final EventObject e) {
        return false;
    }

    @Override
    public TableCellEditor getCellEditor(int rowIndex, int columnIndex) {
        return null;
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
    public int getColumnWidthBuffer() {
        return isReadonly() || getModel().isReadonly() ? 22 : 36;
    }

    private boolean isReadonly() {
        return jsonDataEditor != null && getJsonDataEditor().isReadonly();
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
        JsonDataEditorModel model = getModel();
        if (!isLoading() && !model.is(UPDATING)) {
            super.sort();
            if (!model.isResultSetExhausted()) {
                getJsonDataEditor().loadData(SORT_LOAD_INSTRUCTIONS);
            }
            resizeAndRepaint();
        }
    }

    @Override
    public boolean sort(int columnIndex, SortDirection sortDirection, boolean keepExisting) {
        int modelColumnIndex = convertColumnIndexToModel(columnIndex);
        JsonDataEditorModel model = getModel();
        ColumnInfo columnInfo = model.getColumnInfo(modelColumnIndex);
        if (columnInfo.isSortable()) {
            if (!isLoading() && !model.is(UPDATING)) {
                boolean sorted = super.sort(columnIndex, sortDirection, keepExisting);

                if (sorted && !model.isResultSetExhausted()) {
                    getJsonDataEditor().loadData(SORT_LOAD_INSTRUCTIONS);
                }
                return sorted;
            }
        }
        return false;
    }

    @NotNull
    public JsonDataEditor getJsonDataEditor() {
        return jsonDataEditor.ensure();
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

        JsonDataEditorModel model = getModel();
        if (model.is(INSERTING)) {
            int insertRowIndex = getModel().getInsertRowIndex();
            if (insertRowIndex != -1 && (insertRowIndex == e.getFirstIndex() || insertRowIndex == e.getLastIndex()) && getSelectedRow() != insertRowIndex) {
                DBDataset dataset = getJsonView();
                Progress.prompt(getProject(), dataset, false,
                        txt("prc.dataEditor.title.RefreshingData"),
                        txt("prc.dataEditor.text.RefreshingDataFor", dataset.getQualifiedNameWithType()),
                        progress -> {
                            try {
                                model.postInsertRecord(false, true, false);
                            } catch (SQLException e1) {
                                Messages.showErrorDialog(getProject(), "Could not create row in " + dataset.getQualifiedNameWithType() + ".", e1);
                            }
                        });
            }
        }

        int selectedRow = getSelectedRow();
        JsonDataEditorModelRow row = model.getRowAtIndex(selectedRow);
        if (row == null) {
            getJsonDataEditor().setJsonEditorContent("");
        } else {
            Object userValue = row.getCellAtIndex(0).getUserValue();
            getJsonDataEditor().setJsonEditorContent(Objects.toString(userValue));
        }


    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
        JTableHeader tableHeader = getTableHeader();
        if (tableHeader != null && tableHeader.getDraggedColumn() == null) {
            super.columnSelectionChanged(e);
            if (!e.getValueIsAdjusting()) {
                // TODO populate JSON editor
            }
        }
    }

    /********************************************************
     *                        Popup                         *
     ********************************************************/
    public void showPopupMenu(
            MouseEvent e,
            JsonDataEditorModelCell cell,
            ColumnInfo columnInfo) {

        DBDataset dataset = getJsonView();
        DBColumn column = dataset.getColumn(columnInfo.getName());
        if (isNotValid(column)) return;

        Progress.prompt(getProject(), dataset, true,
                txt("prc.dataEditor.title.LoadingColumnInformation"),
                txt("prc.dataEditor.text.LoadingDetailsOf", column.getQualifiedNameWithType()),
                progress -> {
/*
                    ActionGroup actionGroup = new DatasetEditorTableActionGroup(getJsonDataEditor(), cell, columnInfo);
                    progress.checkCanceled();

                    ActionPopupMenu actionPopupMenu = Actions.createActionPopupMenu(JsonDataEditorTable.this, actionGroup);
                    JPopupMenu popupMenu = actionPopupMenu.getComponent();
                    dispatch(() -> {
                        Component component = (Component) e.getSource();
                        if (!component.isShowing()) return;

                        int x = e.getX();
                        int y = e.getY();
                        if (x >= 0 && x < component.getWidth() && y >= 0 && y < component.getHeight()) {
                            popupMenu.show(component, x, y);
                        }
                    });
*/
                });
    }
}
