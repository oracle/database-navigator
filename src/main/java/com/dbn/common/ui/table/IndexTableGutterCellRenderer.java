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

import com.dbn.common.color.Colors;

import javax.swing.JList;

public class IndexTableGutterCellRenderer extends DBNTableGutterRendererBase {


    @Override
    protected void adjustListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        DBNTableGutter tableGutter = (DBNTableGutter) list;
        DBNTable table = tableGutter.getTable();
        boolean isCaretRow = table.getCellSelectionEnabled() && table.getSelectedRow() == index && table.getSelectedRowCount() == 1;

        mainPanel.setBackground(isSelected ?
                Colors.getTableSelectionBackground(true) :
                isCaretRow ?
                        Colors.getTableCaretRowColor() :
                        table.getBackground());
        textLabel.setForeground(isSelected ?
                Colors.getTableSelectionForeground(cellHasFocus) :
                Colors.getTableGutterForeground());
    }
}
