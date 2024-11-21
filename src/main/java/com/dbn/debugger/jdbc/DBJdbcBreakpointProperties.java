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

package com.dbn.debugger.jdbc;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.debugger.common.breakpoint.DBBreakpointProperties;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import org.jetbrains.annotations.Nullable;

public class DBJdbcBreakpointProperties extends XBreakpointProperties<DBJdbcBreakpointProperties> implements DBBreakpointProperties {
    @Attribute(value = "connection-id", converter = ConnectionId.Converter.class)
    private ConnectionId connectionId;
    private ConnectionRef connection;

    public DBJdbcBreakpointProperties() {
    }

    public DBJdbcBreakpointProperties(ConnectionHandler connection) {
        this.connection = ConnectionRef.of(connection);
        if (connection != null) {
            connectionId = connection.getConnectionId();
        }
    }

    @Override
    public ConnectionId getConnectionId() {
        return connectionId;
    }

    @Override
    @Nullable
    public ConnectionHandler getConnection() {
        if (connection == null && connectionId != null) {
            connection = ConnectionRef.of(connectionId);
        }
        return ConnectionRef.get(connection);
    }

    @Nullable
    @Override
    public DBJdbcBreakpointProperties getState() {
        return this;
    }

    @Override
    public void loadState(DBJdbcBreakpointProperties state) {
        connectionId = state.connectionId;
        connection = state.connection;
    }
}
