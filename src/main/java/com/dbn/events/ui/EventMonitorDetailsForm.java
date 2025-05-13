package com.dbn.events.ui;

import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.tab.DBNTabbedPane;
import com.dbn.connection.ConnectionHandler;
import com.dbn.events.listener.model.DataChangeListenerBundle;
import com.dbn.events.listener.ui.EventListenersForm;
import com.dbn.events.notification.model.DataChangeEventBundle;
import com.dbn.events.notification.ui.EventNotificationsForm;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static com.dbn.common.dispose.Failsafe.nd;

public class EventMonitorDetailsForm extends DBNFormBase {
  private JPanel mainPanel;
  private JPanel tabsPanel;
  private JPanel headerPanel;
  private final DBNTabbedPane<DBNForm> contentTabs;

  public EventMonitorDetailsForm(@NotNull EventMonitorForm parent, ConnectionHandler connection) {
    super(parent);

    // Initialize components
    contentTabs = new DBNTabbedPane<>(this);
    tabsPanel.add(contentTabs, BorderLayout.CENTER);
    contentTabs.enableFocusInheritance();

    DataChangeEventBundle eventModel = new DataChangeEventBundle(connection.getConnectionId().toString());
    DataChangeListenerBundle registrationModel = new DataChangeListenerBundle(connection);

    // Initialize tables
    EventListenersForm listenersForm = new EventListenersForm(this, registrationModel);
    contentTabs.addTab("Listeners", listenersForm.getComponent(), listenersForm);

    EventNotificationsForm notificationsForm = new EventNotificationsForm(this, eventModel);
    contentTabs.addTab("Notifications", notificationsForm.getComponent(), notificationsForm);

    initFormHeader(connection);

    contentTabs.addTabSelectionListener(i -> {
      EventMonitorForm parentForm = nd(getParentComponent());
      parentForm.setTabSelectionIndex(i);
    });
  }

  private void initFormHeader(ConnectionHandler connection) {
    DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
    headerPanel.add(headerForm.getComponent());
  }

 /* private void setUpLayout(DataChangeRegistrationBundle dataChangeRegistrationBundle) {
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

  */

  public void selectTab(int tabIndex) {
    contentTabs.setSelectedIndex(tabIndex);
  }


  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}