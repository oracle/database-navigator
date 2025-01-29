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

package com.dbn.common.ui.table;

import com.dbn.common.color.Colors;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.util.Borders;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TableUtil;

import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.event.KeyEvent;

import static com.dbn.common.dispose.Failsafe.guarded;

public class DBNEditableTable<T extends DBNEditableTableModel> extends DBNTableWithGutter<T> {
    public DBNEditableTable(DBNComponent parent, T model, boolean showHeader) {
        super(parent, model, showHeader);
        setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        getSelectionModel().addListSelectionListener(selectionListener);
        setSelectionBackground(Colors.getTableBackground());
        setSelectionForeground(Colors.getTableForeground());
        setCellSelectionEnabled(true);
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        setDefaultRenderer(String.class, new DBNColoredTableCellRenderer() {
            @Override
            protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
                acquireState(table, false, false, row, column);
                SimpleTextAttributes attributes = SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES;
                if (selected && !table.isEditing()) {
                    attributes = SimpleTextAttributes.SELECTED_SIMPLE_CELL_ATTRIBUTES;
                }
                append(value == null ? "" : (String) value, attributes);
            }
        });
    }

    private final ListSelectionListener selectionListener = e -> {
        if (!e.getValueIsAdjusting() && getSelectedRowCount() == 1) {
            startCellEditing();
        }
    };

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
        super.columnSelectionChanged(e);
        JTableHeader tableHeader = getTableHeader();
        if (tableHeader != null && tableHeader.getDraggedColumn() == null) {
            if (!e.getValueIsAdjusting()) {
                startCellEditing();
            }
        }
    }

    private void startCellEditing() {
        if (getModel().getRowCount() > 0) {
            int selectedRow = getSelectedRow();
            int selectedColumn = getSelectedColumn();

            if (selectedRow > -1 && selectedColumn > -1) {
                TableCellEditor cellEditor = getCellEditor();
                if (cellEditor == null) {
                    editCellAt(selectedRow, selectedColumn);
                }
            }
        }
    }

    @Override
    public void editingStopped(ChangeEvent e) {
        guarded(() -> {
            checkDisposed();
            super.editingStopped(e);
            T model = getModel();
            model.notifyListeners(0, model.getRowCount(), 0);
        });
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int rowIndex, int columnIndex) {
        Component component = super.prepareEditor(editor, rowIndex, columnIndex);
        if (component instanceof JTextField) {
            JTextField textField = (JTextField) component;
            textField.setBorder(Borders.TEXT_FIELD_INSETS);

            //selectCell(rowIndex, columnIndex);

            Dispatch.run(() -> {
                if (getSelectedRow() != rowIndex) return;
                if (getSelectedColumn() != columnIndex) return;

                textField.requestFocus();
                textField.selectAll();
            });
        }
        return component;
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        if (processTabKeyEvent(e)) return true;
        return super.processKeyBinding(ks, e, condition, pressed);
    }

    private boolean processTabKeyEvent(KeyEvent e) {
        // tab navigation on first and last cells should cancel editing to allow
        // focus handover to previous respectively next focusable component
        if (!isEditing()) return false;

        if (e.getID() != KeyEvent.KEY_PRESSED) return false;
        if (e.getKeyCode() != KeyEvent.VK_TAB) return false;

        if (e.isShiftDown()) {
            if (Tables.isFirstCellSelected(this)) {
                removeEditor();
                return true;
            }
        } else {
            if (Tables.isLastCellSelected(this)) {
                removeEditor();
                return true;
            }
        }
        return false;
    }

    public void insertRow() {
        stopCellEditing();
        int rowIndex = getSelectedRow();
        T model = getModel();
        rowIndex = model.getRowCount() == 0 ? 0 : rowIndex + 1;
        model.insertRow(rowIndex);
        resizeAndRepaint();
        getSelectionModel().setSelectionInterval(rowIndex, rowIndex);
    }


    public void removeRow() {
        stopCellEditing();
        int selectedRow = getSelectedRow();
        T model = getModel();
        model.removeRow(selectedRow);
        resizeAndRepaint();

        if (model.getRowCount() == selectedRow && selectedRow > 0) {
            getSelectionModel().setSelectionInterval(selectedRow -1, selectedRow -1);
        }
    }

    public void moveRowUp() {
        int selectedRow = getSelectedRow();
        int selectedColumn = getSelectedColumn();

        TableUtil.moveSelectedItemsUp(this);
        selectCell(selectedRow -1, selectedColumn);
    }

    public void moveRowDown() {
        int selectedRow = getSelectedRow();
        int selectedColumn = getSelectedColumn();

        TableUtil.moveSelectedItemsDown(this);
        selectCell(selectedRow + 1, selectedColumn);
    }
}
