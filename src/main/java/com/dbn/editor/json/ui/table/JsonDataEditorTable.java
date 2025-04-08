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

import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.util.Messages;
import com.dbn.data.grid.ui.table.basic.BasicTableCellRenderer;
import com.dbn.data.grid.ui.table.basic.BasicTableGutter;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.model.DataModelCell;
import com.dbn.data.record.RecordViewInfo;
import com.dbn.data.sorting.SortDirection;
import com.dbn.data.value.ArrayValue;
import com.dbn.data.value.LargeObjectValue;
import com.dbn.data.value.ValueAdapter;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.editor.EditorProviderId;
import com.dbn.editor.data.DataLoadInstructions;
import com.dbn.editor.json.JsonDataEditor;
import com.dbn.editor.json.model.JsonDataEditorModel;
import com.dbn.editor.json.model.JsonDataEditorModelCell;
import com.dbn.editor.json.model.JsonDataEditorModelRow;
import com.dbn.editor.json.ui.JsonDataEditorErrorForm;
import com.dbn.editor.json.ui.JsonDataEditorForm;
import com.dbn.editor.json.ui.table.listener.JsonDataEditorMouseListener;
import com.dbn.object.DBColumn;
import com.dbn.object.DBJsonView;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.EventObject;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.editor.data.DataLoadInstruction.DELIBERATE_ACTION;
import static com.dbn.editor.data.DataLoadInstruction.PRESERVE_CHANGES;
import static com.dbn.editor.data.DataLoadInstruction.USE_CURRENT_FILTER;
import static com.dbn.editor.data.model.RecordStatus.INSERTING;
import static com.dbn.editor.data.model.RecordStatus.UPDATING;
import static com.dbn.nls.NlsResources.txt;

@Getter
@Setter
public class JsonDataEditorTable extends ResultSetTable<JsonDataEditorModel> {
    private static final DataLoadInstructions SORT_LOAD_INSTRUCTIONS = new DataLoadInstructions(USE_CURRENT_FILTER, PRESERVE_CHANGES, DELIBERATE_ACTION);
    private final WeakRef<JsonDataEditor> editor;

    private final JsonDataEditorMouseListener tableMouseListener = new JsonDataEditorMouseListener(this);

    private boolean editingEnabled = true;

    public JsonDataEditorTable(DBNForm parent, JsonDataEditor editor) throws SQLException {
        super(parent, createModel(editor), false,
                new RecordViewInfo(
                    editor.getJsonView().getQualifiedName(),
                    editor.getJsonView().getIcon()));
        setName(editor.getJsonView().getName());
        this.editor = WeakRef.of(editor);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setFillsViewportHeight(true);

        getSelectionModel().addListSelectionListener(getModel());
        addMouseListener(tableMouseListener);

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
    public void showRecordDetails() {
        JsonDataEditor editor = getEditor();
        boolean editorVisible = editor.isContentEditorVisible();
        editor.setContentEditorVisible(!editorVisible);
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

    protected boolean isLargeValuePopupActive() {
        return false;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        // table cells are not editable
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
        return null;
    }

    @Override
    public String getToolTipText(@NotNull MouseEvent e) {
        DataModelCell cell = getCellAtLocation(e.getPoint());
        if (cell instanceof JsonDataEditorModelCell) {
            JsonDataEditorModelCell editorTableCell = (JsonDataEditorModelCell) cell;

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

    @Override
    protected void regionalSettingsChanged() {
        // should not be affected by this event
    }

    @Override
    public void sort() {
        JsonDataEditorModel model = getModel();
        if (!isLoading() && !model.is(UPDATING)) {
            super.sort();
            if (!model.isResultSetExhausted()) {
                getEditor().loadData(SORT_LOAD_INSTRUCTIONS);
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
                    getEditor().loadData(SORT_LOAD_INSTRUCTIONS);
                }
                return sorted;
            }
        }
        return false;
    }

    @NotNull
    public JsonDataEditor getEditor() {
        return editor.ensure();
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
                DBJsonView jsonView = getJsonView();
                Progress.prompt(getProject(), jsonView, false,
                        txt("prc.dataEditor.title.RefreshingData"),
                        txt("prc.dataEditor.text.RefreshingDataFor", jsonView.getQualifiedNameWithType()),
                        progress -> {
                            try {
                                model.postInsertRecord(false, true, false);
                            } catch (SQLException e1) {
                                Messages.showErrorDialog(getProject(), "Could not create row in " + jsonView.getQualifiedNameWithType() + ".", e1);
                            }
                        });
            }
        }

        int selectedRow = getSelectedRow();
        JsonDataEditorModelRow row = model.getRowAtIndex(selectedRow);
        JsonDataEditorForm editorForm = getEditor().getEditorForm();
        if (row == null) {
            editorForm.selectRecord(null);
        } else {
            JsonDataEditorModelCell cell = row.getCellAtIndex(0);
            editorForm.selectRecord(cell);
        }


    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
        // single column, should not happen
    }

    /********************************************************
     *                        Popup                         *
     ********************************************************/
    public void showPopupMenu(
            MouseEvent e,
            JsonDataEditorModelCell cell,
            ColumnInfo columnInfo) {

        DBJsonView jsonView = getJsonView();
        DBColumn column = jsonView.getColumn(columnInfo.getName());
        if (isNotValid(column)) return;

        Progress.prompt(getProject(), jsonView, true,
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
