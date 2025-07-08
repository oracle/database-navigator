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

package com.dbn.event.registration.model;

import com.dbn.common.data.Data;
import com.dbn.common.list.FilteredList;
import com.dbn.common.ui.table.DBNMutableTableModel;
import com.dbn.common.ui.table.DBNTableGutterModel;
import com.dbn.common.ui.table.DBNTableWithGutterModel;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.event.registration.EventRegistrationUtil;
import com.dbn.event.registration.filter.EventRegistrationFilter;
import com.dbn.event.registration.filter.EventRegistrationFilterType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.ListModel;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.common.util.Lists.isInBounds;

@Getter
public class DataChangeRegistrationBundle extends DBNMutableTableModel<DataChangeRegistration> implements DBNTableWithGutterModel<DataChangeRegistration> {
    private final ConnectionRef connection;
    private final ListModel gutterModel = new DBNTableGutterModel<>(this);
    private final EventRegistrationFilter filter;
    private List<DataChangeRegistration> registrations;

    // Column identifiers
    public static final String COL_REG_ID = "Registration Id";
    public static final String COL_USERNAME = "User Name";
    public static final String COL_TABLE_NAME = "Table Name";
    public static final String COL_OPERATIONS = "Operations";
    public static final String COL_TIMEOUT = "Timeout";
    public static final String COL_CHANGE_LAG = "Change Lag";
    public static final String COL_CALLBACK = "Callback";
    public static final String COL_REG_FLAGS = "Reg FLags";

    private static final String[] COLUMN_NAMES = {
            COL_REG_ID,
            COL_USERNAME,
            COL_TABLE_NAME,
            COL_OPERATIONS,
            COL_TIMEOUT,
            COL_CHANGE_LAG,
            COL_CALLBACK,
            COL_REG_FLAGS,
    };

    public DataChangeRegistrationBundle(ConnectionHandler connection) {
        this.connection = ConnectionRef.of(connection);

        ConnectionId connectionId = connection.getConnectionId();
        this.filter = new EventRegistrationFilter(connectionId);
        this.registrations = FilteredList.stateful(filter);
    }

    public ConnectionHandler getConnection() {
        return ConnectionRef.ensure(connection);
    }

    @Override
    public int getRowCount() {
        return registrations.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (isInBounds(registrations, rowIndex)) {
            return registrations.get(rowIndex);
        }
        return null;
    }

    @Override
    public Object getValue(DataChangeRegistration row, int column) {
        switch (column) {
            case 0: return row.getRegId();
            case 1: return row.getUserName();
            case 2: return row.getTableName();
            case 3: return row.getOperationsDescription();
            case 4: return row.getTimeout();
            case 5: return row.getChangeLag();
            case 6: return row.getCallback();
            case 7: return row.getRegFlags();
            default: return "";
        }
    }

    @Override
    public String getPresentableValue(DataChangeRegistration row, int column) {
        return Data.asString(getValue(row, column));
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public @NotNull Class<?> getColumnClass(int columnIndex) {
        return DataChangeRegistration.class;
    }

    @Override
    public void disposeInner() {
        // Clean up if needed (e.g. unregister listeners), but no-op here.
    }

    public void load() throws SQLException {
        ConnectionHandler connection = getConnection();

        List<DataChangeRegistration> registrations = EventRegistrationUtil.fetchRegistrations(connection);
        this.registrations = FilteredList.stateful(filter, registrations);

        notifyRowChanges();
    }

    private List<String> getUserNames() {
        return FilteredList
                .unwrap(registrations)
                .stream()
                .map(l -> l.getUserName())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> getTableNames() {
        return FilteredList
                .unwrap(registrations)
                .stream()
                .map(l -> l.getTableName())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<String> getDistinctValues(EventRegistrationFilterType filterType) {
        switch (filterType) {
            case USER:
                return getUserNames();
            case TABLE:
                return getTableNames();
            case STATUS:
                return List.of("On", "Off");
        }
        return Collections.emptyList();
    }

    @Override
    public ListModel getListModel() {
        return gutterModel;
    }
}