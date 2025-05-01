package com.dbn.events.model;

import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.table.DBNMutableTableModel;
import com.dbn.events.service.HistoryService;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
@Getter
public class DataChangeEventBundle    extends DBNMutableTableModel<DataChangeEvent> {
  private final List<DataChangeEvent> events = new CopyOnWriteArrayList<>();
    // Define the column names for the dashboard
    private final String COLUMN_OPERATION = "Operation";
    private final String COLUMN_TABLE = "Table";
    private final String COLUMN_ROWID = "Row ID";
    private final String COLUMN_TIMESTAMP = "Timestamp";
    // List to hold the NotificationEvent objects
    private final String[] columnNames = {
            COLUMN_OPERATION, COLUMN_TABLE, COLUMN_ROWID, COLUMN_TIMESTAMP
    };

  public DataChangeEventBundle() {
    //intialise the model
    List<DataChangeEvent> initialEvents = HistoryService.getInstance().getEventsByReg();
    events.addAll(initialEvents);

    // subscribe for new events
    HistoryService.getInstance().registerListener(event -> {
      Dispatch.run(true,()->{
//        int row = events.size();
//        events.add(event);
        notifyRowChanges();
      });
    });
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

  public void addEvent(DataChangeEvent event) {
    events.add(event);
  }




}
