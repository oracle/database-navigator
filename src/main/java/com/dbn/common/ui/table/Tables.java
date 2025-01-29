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

import com.dbn.common.ui.Presentable;
import org.jetbrains.annotations.Nls;

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

    /**
     * Attaches a value selector to a specific column of a JTable.
     * The value selector allows users to select values of type {@code T} from a predefined list.
     *
     * @param <T>         the type of values that the selector can handle, must extend {@link Presentable}
     * @param table       the JTable to which the value selector will be attached
     * @param columnIndex the index of the column to which the value selector will be applied
     * @param title       the title of the value selector popup
     * @param values      an array of possible values that the user can select
     */
    public static <T extends Presentable> void attachValueSelector(JTable table, int columnIndex, @Nls String title, T[] values) {
        new TableValueSelector<>(table, columnIndex, title, values);
    }

    public static boolean isFirstCellSelected(JTable table) {
        return table.getSelectedRow() == 0 && table.getSelectedColumn() == 0;
    }

    public static boolean isLastCellSelected(JTable table) {
        return table.getSelectedRow() == table.getRowCount() - 1 &&
                table.getSelectedColumn() == table.getColumnCount() - 1;
    }
}
