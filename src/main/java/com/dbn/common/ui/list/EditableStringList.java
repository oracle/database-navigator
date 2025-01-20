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

package com.dbn.common.ui.list;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNEditableTable;
import com.dbn.common.ui.table.DBNTableGutter;
import com.dbn.common.ui.table.IndexTableGutter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class EditableStringList extends DBNEditableTable<EditableStringListModel> {
    private final boolean sorted;
    private final boolean indexed;

    public EditableStringList(@NotNull DBNComponent parent, boolean sorted, boolean indexed) {
        this(parent, new ArrayList<>(), sorted, indexed);
    }

    public EditableStringList(@NotNull DBNComponent parent, List<String> elements, boolean sorted, boolean indexed) {
        super(parent, new EditableStringListModel(elements, sorted), false);
        setTableHeader(null);
        this.sorted = sorted;
        this.indexed = indexed;
        addKeyListener(keyListener);
    }

    @Override
    public DBNTableGutter<?> createTableGutter() {
        return indexed ? new IndexTableGutter<>(this) : null;
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int rowIndex, int columnIndex) {
        JTextField component = (JTextField) super.prepareEditor(editor, rowIndex, columnIndex);
        component.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (e.getOppositeComponent() != EditableStringList.this) {
                    editor.stopCellEditing();
                }
            }
        });

        component.addKeyListener(keyListener);
        return component;
    }

    private final KeyAdapter keyListener = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
            super.keyTyped(e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.isConsumed()) return;

            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_BACK_SPACE) {
                String value = getValue(e);
                if (value != null && value.isEmpty()) {
                    e.consume();
                    removeRow();
                } else {
                    updateValue(e);
                }
            } else {
                updateValue(e);
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.isConsumed()) return;

            int selectedRow = getSelectedRow();
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_DOWN) {
                if (selectedRow == getModel().getRowCount() - 1) {
                    e.consume();
                    insertRow();
                }
            } else if (keyCode == KeyEvent.VK_ENTER && e.getModifiers() == 0) {
                e.consume();
                insertRow();
            } else if (keyCode == KeyEvent.VK_DELETE) {
                String value = getValue(e);
                if (value != null && value.isEmpty()) {
                    e.consume();
                    removeRow();
                } else {
                    updateValue(e);
                }
            }
        }

        private String getValue(KeyEvent e) {
            Object source = e.getSource();
            return source instanceof EditableStringList ?
                    (String) getModel().getValueAt(getSelectedRow(), 0) :
                    ((JTextField) source).getText();
        }

        private void updateValue(KeyEvent e) {
            if (e.getSource() instanceof JTextField) {
                String value = getValue(e);
                getModel().setValueAt(value, getSelectedRow(), 0);
            }
        }
    };

    @Override
    public Component getEditorComponent() {
        return super.getEditorComponent();
    }

    public List<String> getStringValues() {
        return getModel().getData();
    }

    public void setStringValues(List<String> stringValues) {
        setModel(new EditableStringListModel(stringValues, sorted));
    }


}
