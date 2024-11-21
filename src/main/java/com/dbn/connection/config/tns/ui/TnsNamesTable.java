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

package com.dbn.connection.config.tns.ui;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.table.DBNTableTransferHandler;
import com.dbn.common.ui.util.Borders;
import com.dbn.connection.config.tns.TnsNames;
import com.dbn.connection.config.tns.TnsProfile;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

public class TnsNamesTable extends DBNTable<TnsNamesTableModel> {

    public TnsNamesTable(@NotNull DBNComponent parent, TnsNames tnsNames) {
        super(parent, new TnsNamesTableModel(tnsNames), true);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setDefaultRenderer(TnsProfile.class, new CellRenderer());
        setTransferHandler(DBNTableTransferHandler.INSTANCE);
        initTableSorter();

    }

    @Override
    public void setModel(@NotNull TableModel dataModel) {
        super.setModel(dataModel);
        initTableSorter();
    }

    private class CellRenderer extends DBNColoredTableCellRenderer {
        @Override
        protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
            TnsProfile entry = (TnsProfile) value;
            Object columnValue = getModel().getPresentableValue(entry, column);
            append(columnValue == null ? "" : columnValue.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            setBorder(Borders.TEXT_FIELD_INSETS);
        }
    }
}
