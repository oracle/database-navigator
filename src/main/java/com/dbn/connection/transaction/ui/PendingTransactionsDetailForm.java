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

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.transaction.DatabaseTransactionManager;
import com.dbn.connection.transaction.TransactionAction;
import com.dbn.connection.transaction.TransactionListener;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.util.List;

import static com.dbn.connection.transaction.TransactionAction.actions;


public class PendingTransactionsDetailForm extends DBNFormBase {
    private final PendingTransactionsTable pendingTransactionsTable;
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JButton commitButton;
    private JButton rollbackButton;
    private JPanel transactionActionsPanel;
    private DBNScrollPane changesTableScrollPane;

    private final ConnectionRef connection;

    PendingTransactionsDetailForm(@NotNull DBNComponent parent, @NotNull ConnectionHandler connection, TransactionAction additionalOperation, boolean showActions) {
        super(parent);
        this.connection = connection.ref();

        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        PendingTransactionsTableModel transactionsTableModel = new PendingTransactionsTableModel(connection);
        pendingTransactionsTable = new PendingTransactionsTable(this, transactionsTableModel);
        changesTableScrollPane.setViewportView(pendingTransactionsTable);

        transactionActionsPanel.setVisible(showActions);
        if (showActions) {
            ActionListener actionListener = e -> {
                Project project = connection.getProject();
                DatabaseTransactionManager transactionManager = DatabaseTransactionManager.getInstance(project);
                List<DBNConnection> connections = pendingTransactionsTable.getSelectedConnections();
                Object source = e.getSource();

                for (DBNConnection conn : connections) {
                    if (source == commitButton) {
                        transactionManager.commit(connection, conn);
                    } else if (source == rollbackButton) {
                        List<TransactionAction> actions = actions(TransactionAction.ROLLBACK, additionalOperation);
                        transactionManager.rollback(connection, conn);
                    }
                }
            };

            commitButton.addActionListener(actionListener);
            commitButton.setIcon(Icons.CONNECTION_COMMIT);

            rollbackButton.addActionListener(actionListener);
            rollbackButton.setIcon(Icons.CONNECTION_ROLLBACK);

            ListSelectionModel selectionModel = pendingTransactionsTable.getSelectionModel();
            selectionModel.addListSelectionListener(e -> updateTransactionActions());

            if (transactionsTableModel.getRowCount()> 0) {
                pendingTransactionsTable.selectCell(0, 0);
            }
            updateTransactionActions();

        }
        ProjectEvents.subscribe(ensureProject(), this, TransactionListener.TOPIC, transactionListener);
    }

    private void updateTransactionActions() {
        List<DBNConnection> connections = pendingTransactionsTable.getSelectedConnections();
        boolean selectionAvailable = connections.size() > 0;
        commitButton.setEnabled(selectionAvailable);
        rollbackButton.setEnabled(selectionAvailable);
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @NotNull
    public List<DBNConnection> getConnections() {
        return pendingTransactionsTable.getModel().getTransactionalConnections();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    /********************************************************
     *                Transaction Listener                  *
     ********************************************************/
    private final TransactionListener transactionListener = new TransactionListener() {
        @Override
        public void afterAction(@NotNull ConnectionHandler connection, DBNConnection conn, TransactionAction action, boolean succeeded) {
            if (connection == getConnection() && succeeded) {
                refreshForm(connection);
            }
        }
    };

    private void refreshForm(ConnectionHandler connection) {
        Dispatch.run(() -> {
            checkDisposed();
            PendingTransactionsTableModel transactionsTableModel = new PendingTransactionsTableModel(connection);
            pendingTransactionsTable.setModel(transactionsTableModel);
            if (transactionsTableModel.getRowCount() > 0) {
                pendingTransactionsTable.selectCell(0, 0);
            }
            updateTransactionActions();
        });
    }
}
