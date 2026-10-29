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

import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Keyboard;
import com.dbn.data.editor.ui.TextFieldWithEditor;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import com.intellij.openapi.actionSystem.IdeActions;
import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;

/** Shared cell-editor behavior for values opened in a dedicated editor. */
abstract class DatasetTableCellEditorWithSpecialEditor<T extends TextFieldWithEditor> extends DatasetTableCellEditor<T> {
    DatasetTableCellEditorWithSpecialEditor(DatasetEditorTable table) {
        super(table);
        getEditorComponent().getTextField().setBorder(Borders.EMPTY_BORDER);
    }

    @Override
    public void prepareEditor(@NotNull DatasetEditorModelCell cell) {
        setCell(cell);
        getEditorComponent().setUserValueHolder(cell);
        highlight(cell.hasError() ? HIGHLIGHT_TYPE_ERROR : HIGHLIGHT_TYPE_NONE);
    }

    @Override
    public void setEditable(boolean editable) {
        getEditorComponent().setEditable(editable);
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        if (!keyEvent.isConsumed() && Keyboard.match(
                Keyboard.getShortcuts(IdeActions.ACTION_SHOW_INTENTION_ACTIONS), keyEvent)) {
            keyEvent.consume();
            getEditorComponent().openEditor();
        } else {
            super.keyPressed(keyEvent);
        }
    }
}
