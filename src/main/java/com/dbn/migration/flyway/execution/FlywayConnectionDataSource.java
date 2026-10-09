/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.migration.flyway.execution;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.ResourceStatus;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * DataSource adapter that leases DBN pooled connections for Flyway.
 */
final class FlywayConnectionDataSource implements DataSource {
    private final ConnectionHandler connection;
    private final SchemaId schemaId;

    FlywayConnectionDataSource(@NotNull ConnectionHandler connection, @NotNull SchemaId schemaId) {
        this.connection = connection;
        this.schemaId = schemaId;
    }

    @Override
    public Connection getConnection() throws SQLException {
        DBNConnection leasedConnection = connection.getPoolConnection(schemaId, true);
        leasedConnection.set(ResourceStatus.ACTIVE, true);
        return createConnectionProxy(leasedConnection);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    @NotNull
    private Connection createConnectionProxy(@NotNull DBNConnection leasedConnection) {
        InvocationHandler handler = new PooledConnectionInvocationHandler(leasedConnection);
        return (Connection) Proxy.newProxyInstance(
                FlywayConnectionDataSource.class.getClassLoader(),
                new Class[]{Connection.class},
                handler);
    }

    @Override
    public PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
    }

    @Override
    public void setLoginTimeout(int seconds) {
    }

    @Override
    public int getLoginTimeout() {
        return 0;
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getGlobal();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) return iface.cast(this);
        throw new SQLException("DataSource does not wrap " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }

    private final class PooledConnectionInvocationHandler implements InvocationHandler {
        private final DBNConnection leasedConnection;
        private boolean closed;

        private PooledConnectionInvocationHandler(@NotNull DBNConnection leasedConnection) {
            this.leasedConnection = leasedConnection;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if (methodName.equals("close")) {
                close();
                return null;
            }
            if (methodName.equals("isClosed")) {
                return closed || leasedConnection.isClosed();
            }
            if (methodName.equals("unwrap") && args != null && args.length == 1) {
                Class<?> type = (Class<?>) args[0];
                if (type.isInstance(proxy)) return proxy;
                if (type.isInstance(leasedConnection)) return leasedConnection;
            }
            if (methodName.equals("isWrapperFor") && args != null && args.length == 1) {
                Class<?> type = (Class<?>) args[0];
                if (type.isInstance(proxy) || type.isInstance(leasedConnection)) return true;
            }
            if (closed) throw new SQLException("Connection is closed");

            try {
                return method.invoke(leasedConnection, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        private void close() {
            if (closed) return;
            closed = true;
            connection.freePoolConnection(leasedConnection);
            leasedConnection.set(ResourceStatus.ACTIVE, false);
        }
    }
}
