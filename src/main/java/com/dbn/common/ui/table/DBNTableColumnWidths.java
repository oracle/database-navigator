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

import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.FontMetrics;

import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;

import static com.intellij.util.ui.JBUI.scale;

/**
 * Handler for table column widths, allowing to specify fixed and proportional column widths, but also supporting content driven width adjustments
 * (logic extracted from DBNTable)
 *
 * @author Dan Cioca (Oracle)
 */
public class DBNTableColumnWidths {
    private final WeakRef<DBNTable> table;
    private final FontMetrics metricsCache;

    private final Map<Integer, Integer> fixedWidths = new HashMap<>();
    private final Map<Integer, Integer> proportionalWidths = new HashMap<>();

    public DBNTableColumnWidths(DBNTable table) {
        this.table = WeakRef.of(table);
        this.metricsCache = new FontMetrics(table);

        table.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustColumnWidths();
            }
        });
    }

    public DBNTable getTable() {
        return WeakRef.ensure(table);
    }

    public void setProportionalColumnWidth(int columnIndex, int percentage) {
        proportionalWidths.put(columnIndex, percentage);
    }

    public void setProportionalColumnWidths(int... percentages) {
        for (int i = 0; i < percentages.length; i++) {
            setProportionalColumnWidth(i, percentages[i]);
        }
    }

    public void setFixedColumnWidth(int columnIndex, int width) {
        if (width > 0) fixedWidths.put(columnIndex, width);
    }

    public void setFixedColumnWidths(int... widths) {
        fixedWidths.clear();
        for (int i = 0; i < widths.length; i++) {
            setFixedColumnWidth(i, widths[i]);
        }
    }

    public void adjustColumnWidths() {
        DBNTable table = getTable();
        int buffer = table.getColumnWidthBuffer();
        for (int c = 0; c < table.getColumnCount(); c++){
            adjustColumnWidth(c, buffer);
        }
    }

    public void adjustColumnWidth(int columnIndex, int span) {
        DBNTable table = getTable();
        TableColumnModel columnModel = table.getColumnModel();
        if (columnIndex >= columnModel.getColumnCount()) return;

        DBNTableModel model = table.getModel();
        TableColumn column = columnModel.getColumn(columnIndex);

        Integer fixedWidth = fixedWidths.get(columnIndex);
        if (fixedWidth != null) {
            int width = scale(fixedWidth);
            column.setPreferredWidth(width);
            column.setMinWidth(width);
            column.setMaxWidth(width);
            return;
        }

        Integer percentage = proportionalWidths.get(columnIndex);
        if (percentage != null) {
            int tableWidth = table.getWidth();
            int width = (tableWidth * percentage) / 100;
            column.setPreferredWidth(width);
            column.setWidth(width);
            return;
        }

        int minWidth = table.getMinColumnWidth();
        int maxWidth = table.getMaxColumnWidth();
        int preferredWidth = 0;

        // header
        JTableHeader tableHeader = table.getTableHeader();
        if (tableHeader != null) {
            Object headerValue = column.getHeaderValue();
            TableCellRenderer renderer = column.getHeaderRenderer();
            if (renderer == null) renderer = tableHeader.getDefaultRenderer();
            Component headerComponent = renderer.getTableCellRendererComponent(table, headerValue, false, false, 0, columnIndex);
            int width = (int) headerComponent.getPreferredSize().getWidth();
            if (width > preferredWidth)
                preferredWidth = width;
        }

        // rows
        String columnName = model.getColumnName(columnIndex);
        int rowCount = model.getRowCount();
        for (int r = 0; r < rowCount; r++) {
            if (preferredWidth >= maxWidth) break;

            int c = column.getModelIndex();
            Object value = model.getValueAt(r, c);
            if (value == null) continue;

            String displayValue = model.getPresentableValue(value, c);
            if (displayValue == null || displayValue.length() >= 100) continue;

            int cellWidth = metricsCache.getTextWidth(columnName, displayValue);
            preferredWidth = Math.max(preferredWidth, cellWidth);
        }

        preferredWidth = Math.min(preferredWidth, maxWidth);
        preferredWidth = Math.max(preferredWidth, minWidth);
        preferredWidth = preferredWidth + span;

        if (column.getPreferredWidth() != preferredWidth)  {
            column.setPreferredWidth(preferredWidth);
        }
    }
}
