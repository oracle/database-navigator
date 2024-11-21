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
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.table.DBNTableGutterRendererBase;

import javax.swing.JList;

import static com.dbn.common.dispose.Checks.isValid;

public class BasicTableGutterCellRenderer extends DBNTableGutterRendererBase {

    @Override
    protected void adjustListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        BasicTableGutter tableGutter = (BasicTableGutter) list;

        DBNTable table = tableGutter.getTable();
        boolean isCaretRow = isValid(table) &&
                table.getCellSelectionEnabled() &&
                table.getSelectedRow() == index &&
                table.getSelectedRowCount() == 1;

        mainPanel.setBackground(isSelected ?
                Colors.getTableSelectionBackground(cellHasFocus) :
                isCaretRow ?
                        Colors.getTableCaretRowColor() :
                        table.getBackground());
        textLabel.setForeground(isSelected ?
                Colors.getTableSelectionForeground(cellHasFocus) :
                Colors.getTableGutterForeground());
    }
}
