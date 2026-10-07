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

import com.dbn.data.editor.ui.TextFieldWithTextEditor;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.type.DBDataType;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTextField;

public class DatasetTableCellEditorWithTextEditor extends DatasetTableCellEditorWithSpecialEditor<TextFieldWithTextEditor> {
    public DatasetTableCellEditorWithTextEditor(DatasetEditorTable table) {
        super(table);
    }

    @Override
    protected TextFieldWithTextEditor createEditorComponent(DatasetEditorTable table) {
        return new TextFieldWithTextEditor(table.getProject(), table, null);
    }

    @Override
    public void prepareEditor(@NotNull DatasetEditorModelCell cell) {
        super.prepareEditor(cell);
        getEditorComponent().setContextObject(cell.getColumn());
        ColumnInfo columnInfo = cell.getColumnInfo();
        DBDataType dataType = columnInfo.getDataType();
        if (!dataType.isNative()) return;

        JTextField textField = getTextField();
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
}
