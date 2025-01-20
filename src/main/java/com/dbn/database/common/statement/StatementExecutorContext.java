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

package com.dbn.database.common.statement;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNStatement;
import com.dbn.diagnostics.DiagnosticsManager;
import com.dbn.diagnostics.data.DiagnosticBundle;
import com.dbn.diagnostics.data.DiagnosticEntry;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NonNls;

@Getter
@Setter
public final class StatementExecutorContext {
    private final DiagnosticBundle<String> diagnostics;
    private final String identifier;
    private final int timeout;
    private final DBNConnection connection;

    private DBNStatement statement;

    public StatementExecutorContext(DBNConnection connection, String identifier, int timeout) {
        DiagnosticsManager diagnosticsManager = DiagnosticsManager.getInstance(connection.getProject());

        this.connection = connection;
        this.diagnostics =  diagnosticsManager.getMetadataInterfaceDiagnostics(connection.getConnectionId());
        this.identifier = identifier;
        this.timeout = timeout;
    }

    public DiagnosticEntry<String> log(@NonNls String qualifier, boolean failure, boolean timeout, long value) {
        return diagnostics.log(identifier, qualifier, failure, timeout, value);
    }
}
