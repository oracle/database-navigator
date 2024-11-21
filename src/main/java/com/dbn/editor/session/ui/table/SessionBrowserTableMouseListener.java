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

package com.dbn.editor.session.ui.table;

import com.dbn.common.ref.WeakRef;
import com.dbn.editor.session.model.SessionBrowserColumnInfo;
import com.dbn.editor.session.model.SessionBrowserModelCell;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SessionBrowserTableMouseListener extends MouseAdapter {
    private final WeakRef<SessionBrowserTable> table;

    public SessionBrowserTableMouseListener(SessionBrowserTable table) {
        this.table = WeakRef.of(table);
    }

    @NotNull
    public SessionBrowserTable getTable() {
        return table.ensure();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            SessionBrowserTable table = getTable();
            Point mousePoint = e.getPoint();
            SessionBrowserModelCell cell = (SessionBrowserModelCell) table.getCellAtLocation(mousePoint);
            if (cell != null) {
                int rowIndex = table.rowAtPoint(mousePoint);
                int columnIndex = table.columnAtPoint(mousePoint);
                int[] selectedRows = table.getSelectedRows();
                int[] selectedColumns = table.getSelectedColumns();

                boolean selectCell = true;
                for (int selectedRow : selectedRows) {
                    if (selectedRow == rowIndex) {
                        for (int selectedColumn : selectedColumns) {
                            if (selectedColumn == columnIndex ) {
                                selectCell = false;
                                break;
                            }
                        }
                        break;
                    }
                }


                if (selectCell) {
                    table.selectCell(rowIndex, columnIndex);
                }
                SessionBrowserColumnInfo columnInfo = (SessionBrowserColumnInfo) table.getModel().getColumnInfo(columnIndex);
                table.showPopupMenu(e, cell, columnInfo);
            }
        }
    }
}