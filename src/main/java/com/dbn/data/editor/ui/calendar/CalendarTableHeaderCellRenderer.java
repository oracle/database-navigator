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

package com.dbn.data.editor.ui.calendar;

import com.dbn.common.ui.util.Fonts;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

class CalendarTableHeaderCellRenderer extends DefaultTableCellRenderer {
    static final Border EMPTY_BORDER = JBUI.Borders.empty(1, 1, 1, 9);
    static final Color FOREGROUND_COLOR = new JBColor(new Color(67, 123, 203), new Color(67, 123, 203));

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setHorizontalAlignment(RIGHT);
        setFont(Fonts.BOLD);
        setBorder(EMPTY_BORDER);
        //setForeground(column == 0 ? Color.RED : GUIUtil.getTableForeground());
        setForeground(FOREGROUND_COLOR);
        return component;
    }
}
