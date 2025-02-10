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

import com.dbn.common.compatibility.Exploitable;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.routine.Consumer;
import com.dbn.common.util.TimeUtil;
import com.dbn.common.util.Unsafe;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionHandlerStatusHolder;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionProperties;
import com.dbn.connection.ConnectionStatusListener;
import com.dbn.connection.ConnectionType;
import com.dbn.connection.Resources;
import com.dbn.connection.SchemaId;
import com.dbn.connection.SessionId;
import com.dbn.connection.transaction.PendingTransactionBundle;
import com.dbn.database.interfaces.DatabaseInterface.Callable;
import com.dbn.database.interfaces.DatabaseInterface.Runnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.connection.jdbc.ResourceStatus.ACTIVE;
import static com.dbn.connection.jdbc.ResourceStatus.CLOSED;
import static com.dbn.connection.jdbc.ResourceStatus.RESERVED;
import static com.dbn.connection.jdbc.ResourceStatus.VALID;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@Getter
@Setter
public class DBNConnection extends DBNConnectionBase {
    private final ProjectRef project;
    private final String name;
    private final ConnectionType type;
    private final SessionId sessionId;
    private final ConnectionProperties properties;

    private long lastAccess = System.currentTimeMillis();
    private PendingTransactionBundle dataChanges;
    private SchemaId currentSchema;

    private final Set<DBNStatement> activeStatements = ConcurrentHashMap.newKeySet();
    private final Set<DBNResultSet> activeCursors = ConcurrentHashMap.newKeySet();
    private final Map<String, DBNPreparedStatement> cachedStatements = new ConcurrentHashMap<>();
    private transient DBNStatement enquoteStatement;

    private final IncrementalResourceStatusAdapter<DBNConnection> active =
            IncrementalResourceStatusAdapter.create(
                    DBNConnection.this,
                    ResourceStatus.ACTIVE,
                    (status, value) -> DBNConnection.super.set(status, value));

    private final IncrementalResourceStatusAdapter<DBNConnection> reserved =
            IncrementalResourceStatusAdapter.create(
                    DBNConnection.this,
                    ResourceStatus.RESERVED,
                    (status, value) -> DBNConnection.super.set(status, value));

    private final ResourceStatusAdapter<DBNConnection> valid =
            new ResourceStatusAdapterImpl<>(this,
                    ResourceStatus.VALID,
                    ResourceStatus.CHANGING_VALID,
                    ResourceStatus.EVALUATING_VALID,
                    TimeUtil.Millis.TEN_SECONDS,
                    Boolean.TRUE,
                    Boolean.FALSE) { // false is terminal status
                @Override
                protected void changeInner(boolean value) {
                }

                @Override
                protected boolean checkInner() throws SQLException {
                    return isActive() || inner.isValid(2);
                }
            };

    private final ResourceStatusAdapter<DBNConnection> autoCommit =
            new ResourceStatusAdapterImpl<>(this,
                    ResourceStatus.AUTO_COMMIT,
                    ResourceStatus.CHANGING_AUTO_COMMIT,
                    ResourceStatus.EVALUATING_AUTO_COMMIT,
                    TimeUtil.Millis.TEN_SECONDS,
                    Boolean.FALSE,
                    null) { // no terminal status
                @Override
                protected void changeInner(boolean value) {
                    try {
                        try {
                            inner.setAutoCommit(value);
                        } catch (SQLException e) {
                            inner.setAutoCommit(value);
                        }
                    } catch (Throwable e) {
                        conditionallyLog(e);
                        log.warn("Unable to set auto-commit to " + value + ". Maybe your database does not support transactions...", e);
                    }
                }

                @Override
                protected boolean checkInner() throws SQLException {
                    return inner.getAutoCommit();
                }
            };

    private final ResourceStatusAdapter<DBNConnection> readOnly =
            new ResourceStatusAdapterImpl<>(this,
                    ResourceStatus.READ_ONLY,
                    ResourceStatus.CHANGING_READ_ONLY,
                    ResourceStatus.EVALUATING_READ_ONLY,
                    TimeUtil.Millis.TEN_SECONDS,
                    Boolean.FALSE,
                    null) { // no terminal status
                @Override
                protected void changeInner(boolean value) {
                    try {
                        try {
                            inner.setReadOnly(value);
                        } catch (SQLException e) {
                            inner.setReadOnly(value);
                        }
                    } catch (Throwable e) {
                        conditionallyLog(e);
                        log.warn("Unable to set read-only status to " + value + ". Maybe your database does not support transactions...", e);
                    }
                }

                @Override
                protected boolean checkInner() throws SQLException {
                    return inner.isReadOnly();
                }
            };

    private DBNConnection(Project project, Connection connection, String name, ConnectionType type, ConnectionId id, SessionId sessionId) throws SQLException {
        super(connection, id);
        this.project = ProjectRef.of(project);
        this.name = name;
        this.type = type;
        this.sessionId = sessionId;
        this.properties = new ConnectionProperties(connection);
    }

    public static DBNConnection wrap(Project project, Connection connection, String name, ConnectionType type, ConnectionId id, SessionId sessionId) throws SQLException {
        return new DBNConnection(project, connection, name, type, id, sessionId);
    }


    @Exploitable
    public DBNPreparedStatement prepareStatementCached(String sql) {
        return cachedStatements.computeIfAbsent(sql, s -> {
            DBNPreparedStatement statement = prepareStatement(s);
            statement.setCached(true);
            statement.setFetchSize(500);
            statement.setSql(s);
            return statement;
        });
    }

    @Exploitable
    public DBNCallableStatement prepareCallCached(String sql) {
        return cast(cachedStatements.computeIfAbsent(sql, s -> {
            DBNPreparedStatement statement = prepareCall(s);
            statement.setCached(true);
            statement.setFetchSize(500);
            statement.setSql(s);
            return statement;
        }));
    }

    @Override
    protected <S extends Statement> S wrap(Statement statement) {
        return wrap(statement, null);
    }

    @Override
    protected <S extends Statement> S wrap(Statement statement, String sql) {
        updateLastAccess();
        if (statement instanceof CallableStatement) {
            CallableStatement callableStatement = (CallableStatement) statement;
            statement = new DBNCallableStatement(callableStatement, this);

        } else  if (statement instanceof PreparedStatement) {
            PreparedStatement preparedStatement = (PreparedStatement) statement;
            statement = new DBNPreparedStatement(preparedStatement, this);

        } else {
            statement = new DBNStatement<>(statement, this);
        }

        DBNStatement dbnStatement = (DBNStatement) statement;
        dbnStatement.setSql(sql);
        activeStatements.add(dbnStatement);
        return cast(statement);
    }

    protected void park(DBNStatement statement) {
        activeStatements.remove(statement);
        updateLastAccess();
    }

    protected void release(DBNStatement statement) {
        park(statement);
        if (statement.isCached() && statement instanceof DBNPreparedStatement) {
            DBNPreparedStatement preparedStatement = (DBNPreparedStatement) statement;
            cachedStatements.values().removeIf(v -> v == preparedStatement);
        }

        updateLastAccess();
    }

    protected void release(DBNResultSet resultSet) {
        activeCursors.remove(resultSet);
        updateLastAccess();
    }

    public int getActiveStatementCount() {
        return activeStatements.size();
    }

    public int getCachedStatementCount() {
        return cachedStatements.size();
    }

    public int getActiveCursorCount() {
        return activeCursors.size();
    }

    @Override
    public boolean isClosedInner() throws SQLException {
        return inner.isClosed();
    }

    @Override
    public boolean isClosed() {
        // skip checking "closed" on active connections
        return !isActive() && super.isClosed();
    }

    @Override
    public void closeInner() throws SQLException {
        try {
            if (!isAutoCommit()) {
                inner.rollback();
            }
        } finally {
            inner.close();
        }
    }

    public boolean isPoolConnection() {
        return type == ConnectionType.POOL;
    }

    public boolean isDebugConnection() {
        return type == ConnectionType.DEBUG;
    }

    public boolean isDebuggerConnection() {
        return type == ConnectionType.DEBUGGER;
    }

    public boolean isMainConnection() {
        return type == ConnectionType.MAIN;
    }

    public boolean isTestConnection() {
        return type == ConnectionType.TEST;
    }

    @Override
    public void statusChanged(ResourceStatus status) {
        propagate(connection -> {
            ConnectionHandlerStatusHolder connectionStatus = connection.getConnectionStatus();
            switch (status) {
                case CLOSED: connectionStatus.getConnected().markDirty(); break;
                case VALID: connectionStatus.getValid().markDirty(); break;
                case ACTIVE: connectionStatus.getActive().markDirty(); break;
            }
        });
    }

    public void updateLastAccess() {
        lastAccess = System.currentTimeMillis();
        propagate(connection -> connection.getConnectionPool().updateLastAccess());
    }

    private void propagate(Consumer<ConnectionHandler> consumer) {
        ConnectionHandler connection = getConnectionHandler();
        if (isNotValid(connection)) return;

        consumer.accept(connection);
    }

    @Nullable
    public ConnectionHandler getConnectionHandler() {
        ConnectionId id = getConnectionId();
        if (id == null) return null; // not yet initialised
        return ConnectionHandler.get(id);
    }

    public int getIdleMinutes() {
        long idleTimeMillis = System.currentTimeMillis() - lastAccess;
        return (int) (idleTimeMillis / TimeUtil.Millis.ONE_MINUTE);
    }

    public static Connection getInner(Connection connection) {
        if (connection instanceof DBNConnection) {
            DBNConnection dbnConnection = (DBNConnection) connection;
            return dbnConnection.getInner();
        }
        return connection;
    }

    @NotNull
    public Project getProject() {
        return project.ensure();
    }

    public String enquoteIdentifier(String identifier) throws SQLException {
        if (enquoteStatement == null) enquoteStatement = createStatement();
        return enquoteStatement.enquoteIdentifier(identifier, true);
    }

    /********************************************************************
     *                        Transaction                               *
     ********************************************************************/
    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        this.autoCommit.set(autoCommit);
    }

    @Override
    public boolean getAutoCommit() {
        return autoCommit.get();
    }

    public boolean isAutoCommit() {
        return getAutoCommit();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        this.readOnly.set(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return this.readOnly.get();
    }

    @Override
    public void commit() throws SQLException {
        updateLastAccess();

        super.commit();
        resetDataChanges();
        notifyStatusChange();
    }

    @Override
    public void rollback() throws SQLException {
        updateLastAccess();

        super.rollback();
        resetDataChanges();
        notifyStatusChange();
    }

    @Override
    public void close() throws SQLException {
        try {
            super.close();
            List<DBNPreparedStatement> statements = new ArrayList<>(cachedStatements.values());
            cachedStatements.clear();
            Resources.close(statements);
        } finally {
            updateLastAccess();
            resetDataChanges();
            notifyStatusChange();
        }
    }

    @NotNull
    @Override
    public DBNConnection getConnection() {
        return this;
    }

    private void notifyStatusChange() {
        ProjectEvents.notify(getProject(),
                ConnectionStatusListener.TOPIC,
                l -> l.statusChanged(getConnectionId(), sessionId));
    }

    /********************************************************************
     *                             Status                               *
     ********************************************************************/
    public boolean isIdle() {
        return !isActive() && !isReserved();
    }

    public boolean isReserved() {
        return is(RESERVED);
    }

    public boolean isActive() {
        return is(ACTIVE);
    }


    public boolean isValid() {
        return isValid(2);
    }

    public void reevaluateStatus() {
        Unsafe.warned(() -> {
            if (inner.isClosed() || !inner.isValid(5)) {
                set(VALID, false);
                set(CLOSED, true);
                set(ACTIVE, false);
            }
        });
    }


    @Override
    public boolean isValid(int timeout) {
        return valid.get();
    }

    @Override
    public boolean set(ResourceStatus status, boolean value) {
        boolean changed;
        if (status == ACTIVE) {
            changed = active.set(value);

        } else if (status == RESERVED) {
            if (value) {
                if (isActive()) {
                    log.warn("Reserving active connection");
                } else if (isReserved()) {
                    log.warn("Reserving already reserved connection");
                }
            }
            changed = reserved.set(value);
        } else {
            changed = super.set(status, value);
            if (changed) statusChanged(status);
        }


        return changed;
    }


    public <T> T withSavepoint(Callable<T> callable) throws SQLException{
        Savepoint savepoint = Resources.createSavepoint(this);
        try {
            return callable.call();
        } catch (SQLException e) {
            conditionallyLog(e);
            Resources.rollbackSilently(this, savepoint);
            throw e;
        } finally {
            Resources.releaseSavepoint(this, savepoint);
        }
    }

    public void withSavepoint(Runnable runnable) throws SQLException{
        Savepoint savepoint = Resources.createSavepoint(this);
        try {
            runnable.run();
        } catch (SQLException e) {
            conditionallyLog(e);
            Resources.rollbackSilently(this, savepoint);
            throw e;
        } finally {
            Resources.releaseSavepoint(this, savepoint);
        }
    }

    /********************************************************************
     *                             Data changes                         *
     ********************************************************************/
    public void notifyDataChanges(@NotNull VirtualFile virtualFile) {
        if (!getAutoCommit()) {
            if (dataChanges == null) {
                dataChanges = new PendingTransactionBundle();
            }
            dataChanges.notifyChange(virtualFile, this);
        }
    }

    public void resetDataChanges() {
        dataChanges = null;
    }

    public boolean hasDataChanges() {
        return dataChanges != null && !dataChanges.isEmpty() && !isClosed();
    }
}
