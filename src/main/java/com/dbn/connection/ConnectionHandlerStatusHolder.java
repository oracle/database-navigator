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

package com.dbn.connection;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.property.PropertyHolderBase;
import com.dbn.common.thread.Background;
import com.dbn.common.util.TimeUtil;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.IncrementalStatusAdapter;
import com.dbn.connection.jdbc.LatentResourceStatus;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Getter
@Setter
public class ConnectionHandlerStatusHolder extends PropertyHolderBase.IntStore<ConnectionHandlerStatus> {
    private final ConnectionRef connection;

    private AuthenticationError authenticationError;
    private Throwable connectionException;

    @Override
    protected ConnectionHandlerStatus[] properties() {
        return ConnectionHandlerStatus.VALUES;
    }

    private final LatentConnectionStatus active = new LatentConnectionStatus(ConnectionHandlerStatus.ACTIVE, true, TimeUtil.Millis.ONE_SECOND) {
        @Override
        protected boolean doCheck() {
            ConnectionHandler connection = getConnection();
            List<DBNConnection> connections = connection.getConnections();
            for (DBNConnection conn : connections) {
                if (conn.isActive()) {
                    return true;
                }
            }
            return false;
        }
    };

    private final LatentConnectionStatus busy = new LatentConnectionStatus(ConnectionHandlerStatus.BUSY, true, TimeUtil.Millis.ONE_SECOND) {
        @Override
        protected boolean doCheck() {
            ConnectionHandler connection = getConnection();
            List<DBNConnection> connections = connection.getConnections();
            for (DBNConnection conn : connections) {
                if (conn.hasDataChanges()) {
                    return true;
                }
            }
            return false;
        }
    };

    private final LatentConnectionStatus valid = new LatentConnectionStatus(ConnectionHandlerStatus.VALID, true, TimeUtil.Millis.THIRTY_SECONDS) {
        @Override
        protected boolean doCheck() {
            DBNConnection poolConnection = null;
            ConnectionHandler connection = getConnection();
            try {
                boolean valid = get();
                ConnectionPool connectionPool = connection.getConnectionPool();
                if (isConnected() || !valid || connectionPool.wasNeverAccessed()) {
                    poolConnection = connectionPool.allocateConnection(true);
                }
                return true;
            } catch (Exception e) {
                conditionallyLog(e);
                return false;
            } finally {
                connection.freePoolConnection(poolConnection);
            }
        }
    };

    private final LatentConnectionStatus connected = new LatentConnectionStatus(ConnectionHandlerStatus.CONNECTED, false, TimeUtil.Millis.TEN_SECONDS) {
        @Override
        protected boolean doCheck() {
            try {
                ConnectionHandler connection = getConnection();
                List<DBNConnection> connections = connection.getConnections();
                for (DBNConnection conn : connections) {
                    if (conn != null && !conn.isActive() && !conn.isClosed() && conn.isValid()) {
                        return true;
                    }
                }
            } catch (Exception e) {
                conditionallyLog(e);
                return false;
            }
            return false;
        }
    };

    private final IncrementalStatusAdapter<ConnectionHandlerStatusHolder, ConnectionHandlerStatus> loading =
            new IncrementalStatusAdapter<>(this, ConnectionHandlerStatus.LOADING) {
                @Override
                protected boolean setInner(ConnectionHandlerStatus status, boolean value) {
                    return ConnectionHandlerStatusHolder.super.set(status, value);
                }

                @Override
                protected void statusChanged() {
                    ConnectionHandler connection = Failsafe.nn(getConnection());
                    Project project = connection.getProject();
                    ProjectEvents.notify(project,
                            ConnectionLoadListener.TOPIC,
                            (listener) -> Background.run(project, () -> listener.contentsLoaded(connection)));
                }
            };

    ConnectionHandlerStatusHolder(@NotNull ConnectionHandler connection) {
        this.connection = connection.ref();
    }

    @NotNull
    private ConnectionHandler getConnection() {
        return connection.ensure();
    }

    private boolean canConnect() {
        return getConnection().canConnect();
    }


    public void setValid(boolean valid) {
        this.valid.set(valid);
    }

    public void setConnected(boolean connected) {
        this.connected.set(connected);
    }

    public boolean isConnected() {
        if (isActive()) {
            return true;
        } else {
            return canConnect() ?
                    connected.check() :
                    connected.get();
        }
    }

    public boolean isValid() {
        if (isActive()) {
            return true;
        } else {
            return canConnect() ?
                    valid.check() :
                    valid.get();
        }
    }

    public String getStatusMessage() {
        return connectionException == null ? null : connectionException.getMessage();
    }

    public boolean isBusy() {
        return busy.check();
    }

    public boolean isActive() {
        return active.check();
    }

    public abstract class LatentConnectionStatus extends LatentResourceStatus<ConnectionHandlerStatus> {
        LatentConnectionStatus(ConnectionHandlerStatus status, boolean initialValue, long interval) {
            super(ConnectionHandlerStatusHolder.this, status, initialValue, interval);
        }

        @Override
        public final void statusChanged(ConnectionHandlerStatus status) {
            ConnectionHandler connection = getConnection();
            Project project = connection.getProject();
            ProjectEvents.notify(project,
                    ConnectionHandlerStatusListener.TOPIC,
                    (listener) -> listener.statusChanged(connection.getConnectionId()));
        }
    }
}
