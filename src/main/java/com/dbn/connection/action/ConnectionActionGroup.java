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

package com.dbn.connection.action;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.transaction.action.AutoCommitToggleAction;
import com.dbn.connection.transaction.action.AutoConnectToggleAction;
import com.dbn.connection.transaction.action.DatabaseLoggingToggleAction;
import com.dbn.connection.transaction.action.PendingTransactionsOpenAction;
import com.dbn.connection.transaction.action.TransactionCommitAction;
import com.dbn.connection.transaction.action.TransactionRollbackAction;
import com.dbn.diagnostics.action.BulkLoadAllObjectsAction;
import com.dbn.diagnostics.action.MiscellaneousConnectionAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

public class ConnectionActionGroup extends DefaultActionGroup {

    public ConnectionActionGroup(ConnectionHandler connection) {
        //add(new ConnectAction(connection));
        add(new TransactionCommitAction(connection));
        add(new TransactionRollbackAction(connection));
        add(new AutoCommitToggleAction(connection));
        add(new DatabaseLoggingToggleAction(connection));
        addSeparator();
        add(new SQLConsoleOpenAction(connection));
        add(new PendingTransactionsOpenAction(connection));
        addSeparator();
        add(new AutoConnectToggleAction(connection));
        add(new DatabaseConnectAction(connection));
        add(new DatabaseDisconnectAction(connection));
        add(new DatabaseConnectivityTestAction(connection));
        add(new BulkLoadAllObjectsAction(connection));
        add(new MiscellaneousConnectionAction(connection));
        addSeparator();
        add(new DatabaseInformationOpenAction(connection));
        add(new ConnectionSettingsOpenAction(connection));
    }
}
