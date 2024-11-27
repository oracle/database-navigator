/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.connection.resource.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionHandlerStatusListener;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.ConnectionType;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.transaction.PendingTransactionBundle;
import com.dbn.connection.transaction.TransactionAction;
import com.dbn.connection.transaction.TransactionListener;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;

import static com.dbn.common.ui.util.Splitters.makeRegular;

public class ResourceMonitorForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel detailsPanel;
    private JSplitPane contentSplitPane;
    private JList<ConnectionHandler> connectionsList;

    private final Map<ConnectionId, ResourceMonitorDetailForm> resourceMonitorForms = DisposableContainers.map(this);

    ResourceMonitorForm(ResourceMonitorDialog parentComponent) {
        super(parentComponent);
        makeRegular(contentSplitPane);

        connectionsList.addListSelectionListener(e -> {
            ConnectionHandler connection = connectionsList.getSelectedValue();
            showChangesForm(connection);
        });
        connectionsList.setCellRenderer(new ConnectionListCellRenderer());
        connectionsList.setSelectedIndex(0);
        updateListModel();


        ProjectEvents.subscribe(ensureProject(), this, TransactionListener.TOPIC, transactionListener);
        ProjectEvents.subscribe(ensureProject(), this, ConnectionHandlerStatusListener.TOPIC, (connectionId) -> UserInterface.repaint(connectionsList));
    }

    private void updateListModel() {
        checkDisposed();
        int selectionIndex = connectionsList.getSelectedIndex();
        DefaultListModel<ConnectionHandler> model = new DefaultListModel<>();
        ConnectionManager connectionManager = ConnectionManager.getInstance(ensureProject());
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
        List<ConnectionHandler> connections = connectionBundle.getConnections();
        for (ConnectionHandler connection : connections) {
            checkDisposed();
            model.addElement(connection);
        }
        connectionsList.setModel(model);

        if (selectionIndex < 0) {
            ConnectionHandler connection = ConnectionManager.getLastUsedConnection();
            if (connection != null) {
                selectionIndex = connections.indexOf(connection);
            }
        }
        selectionIndex = Integer.max(selectionIndex, 0);
        if (model.size() > selectionIndex) {
            connectionsList.setSelectedIndex(selectionIndex);
        }
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    private void showChangesForm(ConnectionHandler connection) {
        detailsPanel.removeAll();
        if (connection != null) {
            ConnectionId connectionId = connection.getConnectionId();
            ResourceMonitorDetailForm detailForm = resourceMonitorForms.get(connectionId);
            if (detailForm == null) {
                detailForm = new ResourceMonitorDetailForm(this, connection);
                resourceMonitorForms.put(connectionId, detailForm);
            }
            detailsPanel.add(detailForm.getComponent(), BorderLayout.CENTER);
        }

        UserInterface.repaint(detailsPanel);
    }

    private static class ConnectionListCellRenderer extends ColoredListCellRenderer<ConnectionHandler> {

        @Override
        protected void customizeCellRenderer(@NotNull JList list, ConnectionHandler value, int index, boolean selected, boolean hasFocus) {
            setIcon(value.getIcon());
            append(value.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            List<DBNConnection> connections = value.getConnections(ConnectionType.MAIN, ConnectionType.SESSION);
            int changes = 0;
            for (DBNConnection connection : connections) {
                PendingTransactionBundle dataChanges = connection.getDataChanges();
                changes += dataChanges == null ? 0 : dataChanges.size();
            }

/*            if (changes > 0) {
                append(" (" + changes + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
            }*/
        }
    }

    /********************************************************
     *                Transaction Listener                  *
     ********************************************************/
    private final TransactionListener transactionListener = new TransactionListener() {
        @Override
        public void afterAction(@NotNull ConnectionHandler connection, DBNConnection conn, TransactionAction action, boolean succeeded) {
            refreshForm();
        }
    };

    private void refreshForm() {
        Dispatch.run(() -> updateListModel());

    }
}
