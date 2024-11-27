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

package com.dbn.editor.session.details;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

public class SessionDetailsTable extends DBNTable<SessionDetailsTableModel> {

    public SessionDetailsTable(@NotNull DBNComponent parent) {
        super(parent, new SessionDetailsTableModel(), false);
        setDefaultRenderer(Object.class, cellRenderer);
        setCellSelectionEnabled(true);
        adjustRowHeight(3);
    }

    private final TableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String text = Commons.nvl(value, "").toString();
            setText(text);
            if (column == 1 && Strings.isNotEmpty(text)) {
                switch (row) {
                    case 1: setIcon(Icons.SB_FILTER_USER); break;
                    case 2: setIcon(Icons.DBO_SCHEMA); break;
                    case 3: setIcon(Icons.SB_FILTER_SERVER); break;
                    default: setIcon(null);
                }
            } else{
                setIcon(null);
            }
            setBorder(Borders.TEXT_FIELD_INSETS);

            return component;
        }
    };
}
