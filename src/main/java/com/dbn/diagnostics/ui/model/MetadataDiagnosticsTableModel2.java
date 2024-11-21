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
import com.dbn.diagnostics.DiagnosticsManager;
import com.dbn.diagnostics.data.DiagnosticBundle;
import com.dbn.diagnostics.data.DiagnosticEntry;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class MetadataDiagnosticsTableModel2 extends AbstractDiagnosticsTableModel<String> {
    private final ConnectionRef connection;

    private static final String[] COLUMN_NAMES = new String[]{
            "Identifier",                  // 0
            "Invocations",                 // 1
            "Failures",                    // 2
            "Timeouts",                    // 3
            "Best (Query / Load - ms)",    // 4
            "Worst (Query / Load - ms)",   // 5
            "Average (Query / Load - ms)", // 6
            "Total (Query / Load - ms)",   // 7
            "Fetch Block Size"};           // 8

    public MetadataDiagnosticsTableModel2(ConnectionHandler connection) {
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
    protected DiagnosticBundle<String> resolveDiagnostics() {
        DiagnosticsManager diagnosticsManager = DiagnosticsManager.getInstance(getProject());
        return diagnosticsManager.getMetadataInterfaceDiagnostics(connection.getConnectionId());
    }

    @Override
    public Object getValue(DiagnosticEntry<String> entry, int column) {
        DiagnosticEntry<String> q = entry.getDetail("QUERY");
        DiagnosticEntry<String> l = entry.getDetail("LOAD");
        switch (column) {
            case 0: return q.getIdentifier();
            case 1: return q.getInvocations();
            case 2: return q.getFailures();
            case 3: return q.getTimeouts();
            case 4: return q.getBest() + " / " + l.getBest();
            case 5: return q.getWorst() + " / " + l.getWorst();
            case 6: return q.getAverage() + " / " + l.getAverage();
            case 7: return q.getTotal() + " / " + l.getTotal();
            case 8: return entry.getDetail("FETCH_BLOCK").getAverage();
        }
        return "";
    }

    @Override
    public String getPresentableValue(DiagnosticEntry<String> entry, int column) {
        return getValue(entry, column).toString();
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @NotNull
    public Project getProject() {
        return getConnection().getProject();
    }
}
