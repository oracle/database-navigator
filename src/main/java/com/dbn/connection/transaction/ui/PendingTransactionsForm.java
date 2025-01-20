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

package com.dbn.connection.transaction.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.ConnectionType;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.transaction.PendingTransactionBundle;
import com.dbn.connection.transaction.TransactionAction;
import com.dbn.connection.transaction.TransactionListener;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PendingTransactionsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel detailsPanel;
    private JList<ConnectionHandler> connectionsList;
    private final List<ConnectionHandler> connections = new ArrayList<>();

    private final Map<ConnectionId, PendingTransactionsDetailForm> uncommittedChangeForms = DisposableContainers.map(this);

    PendingTransactionsForm(PendingTransactionsDialog parentComponent) {
        super(parentComponent);
        mainPanel.setBorder(Borders.BOTTOM_LINE_BORDER);

        connectionsList.addListSelectionListener(e -> {
            ConnectionHandler connection = connectionsList.getSelectedValue();
            showChangesForm(connection);
        });

        connectionsList.setCellRenderer(new ListCellRenderer());
        connectionsList.setSelectedIndex(0);
        updateListModel();

        ProjectEvents.subscribe(ensureProject(), this, TransactionListener.TOPIC, transactionListener);
    }

    private void updateListModel() {
        checkDisposed();
        DefaultListModel<ConnectionHandler> model = new DefaultListModel<>();
        ConnectionManager connectionManager = ConnectionManager.getInstance(ensureProject());
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
        for (ConnectionHandler connection : connectionBundle.getConnections()) {
            if (connection.hasUncommittedChanges()) {
                connections.add(connection);
                model.addElement(connection);
            }
        }
        connectionsList.setModel(model);
        if (model.size() > 0) {
            connectionsList.setSelectedIndex(0);
        }
    }

    boolean hasUncommittedChanges() {
        for (ConnectionHandler connection : connections) {
            if (connection.hasUncommittedChanges()) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public List<ConnectionHandler> getConnections(){
        return connections;
    }

    public void showChangesForm(ConnectionHandler connection) {
        detailsPanel.removeAll();
        if (connection != null) {
            ConnectionId connectionId = connection.getConnectionId();
            PendingTransactionsDetailForm pendingTransactionsForm = uncommittedChangeForms.get(connectionId);
            if (pendingTransactionsForm == null) {
                pendingTransactionsForm = new PendingTransactionsDetailForm(this, connection, null, true);
                uncommittedChangeForms.put(connectionId, pendingTransactionsForm);
            }
            detailsPanel.add(pendingTransactionsForm.getComponent(), BorderLayout.CENTER);
        }

        UserInterface.repaint(detailsPanel);
    }

    private static class ListCellRenderer extends ColoredListCellRenderer<Object> {
        @Override
        protected void customize(@NotNull JList list, Object value, int index, boolean selected, boolean hasFocus) {
            ConnectionHandler connection = (ConnectionHandler) value;
            setIcon(connection.getIcon());
            append(connection.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            List<DBNConnection> connections = connection.getConnections(ConnectionType.MAIN, ConnectionType.SESSION);
            int changes = 0;
            for (DBNConnection conn : connections) {
                PendingTransactionBundle dataChanges = conn.getDataChanges();
                changes += dataChanges == null ? 0 : dataChanges.size();
            }

            append(" (" + changes + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
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


    @Override
    public void disposeInner() {
        connections.clear();
        super.disposeInner();
    }

}
