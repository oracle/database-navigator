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

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.ui.table.DBNReadonlyTableModel;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.session.DatabaseSession;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ResourceMonitorSessionsTableModel extends StatefulDisposableBase implements DBNReadonlyTableModel {
    private final ConnectionRef connection;
    private final List<DatabaseSession> sessions;

    ResourceMonitorSessionsTableModel(ConnectionHandler connection) {
        this.connection = connection.ref();
        sessions = connection.getSessionBundle().getSessions();
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @NotNull
    public Project getProject() {
        return getConnection().getProject();
    }

    @NotNull
    public List<DatabaseSession> getSessions() {
        return Failsafe.nn(sessions);
    }

    @Override
    public int getRowCount() {
        return getSessions().size();
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0: return "Session";
            case 1: return "Status";
            case 2: return "Last Access";
            case 3: return "Open Connections / Peak";
            case 4: return "Open Cursors";
            case 5: return "Cached Statements";
        }
        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return DatabaseSession.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return getSession(rowIndex);
    }

    @Nullable
    public DatabaseSession getSession(int rowIndex) {
        return rowIndex == -1 ? null : sessions.get(rowIndex);
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
