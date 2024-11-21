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

package com.dbn.data.model.sortable;

import com.dbn.common.ref.WeakRef;
import com.dbn.data.grid.ui.table.sortable.SortableTable;
import com.dbn.data.sorting.SortDirection;

import javax.swing.table.JTableHeader;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SortableTableHeaderMouseListener extends MouseAdapter {
    private final WeakRef<SortableTable> table;

    public SortableTableHeaderMouseListener(SortableTable table) {
        this.table = WeakRef.of(table);
    }

    public SortableTable getTable() {
        return table.ensure();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        SortableTable table = getTable();
        if (e.getButton() == MouseEvent.BUTTON1) {
            Point mousePoint = e.getPoint();
            mousePoint.setLocation(mousePoint.getX() - 4, mousePoint.getX());
            JTableHeader tableHeader = table.getTableHeader();
            int columnIndex = tableHeader.columnAtPoint(mousePoint);
            if (columnIndex > -1) {
                Rectangle colRect = tableHeader.getHeaderRect(columnIndex);
                boolean isEdgeClick = colRect.getMaxX() - 8 < mousePoint.getX();
                if (isEdgeClick) {
                    if (e.getClickCount() == 2) {
                        table.accommodateColumnSize(columnIndex, table.getColumnWidthBuffer());
                    }
                } else {
                    boolean keepExisting = e.isControlDown();
                    table.sort(columnIndex, SortDirection.INDEFINITE, keepExisting);
                }
            }
        }
        table.requestFocus();
        //event.consume();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
    }
}
