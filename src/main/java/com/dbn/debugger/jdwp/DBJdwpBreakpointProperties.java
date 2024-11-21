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

package com.dbn.debugger.jdwp;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.debugger.common.breakpoint.DBBreakpointProperties;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.debugger.breakpoints.properties.JavaBreakpointProperties;

public class DBJdwpBreakpointProperties extends JavaBreakpointProperties<DBJdwpBreakpointProperties> implements DBBreakpointProperties {
    @Attribute(value = "connection-id", converter = ConnectionId.Converter.class)
    private ConnectionId connectionId;
    private ConnectionRef connection;

    public DBJdwpBreakpointProperties() {
    }

    public DBJdwpBreakpointProperties(ConnectionHandler connection) {
        this.connection = ConnectionRef.of(connection);
        if (connection != null) {
            connectionId = connection.getConnectionId();
        }
    }

    @Override
    public ConnectionId getConnectionId() {
        return connectionId;
    }

    @Nullable
    @Override
    public ConnectionHandler getConnection() {
        if (connection == null && connectionId != null) {
            connection = ConnectionRef.of(connectionId);
        }
        return ConnectionRef.get(connection);
    }

    @Nullable
    @Override
    public DBJdwpBreakpointProperties getState() {
        return super.getState();
    }

    @Override
    public void loadState(@NotNull DBJdwpBreakpointProperties state) {
        super.loadState(state);
        connectionId = state.connectionId;
        connection = state.connection;
    }
}
