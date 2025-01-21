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

package com.dbn.editor.data.ui.table.cell;

import com.dbn.common.color.Colors;
import com.dbn.common.ui.misc.DBNButton;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Keyboard;
import com.dbn.data.editor.ui.TextFieldWithTextEditor;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.type.DBDataType;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Shortcut;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.KeyEvent;

public class DatasetTableCellEditorWithTextEditor extends DatasetTableCellEditor {
    public DatasetTableCellEditorWithTextEditor(DatasetEditorTable table) {
        super(table, createTextField(table));
        TextFieldWithTextEditor editorComponent = getEditorComponent();
        JTextField textField = editorComponent.getTextField();
        textField.setBorder(Borders.EMPTY_BORDER);
    }

    private static TextFieldWithTextEditor createTextField(DatasetEditorTable table) {
        return new TextFieldWithTextEditor(table.getProject()) {
            @Override
            public void setEditable(boolean editable) {
                super.setEditable(editable);
                Color background = getTextField().getBackground();
                setBackground(background);
                getButton().setBackground(background);
            }

            @Override
            public JComponent createButton(Icon icon, String name) {
                DBNButton button = new DBNButton(icon, name);
                button.setBorder(Borders.insetBorder(1));
                button.setBackground(Colors.getTableBackground());
                int rowHeight = table.getRowHeight();
                button.setPreferredSize(new Dimension(Math.max(20, rowHeight), rowHeight - 2));
                button.getParent().setBackground(getTextField().getBackground());
                table.addPropertyChangeListener(e -> {
                    Object newProperty = e.getNewValue();
                    if (newProperty instanceof Font) {
                        int rowHeight1 = table.getRowHeight();
                        button.setPreferredSize(new Dimension(Math.max(20, rowHeight1), table.getRowHeight() - 2));
                    }
                });
                return button;
            }
        };
    }

    @Override
    @NotNull
    public TextFieldWithTextEditor getEditorComponent() {
        return (TextFieldWithTextEditor) super.getEditorComponent();
    }

    @Override
    public void prepareEditor(@NotNull DatasetEditorModelCell cell) {
        getEditorComponent().setUserValueHolder(cell);
        setCell(cell);
        ColumnInfo columnInfo = cell.getColumnInfo();
        DBDataType dataType = columnInfo.getDataType();
        if (!dataType.isNative()) return;

        JTextField textField = getTextField();
        highlight(cell.hasError() ? HIGHLIGHT_TYPE_ERROR : HIGHLIGHT_TYPE_NONE);
        if (dataType.getNativeType().isLargeObject()) {
            setEditable(false);
        } else {
            Object object = cell.getUserValue();
            String userValue = object == null ? null :
                    object instanceof String ? (String) object
                    : object.toString();
            setEditable(userValue == null || (userValue.length() < 1000 && userValue.indexOf('\n') == -1));
        }
        selectText(textField);
    }

    @Override
    public void setEditable(boolean editable) {
        TextFieldWithTextEditor editorComponent = getEditorComponent();
        editorComponent.setEditable(editable);
    }

    /********************************************************
     *                      KeyListener                     *
     ********************************************************/
    @Override
    public void keyPressed(KeyEvent keyEvent) {
        Shortcut[] shortcuts = Keyboard.getShortcuts(IdeActions.ACTION_SHOW_INTENTION_ACTIONS);
        if (!keyEvent.isConsumed() && Keyboard.match(shortcuts, keyEvent)) {
            keyEvent.consume();
            getEditorComponent().openEditor();
        } else {
            super.keyPressed(keyEvent);
        }
    }

}
