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

package com.dbn.data.grid.ui.table.basic;

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.table.DBNTableHeaderRendererBase;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Cursors;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.util.List;

public class BasicTableHeaderRenderer extends DBNTableHeaderRendererBase {
    private JPanel mainPanel;
    private JLabel nameLabel;
    private JLabel sortingLabel;

    public BasicTableHeaderRenderer() {
        mainPanel.setOpaque(true);
        mainPanel.setBackground(Colors.getPanelBackground());
        mainPanel.setBorder(Borders.tableBorder(0, 0, 0, 1));
        nameLabel.setForeground(Colors.getLabelForeground());
        sortingLabel.setText("");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        int width = 0;
        String columnName = value.toString();

        nameLabel.setText(columnName);

        FontMetrics fontMetrics = getFontMetrics();
        width += fontMetrics.stringWidth(columnName) + 24;
        int height = fontMetrics.getHeight() + 6;
        mainPanel.setPreferredSize(new Dimension(width, height));

        Icon icon = null;
        RowSorter rowSorter = table.getRowSorter();
        if (rowSorter != null) {
            Cursor handCursor = Cursors.handCursor();
            mainPanel.setCursor(handCursor);
            nameLabel.setCursor(handCursor);
            sortingLabel.setCursor(handCursor);
            List<? extends RowSorter.SortKey> sortKeys = rowSorter.getSortKeys();
            if (sortKeys.size() == 1) {
                RowSorter.SortKey sortKey = sortKeys.get(0);
                if (sortKey.getColumn() == columnIndex) {
                    SortOrder sortOrder = sortKey.getSortOrder();
                    icon =
                            sortOrder == SortOrder.ASCENDING ? Icons.DATA_EDITOR_SORT_ASC :
                            sortOrder == SortOrder.DESCENDING ? Icons.DATA_EDITOR_SORT_DESC :
                            null;
                }
            }
        }
        sortingLabel.setIcon(icon);
        return mainPanel;
    }

    @Override
    protected JLabel getNameLabel() {
        return nameLabel;
    }
}
