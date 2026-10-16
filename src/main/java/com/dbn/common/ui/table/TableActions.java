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

package com.dbn.common.ui.table;

import com.dbn.common.color.Colors;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Mouse;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Point;
import java.util.function.Predicate;

import static com.dbn.common.color.Colors.getTableSelectionBackground;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public final class TableActions {
    private TableActions() {}

    public static <R> Predicate<Point> installTableAction(
            @NotNull DBNTable<?> table,
            int modelColumnIndex,
            @NotNull Class<R> rowType,
            @NotNull DBNTableRowAction<R> action) {
        JLabel renderer = new JLabel();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        renderer.setOpaque(true);
        setAccessibleName(renderer, action.getName());

        installActionRenderer(table, modelColumnIndex, rowType, action, renderer);
        table.addPropertyChangeListener("model", event ->
                installActionRenderer(table, modelColumnIndex, rowType, action, renderer));
        table.addMouseListener(Mouse.listener().onClick(event -> {
            if (!Mouse.isMainSingleClick(event)) return;
            int viewRowIndex = table.rowAtPoint(event.getPoint());
            if (viewRowIndex < 0 || !isActionColumnAt(table, modelColumnIndex, event.getPoint())) return;

            R row = getActionRow(table, modelColumnIndex, viewRowIndex, rowType);
            if (!action.isAvailable(row)) return;

            table.setRowSelectionInterval(viewRowIndex, viewRowIndex);
            action.perform(row);
        }));
        table.addMouseMotionListener(Mouse.listener().onMove(event -> {
            boolean available = isActionAvailableAt(table, modelColumnIndex, rowType, action, event.getPoint());
            table.setCursor(available ? Cursors.handCursor() : Cursors.defaultCursor());
        }));
        table.addMouseListener(Mouse.listener().onExit(event -> table.setCursor(Cursors.defaultCursor())));

        return point -> isActionColumnAt(table, modelColumnIndex, point);
    }

    private static <R> void installActionRenderer(
            DBNTable<?> table,
            int modelColumnIndex,
            Class<R> rowType,
            DBNTableRowAction<R> action,
            JLabel renderer) {
        if (modelColumnIndex < 0 || modelColumnIndex >= table.getModel().getColumnCount()) return;
        int viewColumnIndex = table.convertColumnIndexToView(modelColumnIndex);
        if (viewColumnIndex < 0) return;

        table.getColumnModel().getColumn(viewColumnIndex).setCellRenderer((source, value, selected, focused, rowIndex, columnIndex) -> {
            R row = rowType.isInstance(value) ? rowType.cast(value) : null;
            boolean available = action.isAvailable(row);
            renderer.setIcon(available ? action.getIcon() : null);
            renderer.setEnabled(available);
            Color background = selected ?
                    getTableSelectionBackground(source.hasFocus()) :
                    source.getBackground();
            renderer.setBackground(background);
            return renderer;
        });
    }

    private static <R> boolean isActionAvailableAt(
            DBNTable<?> table,
            int modelColumnIndex,
            Class<R> rowType,
            DBNTableRowAction<R> action,
            Point point) {
        if (!isActionColumnAt(table, modelColumnIndex, point)) return false;
        int viewRowIndex = table.rowAtPoint(point);
        if (viewRowIndex < 0) return false;
        R row = getActionRow(table, modelColumnIndex, viewRowIndex, rowType);
        return action.isAvailable(row);
    }

    private static <R> R getActionRow(DBNTable<?> table, int modelColumnIndex, int viewRowIndex, Class<R> rowType) {
        int modelRowIndex = table.convertRowIndexToModel(viewRowIndex);
        Object row = table.getModel().getValueAt(modelRowIndex, modelColumnIndex);
        return rowType.isInstance(row) ? rowType.cast(row) : null;
    }

    private static boolean isActionColumnAt(DBNTable<?> table, int modelColumnIndex, Point point) {
        int viewColumnIndex = table.columnAtPoint(point);
        return viewColumnIndex >= 0 && table.convertColumnIndexToModel(viewColumnIndex) == modelColumnIndex;
    }

}
