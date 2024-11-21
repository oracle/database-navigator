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

package com.dbn.common.ui.table;

import javax.swing.JTable;
import java.awt.Rectangle;

public class Tables {
    private Tables() {}

    public static Rectangle getCellRectangle(JTable table, int row, int column) {
        Rectangle rectangle = table.getCellRect(row, column, true);

        rectangle.setLocation(
                (int) (rectangle.getX() + table.getLocationOnScreen().getX()),
                (int) (rectangle.getY() + table.getLocationOnScreen().getY()));
        return rectangle;
    }

    public static void selectCell(JTable table, int rowIndex, int columnIndex) {
        Rectangle cellRect = table.getCellRect(rowIndex, columnIndex, true);
        if (!table.getVisibleRect().contains(cellRect)) {
            table.scrollRectToVisible(cellRect);
        }
        if (table.getSelectedRowCount() != 1 || table.getSelectedRow() != rowIndex) {
            table.setRowSelectionInterval(rowIndex, rowIndex);
        }

        if (table.getSelectedColumnCount() != 1 || table.getSelectedColumn() != columnIndex) {
            table.setColumnSelectionInterval(columnIndex, columnIndex);
        }
    }
}
