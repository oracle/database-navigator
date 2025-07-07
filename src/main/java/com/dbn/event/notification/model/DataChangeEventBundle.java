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

import com.dbn.common.list.FilteredList;
import com.dbn.common.ui.table.DBNMutableTableModel;
import com.dbn.common.ui.table.DBNTableGutterModel;
import com.dbn.common.ui.table.DBNTableWithGutterModel;
import com.dbn.connection.ConnectionId;
import com.dbn.event.notification.filter.EventNotificationFilter;
import com.dbn.event.notification.filter.EventNotificationFilterType;
import com.dbn.event.service.EventHistoryService;
import lombok.Getter;
import lombok.Setter;

import javax.swing.ListModel;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class DataChangeEventBundle extends DBNMutableTableModel<DataChangeEvent> implements DBNTableWithGutterModel<DataChangeEvent> {
  private final EventNotificationFilter filter = new EventNotificationFilter();
  private List<DataChangeEvent> events = FilteredList.stateful(filter);

  private final ListModel gutterModel = new DBNTableGutterModel<>(this);


  private final ConnectionId connectionId  ;
  // Define the column names for the dashboard
  private final String COLUMN_OPERATION = "Operation";
  private final String COLUMN_TABLE = "Table";
  private final String COLUMN_ROWID = "Row ID";
  private final String COLUMN_TIMESTAMP = "Timestamp";
    // List to hold the NotificationEvent objects
    private final String[] columnNames = {
            COLUMN_OPERATION, COLUMN_TABLE, COLUMN_ROWID, COLUMN_TIMESTAMP
    };

  public DataChangeEventBundle(ConnectionId connectionId) {
    this.connectionId = connectionId;
    //intialise the model
    // subscribe for new events
    // TODO the form should subscribe for new events?
/*
    EventHistoryService.getInstance().registerListener(connectionId,event -> {
      load();
    });
*/
  }

  public void load() {
    EventHistoryService eventHistoryService = EventHistoryService.getInstance();
    List<DataChangeEvent> events = eventHistoryService.getAllEventsForConnection(connectionId);
    this.events = FilteredList.stateful(filter, events);

    notifyRowChanges();
  }

  @Override
  public int getRowCount() {
    return events.size();
  }

  @Override
  public int getColumnCount() {
    return columnNames.length;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    if (events.isEmpty()) return null;
    DataChangeEvent event = events.get(rowIndex);
    switch (columnIndex) {
      case 0:
        return event.getOperation();
      case 1:
        return event.getTableName();
      case 2:
        return event.getRowId();
      case 3:
        return event.getTimestamp();
      default:
        return "";
    }
  }

  @Override
  public String getColumnName(int column) {
    return columnNames[column];
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    // All columns are Strings, adjust if any of your values are another type.
    return String.class;
  }

  private List<String> getTableNames() {
    return FilteredList
            .unwrap(events)
            .stream()
            .map(l -> l.getTableName())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
  }

  public List<String> getDistinctValues(EventNotificationFilterType filterType) {
    switch (filterType) {
      case TABLE: return getTableNames();
      case OPERATION: return List.of("INSERT", "UPDATE", "DELETE");
    }
    return Collections.emptyList();
  }

  public ListModel getListModel() {
    return gutterModel;
  }

  @Override
  public void disposeInner() {
    // Clean up resources if needed.
  }

}
