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
import com.dbn.event.notification.filter.EventNotificationFilter;
import com.dbn.event.notification.filter.EventNotificationFilterType;
import com.dbn.event.service.EventHistoryService;
import com.intellij.openapi.application.ApplicationManager;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
@Getter
@Setter
public class DataChangeEventBundle  extends DBNMutableTableModel<DataChangeEvent> {
  private final EventNotificationFilter filter = new EventNotificationFilter();
  private final List<DataChangeEvent> events = FilteredList.stateful(filter);
  private final String connectionId  ;
  // Define the column names for the dashboard
  private final String COLUMN_OPERATION = "Operation";
  private final String COLUMN_TABLE = "Table";
  private final String COLUMN_ROWID = "Row ID";
  private final String COLUMN_TIMESTAMP = "Timestamp";
    // List to hold the NotificationEvent objects
    private final String[] columnNames = {
            COLUMN_OPERATION, COLUMN_TABLE, COLUMN_ROWID, COLUMN_TIMESTAMP
    };
  private String regStatusFilter = "All";
  private String tableNameFilter = "All";

  public DataChangeEventBundle(String connectionId) {
    this.connectionId = connectionId;
    //intialise the model
    // subscribe for new events
    EventHistoryService.getInstance().registerListener(connectionId,event -> {

      List<DataChangeEvent> initialEventss = EventHistoryService.getInstance().getAllEventsForConnection(connectionId,tableNameFilter , regStatusFilter);
      events.clear();
      events.addAll(initialEventss);
      ApplicationManager.getApplication().invokeLater(this::notifyRowChanges);
    });
    loadEvents();
  }

  public void loadEvents() {
    List<DataChangeEvent> initialEvents = EventHistoryService.getInstance().getAllEventsForConnection(connectionId,tableNameFilter , regStatusFilter);
    events.clear();
    events.addAll(initialEvents);
    ApplicationManager.getApplication().invokeLater(this::notifyRowChanges);


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

  @Override
  public void disposeInner() {
    // Clean up resources if needed.
  }


  public List<String> getDistinctValues(EventNotificationFilterType filterType) {
    return Collections.emptyList(); // TODO
  }
}
