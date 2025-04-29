package com.dbn.events.model;

import com.dbn.common.thread.Background;
import com.dbn.common.ui.table.DBNMutableTableModel;
import com.dbn.connection.ConnectionHandler;
import com.dbn.events.service.RegistrationService;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class DataChangeRegistrationBundle extends DBNMutableTableModel<DataChangeRegistration> {
  private  List<DataChangeRegistration> allRegs = new CopyOnWriteArrayList<>();
  private List<DataChangeRegistration> viewRegs = Collections.emptyList();

  private final RegistrationService registrationService = new RegistrationService();
  private final ConnectionHandler connectionHandler;
  private boolean activeOnly = false;


  // Column identifiers
  public static final String COL_REGID            = "REGID";
  public static final String COL_REGFLAGS         = "REGFLAGS";
  public static final String COL_CALLBACK         = "CALLBACK";
  public static final String COL_OPERATIONS       = "OPERATIONS_FILTER";
  public static final String COL_CHANGELAG        = "CHANGELAG";
  public static final String COL_TIMEOUT          = "TIMEOUT";
  public static final String COL_TABLE_NAME       = "TABLE_NAME";

  private static final String[] COLUMN_NAMES = {
          COL_REGID,
          COL_REGFLAGS,
          COL_CALLBACK,
          COL_OPERATIONS,
          COL_CHANGELAG,
          COL_TIMEOUT,
          COL_TABLE_NAME
  };

  public DataChangeRegistrationBundle( ConnectionHandler connectionHandler) throws SQLException {
    super();
    this.connectionHandler = connectionHandler;
    refresh();
  }

  @Override
  public int getRowCount() {
    return allRegs.size();
  }

  @Override
  public int getColumnCount() {
    return COLUMN_NAMES.length;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    DataChangeRegistration reg = allRegs.get(rowIndex);
    switch (columnIndex) {
      case 0: return reg.getRegId();
      case 1: return reg.getRegFlags();
      case 2: return reg.getCallback();
      case 3: return reg.getOperationsFilter();
      case 4: return reg.getChangeLag();
      case 5: return reg.getTimeout();
      case 6: return reg.getTableName();
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
  public void refresh() {
    Background.run(()->{
      allRegs = registrationService.fetchRegistrations(connectionHandler);
      applyFilterAndRefresh();
    });
  }

  private void applyFilterAndRefresh() {
    List<DataChangeRegistration> filtered = activeOnly
            ? allRegs.stream().filter(DataChangeRegistration::isActive).collect(Collectors.toList())
            : allRegs;

    ApplicationManager.getApplication().invokeLater(() -> {
      viewRegs = filtered;
      notifyRowChanges();
    });
  }


}