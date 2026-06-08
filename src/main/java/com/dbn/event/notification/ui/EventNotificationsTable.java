/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.event.notification.ui;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.table.DBNTableGutter;
import com.dbn.common.ui.table.DBNTableTransferHandler;
import com.dbn.common.ui.table.DBNTableWithGutter;
import com.dbn.event.notification.model.DataChangeNotification;
import com.dbn.event.notification.model.DataChangeNotificationBundle;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.nls.NlsResources.txt;

public class EventNotificationsTable extends DBNTableWithGutter<DataChangeNotificationBundle> {

    public EventNotificationsTable(@NotNull DBNComponent parent, DataChangeNotificationBundle registrations) {
        super(parent, registrations, true);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setDefaultRenderer(DataChangeNotification.class, new CellRenderer());
        setTransferHandler(DBNTableTransferHandler.INSTANCE);
        initTableSorter();


        setAccessibleName(this, txt("app.eventNotification.aria.DataChangeEventNotifications"));
    }

    @Override
    protected DBNTableGutter<?> createTableGutter() {
        return new DBNTableGutter<DBNTableWithGutter>(this);
    }

    @Override
    public void setModel(@NotNull TableModel dataModel) {
        super.setModel(dataModel);
        initTableSorter();
    }

    private class CellRenderer extends DBNColoredTableCellRenderer {
        @Override
        protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
            DataChangeNotification entry = (DataChangeNotification) value;
            String columnValue = getModel().getPresentableValue(entry, column);
            append(columnValue == null ? "" : columnValue, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
    }
}
