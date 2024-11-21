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

package com.dbn.editor.data.ui.table.listener;

import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.util.Keyboard.Key;
import com.dbn.common.util.Messages;
import com.dbn.data.type.DBDataType;
import com.dbn.editor.data.model.DatasetEditorModel;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.ui.table.DatasetEditorTable;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.editor.data.model.RecordStatus.INSERTING;
import static com.dbn.editor.data.model.RecordStatus.UPDATING;

public class DatasetEditorKeyListener extends KeyAdapter {
    private final WeakRef<DatasetEditorTable> table;

    public DatasetEditorKeyListener(DatasetEditorTable table) {
        this.table = WeakRef.of(table);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.isConsumed()) return;

        DatasetEditorTable table = getTable();
        if (isNotValid(table)) return;
        DatasetEditorModel model = table.getModel();

        int keyChar = e.getKeyChar();
        if (model.is(INSERTING)) {
            switch (keyChar) {
                case Key.ESCAPE:
                    model.cancelInsert(true);
                    break;
                case Key.ENTER:
                    int index = model.getInsertRowIndex();
                    try {
                        model.postInsertRecord(false, true, false);
                        if (model.isNot(INSERTING)) {
                            model.insertRecord(index + 1);
                        }
                    } catch (SQLException e1) {
                        conditionallyLog(e1);
                        Messages.showErrorDialog(table.getProject(), "Could not create row in " + table.getDataset().getQualifiedNameWithType() + ".", e1);
                    }
                    e.consume();
            }
        } else if (!table.isEditing()){
            if (keyChar == Key.DELETE) {
                int[] selectedRows = table.getSelectedRows();
                int[] selectedColumns = table.getSelectedColumns();
                table.performUpdate(-1, -1, () -> {
                    List<DatasetEditorModelCell> cells = new ArrayList<>();
                    for (int rowIndex : selectedRows) {
                        for (int columnIndex : selectedColumns) {
                            DatasetEditorModelCell cell = model.getCellAt(rowIndex, columnIndex);
                            if (cell != null) {
                                DBDataType dataType = cell.getColumnInfo().getDataType();
                                if (dataType.isNative() && !dataType.getNativeType().isLargeObject()) {
                                    cell.setTemporaryUserValue("");
                                    cell.set(UPDATING, true);
                                    cells.add(cell);
                                }
                            }
                        }
                    }
                    for (DatasetEditorModelCell cell : cells) {
                        cell.updateUserValue(null, true);
                    }

                });
            }
        }

    }

    public DatasetEditorTable getTable() {
        return table.get();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}
