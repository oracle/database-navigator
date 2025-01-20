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
import com.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.table.TableCellRenderer;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class SessionDetailsTable extends DBNTable<SessionDetailsTableModel> {

    public SessionDetailsTable(@NotNull DBNComponent parent) {
        super(parent, new SessionDetailsTableModel(), false);
        setDefaultRenderer(Object.class, createCellRenderer());
        setCellSelectionEnabled(true);

        setAccessibleName(this, "Session Details");
    }

    private TableCellRenderer createCellRenderer() {
        return new DBNColoredTableCellRenderer() {
            @Override
            protected void customizeCellRenderer(DBNTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column) {
                String text = Commons.nvl(value, "").toString();
                append(text);
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
            }
        };
    }
}

