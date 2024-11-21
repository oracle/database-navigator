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

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.ui.table.DBNReadonlyTableModel;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.transaction.PendingTransaction;
import com.dbn.connection.transaction.PendingTransactionBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class ResourceMonitorTransactionsTableModel extends StatefulDisposableBase implements DBNReadonlyTableModel, Disposable {
    private final ConnectionRef connection;
    private DBNConnection conn;

    public ResourceMonitorTransactionsTableModel(ConnectionHandler connection, @Nullable DBNConnection conn) {
        this.connection = connection.ref();
        this.conn = conn;
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @NotNull
    public Project getProject() {
        return getConnection().getProject();
    }

    @Override
    public int getRowCount() {
        PendingTransactionBundle dataChanges = conn == null ? null : conn.getDataChanges();
        return dataChanges == null ? 0 : dataChanges.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return
            columnIndex == 0 ? "Source" :
            columnIndex == 1 ? "Details" : null ;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return PendingTransaction.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        PendingTransactionBundle dataChanges = conn == null ? null : conn.getDataChanges();
        if (dataChanges != null) {
            return dataChanges.getEntries().get(rowIndex);
        }
        return null;
    }

    @Override
    public void disposeInner() {
        conn = null;
    }
}
