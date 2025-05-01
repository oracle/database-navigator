package com.dbn.events.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.config.ConnectionConfigListener;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static com.dbn.common.ui.util.Borderless.markBorderless;

public class EventsNotificationForm extends DBNFormBase {
  private JPanel mainPanel;
  private JPanel detailsPanel;
  private JList<ConnectionHandler> connectionsList;
  private int tabSelectionIndex;

  private final Map<ConnectionId, EventsNotificationDetailsForm> resourceMonitorForms = DisposableContainers.map(this);

  public EventsNotificationForm(@NotNull Project project) {
    super(null, project);
    if (connectionsList == null) {
      connectionsList = new JBList<>();
    }
    connectionsList.addListSelectionListener(e -> {
      ConnectionHandler connection = connectionsList.getSelectedValue();
      showDetailsForm(connection);
    });
    connectionsList.setCellRenderer(new ConnectionListCellRenderer());

    ListModel<ConnectionHandler> model = createModel();
    connectionsList.setModel(model);
    connectionsList.setSelectedIndex(0);
    markBorderless(connectionsList);

    ProjectEvents.subscribe(project, this,
            ConnectionConfigListener.TOPIC,
            ConnectionConfigListener.whenSetupChanged(() -> rebuildModel()));
  }

  private void rebuildModel() {
    ListModel<ConnectionHandler> model = createModel();
    connectionsList.setModel(model);
  }

  @NotNull
  private ListModel<ConnectionHandler> createModel() {
    DefaultListModel<ConnectionHandler> model = new DefaultListModel<>();
    ConnectionManager connectionManager = ConnectionManager.getInstance(ensureProject());
    ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
    //todo filter just the oracle dbs
    List<ConnectionHandler> connections = connectionBundle.getConnections();
    for (ConnectionHandler connection : connections) {
      model.addElement(connection);
    }
    return model;
  }

  private void showDetailsForm(ConnectionHandler connection) {
    detailsPanel.removeAll();
    if (connection != null) {
      ConnectionId connectionId = connection.getConnectionId();
      EventsNotificationDetailsForm detailForm = resourceMonitorForms.get(connectionId);
      if (detailForm == null) {
        try {
          detailForm = new EventsNotificationDetailsForm(this, connection);
        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
        resourceMonitorForms.put(connectionId, detailForm);
      }
      detailsPanel.add(detailForm.getComponent(), BorderLayout.CENTER);
      detailForm.selectTab(tabSelectionIndex);
    }

    UserInterface.repaint(detailsPanel);
  }

  public void setTabSelectionIndex(int tabSelectionIndex) {
    this.tabSelectionIndex = tabSelectionIndex;
  }



  private static class ConnectionListCellRenderer extends ColoredListCellRenderer<ConnectionHandler> {

    @Override
    protected void customize(@NotNull JList<? extends ConnectionHandler> list, ConnectionHandler value, int index, boolean selected, boolean hasFocus) {
      setIcon(value.getIcon());
/*            if (!selected) {
                JBColor color = Commons.nvl(value.getEnvironmentType().getColor(), JBColor.WHITE);
                setBackground(Colors.softer(color, 30));
            }*/
      append(value.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }
  }


  @NotNull
  @Override
  public JPanel getMainComponent() {
    return mainPanel;
  }
}
