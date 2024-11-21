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

package com.dbn.diagnostics.ui.model;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.SessionId;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.diagnostics.DiagnosticsManager;
import com.dbn.diagnostics.data.DiagnosticBundle;
import com.dbn.diagnostics.data.DiagnosticEntry;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ConnectivityDiagnosticsTableModel extends AbstractDiagnosticsTableModel<SessionId> {
    private final ConnectionRef connection;

    private static final String[] COLUMN_NAMES = new String[] {
            "Session",
            "Attempts",
            "Failures",
            "Timeouts",
            "Average Time (ms)",
            "Total Time (ms)"};

    public ConnectivityDiagnosticsTableModel(ConnectionHandler connection) {
        super(connection.getProject());
        this.connection = connection.ref();
    }

    @NotNull
    @Override
    protected String[] getColumnNames() {
        return COLUMN_NAMES;
    }

    @NotNull
    @Override
    protected DiagnosticBundle<SessionId> resolveDiagnostics() {
        DiagnosticsManager diagnosticsManager = DiagnosticsManager.getInstance(getProject());
        return diagnosticsManager.getConnectivityDiagnostics(connection.getConnectionId());
    }

    @Override
    public Object getValue(DiagnosticEntry<SessionId> entry, int column) {
        switch (column) {
            case 0: return getSession(entry.getIdentifier());
            case 1: return entry.getInvocations();
            case 2: return entry.getFailures();
            case 3: return entry.getTimeouts();
            case 4: return entry.getAverage();
            case 5: return entry.getTotal();
        }
        return "";
    }

    @NotNull
    private DatabaseSession getSession(SessionId sessionId) {
        return getConnection().getSessionBundle().getSession(sessionId);
    }

    @Override
    public String getPresentableValue(DiagnosticEntry<SessionId> entry, int column) {
        switch (column) {
            case 0: return getSession(entry.getIdentifier()).getName();
            case 1: return Long.toString(entry.getInvocations());
            case 2: return Long.toString(entry.getFailures());
            case 3: return Long.toString(entry.getTimeouts());
            case 4: return Long.toString(entry.getAverage());
            case 5: return Long.toString(entry.getTotal());
        }
        return "";
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @NotNull
    public Project getProject() {
        return getConnection().getProject();
    }
}
