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
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;

import static com.dbn.common.ui.util.Keyboard.insertKeyListener;
import static com.dbn.common.ui.util.UserInterface.focusNextComponent;
import static com.dbn.common.ui.util.UserInterface.focusPreviousComponent;

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

    public static void installFocusTraversal(JTable table) {
        insertKeyListener(table, new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isConsumed()) return;
                if (e.getKeyCode() != KeyEvent.VK_TAB) return;
                JTable table = (JTable) e.getSource();

                if (e.isShiftDown()) {
                    if (allowTabFocusTraversal(table) || isFirstCellSelected(table)) {
                        focusPreviousComponent(table);
                        e.consume();
                    }
                } else {
                    if (allowTabFocusTraversal(table) || isLastCellSelected(table)) {
                        focusNextComponent(table);
                        e.consume();
                    }
                }
            }
        });
    }

    private static boolean allowTabFocusTraversal(JTable table) {
        if (table.getRowCount() == 0) return true; // no data -> o cell navigation
        if (!table.getColumnSelectionAllowed()) return true; // no cell tab navigation possible
        if (table.getSelectedRowCount() > 1) return true; // multiple rows selected -> no cell tab navigation
        if (table.getSelectedColumnCount() > 1) return true; // multiple columns selected -> no cell tab navigation
        return false;
    }

    /**
     * Selects a row in the given JTable. If the specified row index is out of bounds,
     * the method adjusts it to the nearest valid index within the table's row range.
     *
     * @param table    the JTable in which the row selection will be applied
     * @param rowIndex the index of the row to be selected; adjusted if out of bounds
     */
    public static void selectTableRow(JTable table, int rowIndex) {
        int rowCount = table.getRowCount();
        if (rowCount == 0) return;

        int lastIndex = rowCount - 1;
        if (rowIndex > lastIndex) {
            rowIndex = lastIndex;
        }
        table.setRowSelectionInterval(rowIndex, rowIndex);
    }

    /**
     * Adjusts the row height of a given JTable by calculating the necessary height
     * based on the table's font and specified padding.
     *
     * @param table   the JTable whose row height will be adjusted
     * @param padding the additional space (in pixels) to add above and below the font height
     */
    public static void adjustTableRowHeight(JTable table, int padding) {
        Font font = table.getFont();
        FontRenderContext fontRenderContext = table.getFontMetrics(font).getFontRenderContext();
        LineMetrics lineMetrics = font.getLineMetrics("ABCÄÜÖÂÇĞIİÖŞĀČḎĒËĠḤŌŠṢṬŪŽy", fontRenderContext);
        int fontHeight = Math.round(lineMetrics.getHeight());
        table.setRowHeight(fontHeight + (padding * 2));
    }
}
