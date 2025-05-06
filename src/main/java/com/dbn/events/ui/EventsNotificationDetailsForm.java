package com.dbn.events.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.tab.DBNTabbedPane;
import com.dbn.common.ui.table.DBNMutableTableModel;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.table.DBNTableModel;
import com.dbn.connection.ConnectionHandler;
import com.dbn.events.model.DataChangeEventBundle;
import com.dbn.events.model.DataChangeRegistrationBundle;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.dispose.Failsafe.nd;

public class EventsNotificationDetailsForm extends DBNFormBase {
  private final DBNTable<DBNTableModel> eventsTable;
  private final DBNTable<DBNTableModel> registrationsTable;
  private final JComboBox<String> tableNameFilterComboBox;
  private final JComboBox<String> regStatusFilterComboBox;

  private JPanel mainPanel;
  private JPanel headerPanel;
  private JPanel diagnosticsTabsPanel;
  private final DBNTabbedPane<DBNTable> diagnosticsTabs;

  public EventsNotificationDetailsForm(@NotNull com.dbn.events.ui.EventsNotificationForm parent, ConnectionHandler connection) throws SQLException {
    super(parent);

    // Initialize components
    diagnosticsTabs = new DBNTabbedPane<>(this);
    diagnosticsTabsPanel.add(diagnosticsTabs, BorderLayout.CENTER);
    diagnosticsTabs.enableFocusInheritance();

    DataChangeEventBundle eventModel = new DataChangeEventBundle(connection.getConnectionId().toString());
    tableNameFilterComboBox = new ComboBox<>(new String[]{"Loading..."});
    tableNameFilterComboBox.setEnabled(false);
    DataChangeRegistrationBundle[] modelHolder = new DataChangeRegistrationBundle[1];

    modelHolder[0] = new DataChangeRegistrationBundle(connection, () -> {
      List<String> tableNames = modelHolder[0].getAllTableNames();
      tableNames.add(0, "All");  // Add "All" option to the beginning of the list

      tableNameFilterComboBox.setModel(new DefaultComboBoxModel<>(tableNames.toArray(new String[0])));
      tableNameFilterComboBox.setEnabled(true);
    });


    regStatusFilterComboBox = new ComboBox<>(new String[]{"All", "Active", "Inactive"});

    // Add action listeners to filters
    regStatusFilterComboBox.addActionListener(e -> updateEventFilter());
    tableNameFilterComboBox.addActionListener(e -> updateEventFilter());

    // Set up layout and add components
    setUpLayout(modelHolder[0]);

    // Initialize tables
    eventsTable = new NotificationEventTable(this, eventModel).getTable();
    addTab(eventsTable, "Events");

    registrationsTable = new DBNTable<>(this, modelHolder[0], true);
    addTab(registrationsTable, "Registrations");

    diagnosticsTabs.addTabSelectionListener(i -> {
      com.dbn.events.ui.EventsNotificationForm parentForm = nd(getParentComponent());
      parentForm.setTabSelectionIndex(i);
    });
  }

  private void setUpLayout(DataChangeRegistrationBundle dataChangeRegistrationBundle) {
    // Create toolbar
    JButton refreshButton = new JButton("Refresh");
    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(new JLabel("Table:"));
    toolbar.add(tableNameFilterComboBox);
    toolbar.add(new JLabel("Status:"));
    toolbar.add(regStatusFilterComboBox);
    toolbar.add(refreshButton);
    refreshButton.addActionListener(e -> {
      tableNameFilterComboBox.setEnabled(false);
      dataChangeRegistrationBundle.refresh(()->{
        List<String> tableNames = dataChangeRegistrationBundle.getAllTableNames();
        tableNames.add(0, "All");  // Add "All" option to the beginning of the list

        tableNameFilterComboBox.setModel(new DefaultComboBoxModel<>(tableNames.toArray(new String[0])));
        tableNameFilterComboBox.setEnabled(true);
      });
    });

    // Add toolbar to the panel
    headerPanel.add(toolbar,BorderLayout.AFTER_LAST_LINE );
  }

  private void updateEventFilter() {
    String selectedTable = (String) tableNameFilterComboBox.getSelectedItem();
    String selectedStatus = (String) regStatusFilterComboBox.getSelectedItem();

    // Apply filtering logic to update the events in the table model
    DataChangeEventBundle eventsModel = (DataChangeEventBundle) eventsTable.getModel();
    DataChangeRegistrationBundle registrationModel = (DataChangeRegistrationBundle) registrationsTable.getModel();

    eventsModel.setTableNameFilter(selectedTable);  // Filter by specific table name
    registrationModel.setTableNameFilter(selectedTable);  // Filter by specific table name

    // Apply filter based on the status (All, Active, Inactive)
    eventsModel.setRegStatusFilter(selectedStatus);
    registrationModel.setRegStatusFilter(selectedStatus);

    // Reload filtered events
    eventsModel.loadEvents();
    registrationModel.applyFilterAndRefresh();
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
}