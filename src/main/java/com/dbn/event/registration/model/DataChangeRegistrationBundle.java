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

@Getter
public class DataChangeRegistrationBundle extends DBNMutableTableModel<DataChangeRegistration> implements DBNTableWithGutterModel<DataChangeRegistration> {
  private final ConnectionRef connection;
  private final ListModel gutterModel = new DBNTableGutterModel<>(this);

  private final EventRegistrationFilter filter;
  private List<DataChangeRegistration> listeners;

  // Column identifiers
  public static final String COL_USERNAME         = "USERNAME";
  public static final String COL_REGID            = "REGID";
  public static final String COL_REGFLAGS         = "REGFLAGS";
  public static final String COL_CALLBACK         = "CALLBACK";
  public static final String COL_OPERATIONS       = "OPERATIONS_FILTER";
  public static final String COL_CHANGELAG        = "CHANGELAG";
  public static final String COL_TIMEOUT          = "TIMEOUT";
  public static final String COL_TABLE_NAME       = "TABLE_NAME";
  public static final String COL_OPERATION_TYPES = "OPERATIONS";

  private static final String[] COLUMN_NAMES = {
          COL_USERNAME,
          COL_REGID,
          COL_REGFLAGS,
          COL_CALLBACK,
          COL_OPERATION_TYPES,
          COL_CHANGELAG,
          COL_TIMEOUT,
          COL_TABLE_NAME
  };

  public DataChangeRegistrationBundle(ConnectionHandler connection) {
    this.connection = ConnectionRef.of(connection);

    ConnectionId connectionId = connection.getConnectionId();
    this.filter = new EventRegistrationFilter(connectionId);
    this.listeners = FilteredList.stateful(filter);
  }

  public ConnectionHandler getConnection() {
    return ConnectionRef.ensure(connection);
  }

  @Override
  public int getRowCount() {
    return listeners.size();
  }

  @Override
  public int getColumnCount() {
    return COLUMN_NAMES.length;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    DataChangeRegistration reg = listeners.get(rowIndex);
    switch (columnIndex) {
      case 0: return reg.getUserName();
      case 1: return reg.getRegId();
      case 2: return reg.getRegFlags();
      case 3: return reg.getCallback();
      case 4: return reg.getOperationsFilter();       // numeric
      case 5: return reg.getOperationsDescription();  // human-readable
      case 6: return reg.getChangeLag();
      case 7: return reg.getTimeout();
      case 8: return reg.getTableName();
      default: return "";
    }
  }

  @Override
  public String getColumnName(int column) {
    return COLUMN_NAMES[column];
  }

  @Override
  public @NotNull Class<?> getColumnClass(int columnIndex) {
    // If any column is a number, you could return Integer.class, Long.class, etc.
    // Here we assume everything is best represented as String.
    return String.class;
  }

  @Override
  public void disposeInner() {
    // Clean up if needed (e.g. unregister listeners), but no-op here.
  }

  public void load() throws SQLException {
    ConnectionHandler connection = getConnection();

    List<DataChangeRegistration> registrations = EventRegistrationUtil.fetchRegistrations(connection);
    this.listeners = FilteredList.stateful(filter, registrations);

    notifyRowChanges();
  }

/*  public void applyFilterAndRefresh() {

    // Filter registrations based on the active status and table name
    List<DataChangeRegistration> filtered = allRegs.stream()
            .filter(reg -> {
              // Apply the active/inactive filter based on user selection
              boolean isActive = RegistrationManager.getInstance().isActive(reg.getRegId());
              boolean matchesActiveFilter = false;

              switch (regStatusFilter) {
                case "Active":
                  matchesActiveFilter = isActive;
                  break;
                case "Inactive":
                  matchesActiveFilter = !isActive;
                  break;
                case "All":
                  matchesActiveFilter = true;  // Include all registrations
                  break;
              }

              // Apply table name filter (All or specific table)
              boolean matchesTableNameFilter = "All".equals(tableNameFilter) || reg.getTableName().equalsIgnoreCase(tableNameFilter);

              return matchesActiveFilter && matchesTableNameFilter;
            })
            .collect(Collectors.toList());

    // Update the UI with the filtered registrations
    ApplicationManager.getApplication().invokeLater(() -> {
      viewRegs = filtered;
      notifyRowChanges(); // Refresh the table view
    });
  }*/

  private List<String> getUserNames() {
    return FilteredList
            .unwrap(listeners)
            .stream()
            .map(l -> l.getUserName())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
  }

  private List<String> getTableNames() {
    return FilteredList
            .unwrap(listeners)
            .stream()
            .map(l -> l.getTableName())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
  }

  public List<String> getDistinctValues(EventRegistrationFilterType filterType) {
    switch (filterType) {
      case USER: return getUserNames();
      case TABLE: return getTableNames();
      case STATUS: return List.of("On", "Off");
    }
    return Collections.emptyList();
  }

  @Override
  public ListModel getListModel() {
    return gutterModel;
  }
}