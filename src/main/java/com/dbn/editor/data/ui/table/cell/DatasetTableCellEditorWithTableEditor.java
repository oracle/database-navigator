/*
 * Copyright 2026 Oracle and/or its affiliates
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

import com.dbn.data.editor.ui.TextFieldWithTableEditor;
import com.dbn.data.value.NestedTableValue;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import org.jetbrains.annotations.NotNull;

/**
 * Cell editor for collection/table values. The value is represented by a
 * dedicated table action and is deliberately not backed by the text editor.
 */
public class DatasetTableCellEditorWithTableEditor extends DatasetTableCellEditorWithSpecialEditor<TextFieldWithTableEditor> {
    public DatasetTableCellEditorWithTableEditor(DatasetEditorTable table) {
        super(table);
    }

    @Override
    protected TextFieldWithTableEditor createEditorComponent(DatasetEditorTable table) {
        return new TextFieldWithTableEditor(table.getProject(), table);
    }

    @Override
    public void prepareEditor(@NotNull DatasetEditorModelCell cell) {
        super.prepareEditor(cell);

        Object userValue = cell.getUserValue();
        getEditorComponent().setEnabled(userValue instanceof NestedTableValue);
        setEditable(false);
    }

    @Override
    public Object getCellEditorValue() {
        DatasetEditorModelCell cell = getCell();
        return cell == null ? null : cell.getUserValue();
    }
}
