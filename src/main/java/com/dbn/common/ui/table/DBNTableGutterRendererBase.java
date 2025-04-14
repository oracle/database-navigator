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
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Fonts;
import com.dbn.data.grid.color.BasicTableTextAttributes;
import com.dbn.data.grid.color.DataGridTextAttributes;
import com.dbn.data.grid.ui.table.basic.BasicTableGutter;
import com.intellij.ui.SimpleTextAttributes;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.Component;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public abstract class DBNTableGutterRendererBase implements DBNTableGutterRenderer{
    protected JLabel textLabel;
    protected JLabel iconLabel;
    protected JPanel mainPanel;

    public DBNTableGutterRendererBase() {
        textLabel.setText("");
        iconLabel.setText("");
        textLabel.setFont(Fonts.editor(-2));
        textLabel.setForeground(Colors.getTableGutterForeground());
        mainPanel.setBackground(Colors.getTableGutterBackground());
        iconLabel.setBorder(Borders.insetBorder(4));

        mainPanel.setBorder(Borders.tableBorder(0, 0, 0, 1));
    }

    protected static DataGridTextAttributes getAttributes() {
        return BasicTableTextAttributes.get();
    }

    @Override
    public final Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        BasicTableGutter tableGutter = (BasicTableGutter) list;

        boolean isCaretRow = isCaretRow(index, tableGutter.getTable());
        boolean isFocusOwner = list.isFocusOwner();

        SimpleTextAttributes attributes = getAttributes(isSelected, isFocusOwner, isCaretRow);
        mainPanel.setBackground(attributes.getBgColor());

        textLabel.setForeground(isSelected ?
                Colors.getTableSelectionForeground(cellHasFocus) :
                Colors.getTableGutterForeground());

        textLabel.setText(Integer.toString(index + 1));
        setAccessibleName(mainPanel, "Row index " + (index + 1));

        adjustListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        return mainPanel;
    }

    protected abstract void adjustListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus);

    protected SimpleTextAttributes getAttributes(boolean selected, boolean focussed, boolean caretRow) {
        DataGridTextAttributes attributes = getAttributes();
        if (selected) {
            return focussed ?
                    attributes.getSelection() :
                    attributes.getCaretRow();
        }
        return caretRow ?
                attributes.getCaretRow() :
                attributes.getPlainData(false, false);
    }

    protected static boolean isCaretRow(int index, DBNTableWithGutter table) {
        return isValid(table) &&
                table.getCellSelectionEnabled() &&
                table.getSelectedRow() == index &&
                table.getSelectedRowCount() == 1;
    }

}
