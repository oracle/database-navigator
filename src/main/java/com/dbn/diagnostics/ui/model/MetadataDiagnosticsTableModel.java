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
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

public class MetadataDiagnosticsTableModel extends AbstractDiagnosticsTableModel<String> {
    private final ConnectionRef connection;

    private static final String[] COLUMN_NAMES = new String[] {
            txt("app.diagnostics.column.Identifier"),
            txt("app.diagnostics.column.Invocations"),
            txt("app.diagnostics.column.Failures"),
            txt("app.diagnostics.column.Timeouts"),
            txt("app.diagnostics.column.BestQueryMillis"),
            txt("app.diagnostics.column.BestLoadMillis"),
            txt("app.diagnostics.column.WorstQueryMillis"),
            txt("app.diagnostics.column.WorstLoadMillis"),
            txt("app.diagnostics.column.AverageQueryMillis"),
            txt("app.diagnostics.column.AverageLoadMillis"),
            txt("app.diagnostics.column.TotalQueryMillis"),
            txt("app.diagnostics.column.TotalLoadMillis"),
            txt("app.diagnostics.column.FetchBlockSize")};

    public MetadataDiagnosticsTableModel(ConnectionHandler connection) {
        super(connection.getProject());
        this.connection = connection.ref();
    }

    @NotNull
    @Override
    protected @Nls String[] getColumnNames() {
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
        return switch (column) {
            case 0 -> q.getIdentifier();
            case 1 -> q.getInvocations();
            case 2 -> q.getFailures();
            case 3 -> q.getTimeouts();
            case 4 -> q.getBest();
            case 5 -> l.getBest();
            case 6 -> q.getWorst();
            case 7 -> l.getWorst();
            case 8 -> q.getAverage();
            case 9 -> l.getAverage();
            case 10 -> q.getTotal();
            case 11 -> l.getTotal();
            case 12 -> entry.getDetail("FETCH_BLOCK").getAverage();
            default -> "";
        };
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
