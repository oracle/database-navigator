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

package com.dbn.event.notification.model;

import com.dbn.common.data.Data;
import com.dbn.common.list.FilteredList;
import com.dbn.common.locale.Formatter;
import com.dbn.common.ui.table.DBNMutableTableModel;
import com.dbn.common.ui.table.DBNTableGutterModel;
import com.dbn.common.ui.table.DBNTableWithGutterModel;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.event.notification.EventNotificationData;
import com.dbn.event.notification.EventNotificationManager;
import com.dbn.event.notification.filter.EventNotificationFilter;
import com.dbn.event.notification.filter.EventNotificationFilterType;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.ListModel;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.common.util.Lists.isInBounds;

@Getter
@Setter
public class DataChangeNotificationBundle extends DBNMutableTableModel<DataChangeNotification> implements DBNTableWithGutterModel<DataChangeNotification> {
    private final EventNotificationFilter filter = new EventNotificationFilter();
    private List<DataChangeNotification> notifications = FilteredList.stateful(filter);

    private final ListModel gutterModel = new DBNTableGutterModel<>(this);
    private final ConnectionRef connection;

    private final String COLUMN_TABLE = "Table Name";
    private final String COLUMN_OPERATION = "Operation";
    private final String COLUMN_TIMESTAMP = "Timestamp";
    private final String COLUMN_ROWID = "Row Id";
    private final String COLUMN_REG_ID = "Source Registration Id";
    private final String[] columnNames = {
            COLUMN_TABLE,
            COLUMN_OPERATION,
            COLUMN_TIMESTAMP,
            COLUMN_ROWID,
            COLUMN_REG_ID};

    public DataChangeNotificationBundle(ConnectionHandler connection) {
        this.connection = ConnectionRef.of(connection);
    }

    public ConnectionId getConnectionId() {
        return connection.getConnectionId();
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return ConnectionRef.ensure(connection);
    }

    @NotNull
    private Project getProject() {
        return getConnection().getProject();
    }

    public void load() {
        ConnectionId connectionId = getConnectionId();
        EventNotificationData notificationData = getNotificationData();
        List<DataChangeNotification> events = notificationData.getNotifications(connectionId);
        this.notifications = FilteredList.stateful(filter, events);

        notifyRowChanges();
    }

    private EventNotificationData getNotificationData() {
        Project project = getProject();
        EventNotificationManager notificationManager = EventNotificationManager.getInstance(project);
        return notificationManager.getNotificationData();
    }

    @Override
    public int getRowCount() {
        return notifications.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public DataChangeNotification getValueAt(int rowIndex, int columnIndex) {
        if (isInBounds(notifications, rowIndex)) {
            return notifications.get(rowIndex);
        }
        return null;
    }

    @Override
    public Object getValue(DataChangeNotification row, int column) {
        if (row == null) return null;
        return switch (column) {
            case 0 -> row.getTableIdentifier();
            case 1 -> row.getOperation();
            case 2 -> row.getTimestamp();
            case 3 -> row.getRowId();
            case 4 -> row.getRegId();
            default -> "";
        };
    }

    @Override
    public String getPresentableValue(DataChangeNotification row, int column) {
        if (column == 2) {
            Formatter formatter = Formatter.getInstance(getProject());
            Date date = new Date(row.getTimestamp());
            return formatter.formatDateTime(date);
        }
        return Data.asString(getValue(row, column));
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return DataChangeNotification.class;
    }

    private List<String> getTableIdentifiers() {
        return FilteredList
                .unwrap(notifications)
                .stream()
                .map(l -> l.getTableIdentifier())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<String> getDistinctValues(EventNotificationFilterType filterType) {
        return switch (filterType) {
            case TABLE -> getTableIdentifiers();
            case OPERATION -> List.of("INSERT", "UPDATE", "DELETE");
        };
    }

    @Override
    public void disposeInner() {
        // Clean up resources if needed.
    }

}
