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

import com.dbn.common.property.PropertyHolder;
import com.dbn.common.property.PropertyHolderBase;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNEditableTable;
import com.dbn.common.ui.table.DBNTableGutter;
import com.dbn.common.ui.table.IndexTableGutter;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.ui.list.ListProperty.EDITABLE;
import static com.dbn.common.ui.list.ListProperty.INDEXED;
import static com.dbn.common.ui.list.ListProperty.SORTED;
import static java.util.Collections.emptyList;

@Getter
@Setter
public class EditableStringList extends DBNEditableTable<EditableStringListModel> implements PropertyHolder<ListProperty> {
    @Delegate
    protected PropertyHolder<ListProperty> properties = PropertyHolderBase.intBase(ListProperty.VALUES);

    public EditableStringList(@NotNull DBNComponent parent, ListProperty ... properties) {
        this(parent, emptyList(), properties);
    }

    public EditableStringList(@NotNull DBNComponent parent, List<String> elements, ListProperty ... properties) {
        super(parent, new EditableStringListModel(), false);
        this.properties.set(properties, true);

        setTableHeader(null);
        setStringValues(elements);
        addKeyListener(keyListener);
    }

    public boolean isEditable() {
        return is(EDITABLE);
    }

    public boolean isIndexed() {
        return is(INDEXED);
    }

    @Override
    public DBNTableGutter<?> createTableGutter() {
        return isIndexed() ? new IndexTableGutter<>(this) : null;
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
            if (!isEditable()) return;
            if (e.isConsumed()) return;

            int selectedRow = getSelectedRow();
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_DOWN) {
                e.consume();
                if (selectedRow == getModel().getRowCount() - 1) {

                    insertRow();
                } else {
                    stopCellEditing();
                    selectCell(selectedRow + 1, 0);
                }
            } else if (keyCode == KeyEvent.VK_UP) {
                e.consume();
                if (selectedRow > 0) {
                    stopCellEditing();
                    selectCell(selectedRow - 1, 0);
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
        if (is(SORTED)) {
            stringValues = new ArrayList<>(stringValues);
            Collections.sort(stringValues);
        }
        setModel(new EditableStringListModel(stringValues));
    }


}
