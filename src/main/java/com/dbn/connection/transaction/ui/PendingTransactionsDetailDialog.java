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

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.transaction.DatabaseTransactionManager;
import com.dbn.connection.transaction.TransactionAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.List;

import static com.dbn.connection.transaction.TransactionAction.COMMIT;
import static com.dbn.connection.transaction.TransactionAction.ROLLBACK;
import static com.dbn.connection.transaction.TransactionAction.actions;

public class PendingTransactionsDetailDialog extends DBNDialog<PendingTransactionsDetailForm> {
    private final ConnectionRef connection;
    private final TransactionAction additionalOperation;
    private final boolean showActions;

    public PendingTransactionsDetailDialog(ConnectionHandler connection, TransactionAction additionalOperation, boolean showActions) {
        super(connection.getProject(), "Open transactions", true);
        this.connection = connection.ref();
        this.additionalOperation = additionalOperation;
        this.showActions = showActions;
        setModal(false);
        setResizable(true);
        init();
    }

    @NotNull
    @Override
    protected PendingTransactionsDetailForm createForm() {
        return new PendingTransactionsDetailForm(this, getConnection(), additionalOperation, showActions);
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                new CommitAction(),
                new RollbackAction(),
                getCancelAction(),
                getHelpAction()
        };
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }

    private class CommitAction extends AbstractAction {
        CommitAction() {
            super("Commit", Icons.CONNECTION_COMMIT);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                List<TransactionAction> actions = actions(COMMIT, additionalOperation);
                executeActions(actions);
            } finally {
                doOKAction();
            }
        }
    }

    private class RollbackAction extends AbstractAction {
        RollbackAction() {
            super("Rollback", Icons.CONNECTION_ROLLBACK);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                List<TransactionAction> actions = actions(ROLLBACK, additionalOperation);
                executeActions(actions);
            } finally {
                doOKAction();
            }
        }
    }

    protected void executeActions(List<TransactionAction>  actions) {
        ConnectionHandler connection = getConnection();
        List<DBNConnection> connections = getForm().getConnections();
        DatabaseTransactionManager transactionManager = getTransactionManager();
        for (DBNConnection conn : connections) {
            transactionManager.execute(connection, conn, actions, true, null);
        }
    }

    private DatabaseTransactionManager getTransactionManager() {
        Project project = getConnection().getProject();
        return DatabaseTransactionManager.getInstance(project);
    }
}
