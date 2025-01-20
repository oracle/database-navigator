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

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.data.grid.ui.table.basic.BasicTable;
import com.dbn.data.grid.ui.table.basic.BasicTableSpeedSearch;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.model.sortable.SortableDataModel;
import com.dbn.data.model.sortable.SortableTableHeaderMouseListener;
import com.dbn.data.sorting.SortDirection;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;

public abstract class SortableTable<T extends SortableDataModel<?, ?>> extends BasicTable<T> {

    public SortableTable(DBNComponent parent, T dataModel, boolean enableSpeedSearch) {
        super(parent, dataModel);
        JTableHeader tableHeader = getTableHeader();
        tableHeader.setDefaultRenderer(new SortableTableHeaderRenderer());
        tableHeader.addMouseListener(new SortableTableHeaderMouseListener(this));
        tableHeader.setCursor(Cursors.handCursor());

        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setCellSelectionEnabled(true);
        adjustColumnWidths();
        if (enableSpeedSearch) {
            new BasicTableSpeedSearch(this);
        }
    }

    public void sort() {
        getModel().sort();
        JTableHeader tableHeader = getTableHeader();
        UserInterface.repaint(tableHeader);
    }

    public boolean sort(int columnIndex, SortDirection sortDirection, boolean keepExisting) {
        SortableDataModel<?, ?> model = getModel();
        int modelColumnIndex = convertColumnIndexToModel(columnIndex);
        ColumnInfo columnInfo = model.getColumnInfo(modelColumnIndex);
        if (columnInfo == null) return false;
        if (!columnInfo.isSortable()) return false;
        boolean sorted = model.sort(modelColumnIndex, sortDirection, keepExisting);
        if (sorted) {
            JTableHeader tableHeader = getTableHeader();
            UserInterface.repaint(tableHeader);
        }
        return sorted;
    }

}
