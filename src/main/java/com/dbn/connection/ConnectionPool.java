package com.dbn.connection;

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.notification.NotificationSupport;
import com.dbn.common.thread.Background;
import com.dbn.connection.config.ConnectionDetailSettings;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNConnectionCache;
import com.dbn.connection.jdbc.DBNConnectionPool;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.dbn.common.util.TimeUtil.isOlderThan;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
public final class ConnectionPool extends StatefulDisposableBase implements NotificationSupport, Disposable {
    static {
        ConnectionPoolCleaner.INSTANCE.start();
    }

    private final ConnectionRef connection;
    private final @Getter(lazy = true) DBNConnectionPool connectionPool = new DBNConnectionPool(getConnection());
    private final @Getter(lazy = true) DBNConnectionCache connectionCache = new DBNConnectionCache(getConnection());

    ConnectionPool(@NotNull ConnectionHandler connection) {
        super(connection);
        this.connection = connection.ref();
    }

    DBNConnection ensureTestConnection() throws SQLException {
        return ensureConnection(SessionId.TEST);
    }

    @NotNull
    DBNConnection ensureMainConnection() throws SQLException {
        return ensureConnection(SessionId.MAIN);
    }

    @NotNull
    DBNConnection ensureDebugConnection() throws SQLException {
        return ensureConnection(SessionId.DEBUG);
    }

    @NotNull
    DBNConnection ensureDebuggerConnection() throws SQLException {
        return ensureConnection(SessionId.DEBUGGER);
    }

    @NotNull
    DBNConnection ensureAssistantConnection() throws SQLException {
        return ensureConnection(SessionId.ASSISTANT);
    }
    @Nullable
    public DBNConnection getMainConnection() {
        return getConnectionCache().get(SessionId.MAIN);
    }

    @Nullable
    public DBNConnection getTestConnection() {
        return getConnectionCache().get(SessionId.TEST);
    }

    @Nullable
    public DBNConnection getSessionConnection(SessionId sessionId) {
        return getConnectionCache().get(sessionId);
    }

    @NotNull
    DBNConnection ensureSessionConnection(SessionId sessionId) throws SQLException {
        return ensureConnection(sessionId);
    }

    @NotNull
    public List<DBNConnection> getConnections(ConnectionType... connectionTypes) {
        List<DBNConnection> connections = new ArrayList<>();
        if (ConnectionType.POOL.matches(connectionTypes)) {
            getConnectionPool().visit(c -> connections.add(c));
        }

        getConnectionCache().visit(
                c -> c.getType().matches(connectionTypes),
                c -> connections.add(c));
        return connections;
    }

    @NotNull
    private DBNConnection ensureConnection(SessionId sessionId) throws SQLException {
        ConnectionHandler connection = getConnection();
        ConnectionManager.setLastUsedConnection(connection);
        return getConnectionCache().ensure(sessionId);
    }

    public void updateLastAccess() {
        getConnectionPool().updateLastAccess();
    }

    public long getLastAccess() {
        return getConnectionPool().getLastAccess();
    }

    boolean wasNeverAccessed() {
        return getLastAccess() == 0;
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Override
    @NotNull
    public Project getProject() {
        return getConnection().getProject();
    }

    @NotNull
    DBNConnection allocateConnection(boolean readonly) throws SQLException {
        return getConnectionPool().acquire(readonly);
    }

    void releaseConnection(@Nullable DBNConnection connection) {
        if (connection == null) return;

        if (connection.isPoolConnection()) {
            getConnectionPool().release(connection);
        } else {
            log.error("Trying to release non-POOL connection: " + connection.getType(), new IllegalArgumentException("No POOL connection"));
        }

    }

    void closeConnections() {
        List<DBNConnection> connections = getConnections();
        for (DBNConnection connection : connections) {
            closeConnection(connection);
        }
    }

    void closeConnection(DBNConnection connection) {
        SessionId sessionId = connection.getSessionId();
        if (sessionId == SessionId.POOL) {
            getConnectionPool().drop(connection);
        } else {
            getConnectionCache().drop(sessionId);
        }
    }

    public int getSize() {
        return getConnectionPool().size();
    }

    public int getPeakPoolSize() {
        return getConnectionPool().peakSize();
    }

    @Override
    public void disposeInner() {
        Background.run(null, () -> closeConnections());
    }

    public boolean isConnected(SessionId sessionId) {

        if (sessionId == SessionId.POOL) {
            return getConnectionPool().size() > 0;
        }

        if (sessionId != null) {
            DBNConnection connection = getConnectionCache().get(sessionId);
            return connection != null && !connection.isClosed() && connection.isValid();
        }
        return false;
    }

    void clean() {
        if (getConnectionPool().isEmpty()) return;
        try {
            ConnectionHandler connection = getConnection();
            ConnectionHandlerStatusHolder status = connection.getConnectionStatus();
            if (status.is(ConnectionHandlerStatus.CLEANING)) return;

            try {
                status.set(ConnectionHandlerStatus.CLEANING, true);

                long lastAccess = getLastAccess();
                ConnectionDetailSettings detailSettings = connection.getSettings().getDetailSettings();
                int minutesToDisconnect = detailSettings.getIdleMinutesToDisconnectPool();
                if (lastAccess > 0 && isOlderThan(lastAccess, minutesToDisconnect, TimeUnit.MINUTES)) {
                    getConnectionPool().clean(conn -> !conn.isActive() && !conn.isReserved());
                }

            } finally {
                status.set(ConnectionHandlerStatus.CLEANING, false);
            }
        } catch (ProcessCanceledException e) {
            conditionallyLog(e);
        } catch (Exception e) {
            conditionallyLog(e);
            log.error("Failed to clean connection pool", e);
        }
    }
}
