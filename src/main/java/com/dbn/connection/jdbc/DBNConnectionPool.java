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

package com.dbn.connection.jdbc;

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.notification.NotificationGroup;
import com.dbn.common.pool.ObjectPoolBase;
import com.dbn.common.thread.Background;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionHandlerStatusHolder;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.ConnectionUtil;
import com.dbn.connection.Resources;
import com.dbn.connection.SessionId;
import com.dbn.connection.config.ConnectionConfigListener;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.dbn.common.notification.NotificationSupport.sendInfoNotification;

@Slf4j
public class DBNConnectionPool extends ObjectPoolBase<DBNConnection, SQLException> {
    private final String identifier;
    private final ConnectionRef connection;
    private final AtomicLong lastAccess = new AtomicLong();
    private int maxSize;

    public DBNConnectionPool(ConnectionHandler connection) {
        super(connection);
        this.connection = ConnectionRef.of(connection);
        this.identifier = connection.getName();
        this.maxSize = loadMaxPoolSize();

        ProjectEvents.subscribe(connection.getProject(), this,
                ConnectionConfigListener.TOPIC,
                ConnectionConfigListener.whenChanged(id -> {
                    if (id == connection.getConnectionId()) {
                        maxSize = loadMaxPoolSize();
                    }
                }));

    }

    private int loadMaxPoolSize() {
        return getConnection().getSettings().getDetailSettings().getMaxConnectionPoolSize();
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    public long getLastAccess() {
        return lastAccess.get();
    }

    public void updateLastAccess() {
        lastAccess.set(System.currentTimeMillis());
    }

    public final DBNConnection acquire(boolean readonly) throws SQLException {
        DBNConnection conn = acquire(30, TimeUnit.SECONDS);

        Resources.setAutoCommit(conn, readonly);
        Resources.setReadonly(conn, readonly);
        return conn;
    }


    @Override
    protected DBNConnection create() throws SQLException{
        checkDisposed();
        ConnectionHandler connection = getConnection();
        DBNConnection conn = ConnectionUtil.connect(connection, SessionId.POOL);

        Resources.setAutoCommit(conn, true);
        Resources.setReadonly(conn, true);

        if (size() == 0) {
            // Notify first pool connection
            String connectionName = connection.getConnectionName(conn);
            sendInfoNotification(
                    connection.getProject(),
                    NotificationGroup.SESSION,
                    txt("ntf.connection.info.ConnectedToDatabase", connectionName));
        }

        return conn;
    }

    @Override
    protected boolean check(@Nullable DBNConnection conn) {
        return conn != null && !conn.isClosed() && conn.isValid();
    }

    @Override
    public int maxSize() {
        return maxSize;
    }

    @Override
    protected String identifier() {
        return identifier;
    }

    @Override
    protected String identifier(DBNConnection conn) {
        return "Connection " + (conn == null ? "" : conn.getResourceId());
    }

    @Override
    protected DBNConnection whenNull() throws SQLException{
        throw new SQLTimeoutException("Busy connection pool");
    }

    @Override
    protected DBNConnection whenErrored(Throwable e) throws SQLException{
        throw Exceptions.toSqlException(e);
    }

    @Override
    protected DBNConnection whenAcquired(DBNConnection conn) {
        conn.set(ResourceStatus.RESERVED, true);

        ConnectionHandler connection = getConnection();
        ConnectionHandlerStatusHolder connectionStatus = connection.getConnectionStatus();
        connectionStatus.setConnected(true);
        connectionStatus.setValid(true);
        updateLastAccess();

        return conn;
    }

    @Override
    protected DBNConnection whenReleased(DBNConnection conn) throws SQLException {
        Resources.rollback(conn);
        Resources.setAutoCommit(conn, true);
        Resources.setReadonly(conn, true);

        conn.set(ResourceStatus.RESERVED, false);
        updateLastAccess();
        return conn;
    }

    @Override
    protected DBNConnection whenDropped(DBNConnection conn) {
        Background.run(null, () -> Resources.close(conn));
        return conn;
    }
}
