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
import com.dbn.common.pool.ObjectCacheBase;
import com.dbn.common.thread.Background;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.ConnectionStatusListener;
import com.dbn.connection.ConnectionUtil;
import com.dbn.connection.Resources;
import com.dbn.connection.SessionId;
import com.dbn.nls.NlsSupport;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;

import static com.dbn.common.notification.NotificationSupport.sendInfoNotification;

public class DBNConnectionCache extends ObjectCacheBase<SessionId, DBNConnection, SQLException> implements NlsSupport {
    private final ConnectionRef connection;

    public DBNConnectionCache(ConnectionHandler connection) {
        super(connection);
        this.connection = ConnectionRef.of(connection);
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Override
    public boolean check(DBNConnection conn) {
        return conn != null && !conn.isClosed() && conn.isValid();
    }

    @NotNull
    @Override
    @SneakyThrows
    protected DBNConnection create(SessionId sessionId) {
        ConnectionHandler connection = getConnection();
        Project project = connection.getProject();
        try {
            DBNConnection conn = ConnectionUtil.connect(connection, sessionId);
            String connectionName = connection.getConnectionName(conn);
            sendInfoNotification(
                    project,
                    NotificationGroup.SESSION,
                    txt("ntf.connection.info.ConnectedToDatabase", connectionName));

            return conn;
        } finally {
            ProjectEvents.notify(project,
                    ConnectionStatusListener.TOPIC,
                    (listener) -> listener.statusChanged(connection.getConnectionId(), sessionId));
        }
    }

    @Override
    protected DBNConnection whenDropped(DBNConnection conn) {
        Background.run(() -> Resources.close(conn));
        return conn;
    }

    @Override
    protected DBNConnection whenReused(DBNConnection conn) {
        ConnectionHandler connection = getConnection();
        //connection.updateLastAccess(); TODO
        return conn;
    }

    @Override
    protected DBNConnection whenErrored(Throwable e) throws SQLException {
        if (e instanceof ProcessCanceledException) throw (ProcessCanceledException) e;
        throw Exceptions.toSqlException(e);
    }

    @Override
    protected DBNConnection whenNull() throws SQLException {
        throw new SQLRecoverableException("Failed to initialize connection");
    }
}
