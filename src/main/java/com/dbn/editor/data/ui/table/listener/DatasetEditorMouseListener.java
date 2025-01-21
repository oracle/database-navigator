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
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.util.Mouse;
import com.dbn.editor.data.DatasetEditorManager;
import com.dbn.editor.data.filter.DatasetFilterInput;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dbn.object.DBColumn;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.dbn.nls.NlsResources.txt;

public class DatasetEditorMouseListener extends MouseAdapter {
    private final WeakRef<DatasetEditorTable> table;

    public DatasetEditorMouseListener(DatasetEditorTable table) {
        this.table = WeakRef.of(table);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }


    @NotNull
    public DatasetEditorTable getTable() {
        return table.ensure();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            Point mousePoint = e.getPoint();
            DatasetEditorTable table = getTable();
            DatasetEditorModelCell cell = (DatasetEditorModelCell) table.getCellAtLocation(mousePoint);
            if (cell != null) {

                if (table.getSelectedRowCount() <= 1 && table.getSelectedColumnCount() <= 1) {
                    table.cancelEditing();
                    boolean oldEditingStatus = table.isEditingEnabled();
                    table.setEditingEnabled(false);
                    table.selectCell(table.rowAtPoint(mousePoint), table.columnAtPoint(mousePoint));
                    table.setEditingEnabled(oldEditingStatus);
                }

                table.showPopupMenu(e, cell, cell.getColumnInfo());
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (Mouse.isNavigationEvent(e)) {
            DatasetEditorTable table = getTable();
            DatasetEditorModelCell cell = (DatasetEditorModelCell) table.getCellAtLocation(e.getPoint());
            if (cell != null) {
                DBColumn column = cell.getColumn();

                if (column.isForeignKey() && cell.getUserValue() != null) {
                    table.clearSelection();

                    Project project = table.getProject();
                    Progress.prompt(project, column, true,
                            txt("prc.dataEditor.title.OpeningRecord"),
                            txt("prc.dataEditor.text.OpeningRecordFor", column.getQualifiedNameWithType()),
                            progress -> {
                                DatasetFilterInput filterInput = table.getModel().resolveForeignKeyRecord(cell);
                                if (filterInput != null && !filterInput.isEmpty()) {
                                    Dispatch.run(() -> {
                                        DatasetEditorManager datasetEditorManager = DatasetEditorManager.getInstance(column.getProject());
                                        datasetEditorManager.navigateToRecord(filterInput, e);
                                    });
                                }
                            });
                }
            }
            e.consume();
        }
    }
}