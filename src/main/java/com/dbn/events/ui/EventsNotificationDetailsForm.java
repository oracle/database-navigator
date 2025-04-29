package com.dbn.events.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.tab.DBNTabbedPane;
import com.dbn.common.ui.table.DBNMutableTableModel;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.table.DBNTableModel;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.events.EventNotificationManager;
import com.dbn.events.model.DataChangeEventBundle;
import com.dbn.events.model.DataChangeRegistrationBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

import static com.dbn.common.dispose.Failsafe.nd;

public class EventsNotificationDetailsForm extends DBNFormBase {
  private final DBNTable<DBNTableModel> eventsTable;
  private final  DBNTable<DBNTableModel> registrationsTable;
//  private final DBNTable<AbstractDiagnosticsTableModel> connectivityTable;

  private JPanel mainPanel;
  private JPanel headerPanel;
  private JPanel diagnosticsTabsPanel;
  private final DBNTabbedPane<DBNTable> diagnosticsTabs;

  public EventsNotificationDetailsForm(@NotNull com.dbn.events.ui.EventsNotificationForm parent, ConnectionHandler connection) throws SQLException {
    super(parent);

    DBNHeaderForm headerForm = new DBNHeaderForm(this, connection).withEmptyBorder();
    headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

    diagnosticsTabs = new DBNTabbedPane<>(this);
    diagnosticsTabsPanel.add(diagnosticsTabs, BorderLayout.CENTER);
    diagnosticsTabs.enableFocusInheritance();

//    DBNTable dbnTable = new DBNTable()  ;
    DataChangeEventBundle  model = (EventNotificationManager.getInstance(getProject()).getEventBundle(connection.getConnectionId()));
    eventsTable = new NotificationEventTable(this,model).getTable();

//    AbstractDiagnosticsTableModel metadataTableModel = new MetadataDiagnosticsTableModel2(connection);
//    metadataTable = new DiagnosticsTable<>(this, metadataTableModel);
//    metadataTable.getRowSorter().toggleSortOrder(0);
    addTab(eventsTable, "Events");
//

    DataChangeRegistrationBundle registrationModel = new DataChangeRegistrationBundle(connection);
    registrationsTable = new DBNTable<>(this,registrationModel,true);
//    AbstractDiagnosticsTableModel connectivityTableModel = new ConnectivityDiagnosticsTableModel(connection);
//    connectivityTable = new DiagnosticsTable<>(this, connectivityTableModel);
//    connectivityTable.getRowSorter().toggleSortOrder(0);
    addTab(registrationsTable, "Registrations");


    diagnosticsTabs.addTabSelectionListener(i -> {
      com.dbn.events.ui.EventsNotificationForm parentForm = nd(getParentComponent());
      parentForm.setTabSelectionIndex(i);

    });
  }

  private void addTab(DBNTable component, String title) {
    JScrollPane scrollPane = new DBNScrollPane(component);
    diagnosticsTabs.addTab(title, scrollPane, component);
  }

  public void selectTab(int tabIndex) {
    diagnosticsTabs.setSelectedIndex(tabIndex);
    DBNTable table = diagnosticsTabs.getContentAt(tabIndex);
    DBNMutableTableModel model = (DBNMutableTableModel) table.getModel();
    model.notifyRowChanges();
  }


  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }


  public void updateEvents(ConnectionId connectionId) {
    DataChangeEventBundle  newModel = (EventNotificationManager.getInstance(getProject()).getEventBundle(connectionId));

    eventsTable.setModel(newModel);
  }
}