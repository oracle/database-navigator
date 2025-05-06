package com.dbn.events.model;

import com.dbn.common.thread.Background;
import com.dbn.common.ui.table.DBNMutableTableModel;
import com.dbn.connection.ConnectionHandler;
import com.dbn.events.RegistrationManager;
import com.dbn.events.service.RegistrationService;
import com.intellij.openapi.application.ApplicationManager;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
  @Setter
  private String regStatusFilter = "All";
  @Setter
  private String tableNameFilter = "All";

  public DataChangeRegistrationBundle( ConnectionHandler connectionHandler,Runnable onFinish) throws SQLException {
    super();
    this.connectionHandler = connectionHandler;
    refresh(onFinish);
  }

  @Override
  public int getRowCount() {
    return viewRegs.size();
  }

  @Override
  public int getColumnCount() {
    return COLUMN_NAMES.length;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    DataChangeRegistration reg = viewRegs.get(rowIndex);
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
  public void refresh(Runnable onFinish) {
    Background.run(()->{
      allRegs = registrationService.fetchRegistrations(connectionHandler);
      applyFilterAndRefresh();
      if (onFinish != null) {
        ApplicationManager.getApplication().invokeLater(onFinish);
      }
    });
  }

  public void applyFilterAndRefresh() {

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
  }


  public Set<String> getUniqueTableNames() {

    Set<String> tableNames = new HashSet<>();
    for (DataChangeRegistration reg : allRegs) {
      tableNames.add(reg.getTableName());
    }
    return tableNames;
  }

  public Set<Long> getActiveRegistrations( RegistrationManager dcnListenerManager) throws SQLException {
    Set<Long> activeRegs = new HashSet<>((Collection) dcnListenerManager.getActiveRegistrations());
    List <Long> allRegsId = allRegs.stream()
            .map(DataChangeRegistration::getRegId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    Set<Long> dbRegs = new HashSet<>(allRegsId);

    activeRegs.retainAll(dbRegs); // Retain only those present in both sets
    return activeRegs;
  }


  public List<String> getAllTableNames() {
//    try {
//      allRegs = registrationService.fetchRegistrations(connectionHandler);
//    } catch (SQLException e) {
//      throw new RuntimeException(e);
//    }

    Set<String> tableNames = new HashSet<>();
    for (DataChangeRegistration reg : allRegs) {
      tableNames.add(reg.getTableName());
    }
    return new ArrayList<>(tableNames) ;
  }


}