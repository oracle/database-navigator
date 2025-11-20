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

package com.dbn.event.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.dbn.common.ui.util.Splitters;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.event.notification.ui.EventNotificationsForm;
import com.dbn.event.registration.EventRegistrationListener;
import com.dbn.object.DBTable;
import com.dbn.object.event.ObjectChangeAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListModel;
import java.util.List;
import java.util.Map;

import static com.dbn.common.ui.util.Borderless.markBorderless;

public class EventMonitorForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel detailsPanel;
    private JList<ConnectionHandler> connectionsList;
    private JSplitPane splitPane;

    @Setter
    private int tabSelectionIndex;

    private final Map<ConnectionId, EventMonitorDetailsForm> resourceMonitorForms = DisposableContainers.map(this);

    public EventMonitorForm(@NotNull Project project) {
        super(null, project);

        connectionsList.addListSelectionListener(e -> {
            ConnectionHandler connection = connectionsList.getSelectedValue();
            showDetailsForm(connection);
        });

        connectionsList.setCellRenderer(new ConnectionListCellRenderer());

        ListModel<ConnectionHandler> model = createModel();
        connectionsList.setModel(model);
        connectionsList.setSelectedIndex(0);
        markBorderless(connectionsList);

        Splitters.setSplitPaneProportion(splitPane, 0.2);

        ProjectEvents.subscribe(project, this,
                ConnectionConfigListener.TOPIC,
                ConnectionConfigListener.whenSetupChanged(() -> rebuildModel()));

        ProjectEvents.subscribe(project, this,
                EventRegistrationListener.TOPIC,
                createEventRegistrationListener());
    }

    private EventRegistrationListener createEventRegistrationListener() {
        return event -> {
            if (event.getAction() != ObjectChangeAction.CREATE) return;

            ConnectionHandler connection = ConnectionHandler.get(event.getConnectionId());
            if (connection == null) return;

            dispatch(() -> {
                tabSelectionIndex = 0; // registration tab
                connectionsList.setSelectedValue(connection, true);
            });
        };
    }

    public void selectContent(ConnectionId connectionId, int index) {
        if (connectionId == null) return;
        if (index < 0) return;

        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        if (connection == null) return;

        connectionsList.setSelectedValue(connection, true);
        EventMonitorDetailsForm detailsForm = ensureDetailsForm(connectionId);
        detailsForm.selectTab(index);
    }

    public void showTableNotifications(DBTable table) {
        ConnectionId connectionId = table.getConnectionId();
        selectContent(connectionId, 1);

        EventMonitorDetailsForm detailsForm = ensureDetailsForm(connectionId);
        EventNotificationsForm notificationsForm = detailsForm.getNotificationsForm();
        notificationsForm.applyTableFilter(table);
    }

    private void rebuildModel() {
        ListModel<ConnectionHandler> model = createModel();
        connectionsList.setModel(model);
    }

    @NotNull
    private ListModel<ConnectionHandler> createModel() {
        DefaultListModel<ConnectionHandler> model = new DefaultListModel<>();
        ConnectionManager connectionManager = ConnectionManager.getInstance(ensureProject());
        List<ConnectionHandler> connections = connectionManager.getConnections(c -> c.getDatabaseType() == DatabaseType.ORACLE);
        for (ConnectionHandler connection : connections) {
            model.addElement(connection);
        }
        return model;
    }

    private void showDetailsForm(ConnectionHandler connection) {
        detailsPanel.removeAll();
        if (connection == null) return;

        ConnectionId connectionId = connection.getConnectionId();
        EventMonitorDetailsForm detailForm = ensureDetailsForm(connectionId);

        detailsPanel.add(detailForm.getComponent());
        detailForm.selectTab(tabSelectionIndex);

        UserInterface.repaint(detailsPanel);
    }

    @NotNull
    private EventMonitorDetailsForm ensureDetailsForm(ConnectionId connectionId) {
        EventMonitorDetailsForm detailForm = resourceMonitorForms.get(connectionId);
        if (detailForm == null) {
            ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
            detailForm = new EventMonitorDetailsForm(this, connection);
            resourceMonitorForms.put(connectionId, detailForm);
        }
        return detailForm;
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
