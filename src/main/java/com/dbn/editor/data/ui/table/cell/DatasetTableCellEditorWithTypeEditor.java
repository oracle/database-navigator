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

import com.dbn.connection.ConnectionHandler;
import com.dbn.data.editor.ui.TextFieldWithTypeEditor;
import com.dbn.data.type.GenericDataType;
import com.dbn.data.type.ui.DeclaredTypeValueEditorDialog;
import com.dbn.data.value.ComplexValue;
import com.dbn.data.value.StructureValue;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dbn.object.DBType;
import org.jetbrains.annotations.NotNull;

import static com.dbn.editor.data.model.RecordStatus.DELETED;

/**
 * Cell editor for SQL object values. The table cell is a launch point for the
 * attribute form; the individual values are edited in the modal form.
 */
class DatasetTableCellEditorWithTypeEditor extends DatasetTableCellEditorWithSpecialEditor<TextFieldWithTypeEditor> {
    private Object value;

    DatasetTableCellEditorWithTypeEditor(DatasetEditorTable table) {
        super(table);
        getEditorComponent().setOpenAction(this::openEditor);
    }

    @Override
    protected TextFieldWithTypeEditor createEditorComponent(DatasetEditorTable table) {
        return new TextFieldWithTypeEditor(table.getProject(), table);
    }

    @Override
    public void prepareEditor(@NotNull DatasetEditorModelCell cell) {
        super.prepareEditor(cell);
        value = cell.getUserValue();

        Object userValue = cell.getUserValue();
        String text = cell.getColumnInfo().getDataType().getGenericDataType() == GenericDataType.STRUCTURE ?
                StructureValue.DISPLAY_VALUE :
                userValue instanceof StructureValue structureValue ?
                structureValue.getDisplayValue() :
                userValue instanceof ComplexValue complexValue ?
                complexValue.getDisplayValue() :
                userValue == null ? null : userValue.toString();
        TextFieldWithTypeEditor editorComponent = getEditorComponent();
        editorComponent.setText(text);
        editorComponent.setEnabled(isEditable());
        editorComponent.setEditable(false);
    }

    @Override
    public Object getCellEditorValue() {
        return value;
    }

    @Override
    public boolean isEditable() {
        DatasetEditorModelCell cell = getCell();
        return cell != null &&
                cell.getRow().isNot(DELETED) &&
                cell.getRow().getModel().isEditable();
    }

    @Override
    public void setEditable(boolean editable) {
        TextFieldWithTypeEditor editorComponent = getEditorComponent();
        editorComponent.setEditable(false);
        editorComponent.setEnabled(editable);
    }

    private void openEditor() {
        DatasetEditorModelCell cell = getCell();
        if (cell == null) return;
        if (!isEditable()) return;

        ConnectionHandler connection = cell.getConnection();
        DBType declaredType = cell.getColumnInfo().getDataType().getDeclaredType();
        DeclaredTypeValueEditorDialog.showEditor(
                getProject(),
                getTable(),
                connection,
                declaredType,
                cell.getUserValue(),
                cell.getColumn().getQualifiedNameWithType(),
                () -> !getTable().isDisposed() && getCell() == cell && cell.isEditing(),
                attributes -> {
                    value = attributes;
                    getEditorComponent().setText(StructureValue.DISPLAY_VALUE);
                    stopCellEditing();
                });
    }
}
