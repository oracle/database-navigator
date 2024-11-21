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

package com.dbn.data.grid.ui.table.sortable;

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.table.DBNTableHeaderRendererBase;
import com.dbn.common.ui.util.Borders;
import com.dbn.data.model.sortable.SortableDataModel;
import com.dbn.data.sorting.SortDirection;
import com.dbn.data.sorting.SortingInstruction;
import com.dbn.data.sorting.SortingState;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;

public class SortableTableHeaderRenderer extends DBNTableHeaderRendererBase {
    private JPanel mainPanel;
    private JLabel nameLabel;
    private JLabel sortingLabel;

    public SortableTableHeaderRenderer() {
        mainPanel.setOpaque(true);
        mainPanel.setBackground(Colors.getPanelBackground());
        mainPanel.setBorder(Borders.tableBorder(0, 0, 0, 1));
        nameLabel.setForeground(Colors.getLabelForeground());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        SortableDataModel model = (SortableDataModel) table.getModel();
        sortingLabel.setText(null);
        int width = 0;
        String columnName = value.toString();
        SortingState sortingState = model.getSortingState();
        SortingInstruction sortingInstruction = sortingState.getSortingInstruction(columnName);

        if (sortingInstruction != null) {
            Icon icon = sortingInstruction.getDirection() == SortDirection.ASCENDING ?
                    Icons.DATA_EDITOR_SORT_ASC :
                    Icons.DATA_EDITOR_SORT_DESC;
            sortingLabel.setIcon(icon);
            width += icon.getIconWidth();
            if (sortingState.size() > 1) {
                sortingLabel.setText(Integer.toString(sortingInstruction.getIndex()));
            }
        } else {
            sortingLabel.setIcon(null);
        }

        nameLabel.setText(columnName);


        FontMetrics fontMetrics = getFontMetrics();
        width += fontMetrics.stringWidth(columnName) + 24;
        int height = fontMetrics.getHeight() + 6;
        mainPanel.setPreferredSize(new Dimension(width, height));
        return mainPanel;
    }

    @Override
    protected JLabel getNameLabel() {
        return nameLabel;
    }
}
