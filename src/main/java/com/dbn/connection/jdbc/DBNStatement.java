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
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.routine.ThrowableCallable;
import com.dbn.common.util.Unsafe;
import com.dbn.connection.Resources;
import com.dbn.diagnostics.Diagnostics;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;

import static com.dbn.connection.jdbc.ResourceStatus.ACTIVE;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static java.lang.Math.min;

@Getter
@Setter
public class DBNStatement<T extends Statement> extends DBNResource<T> implements Statement, CloseableResource, CancellableResource {
    private final WeakRef<DBNConnection> connection;
    private WeakRef<DBNResultSet> resultSet;

    /** last execution time. -1 unknown */
    private final AtomicLong executeDuration = new AtomicLong(-1);

    private boolean cached;
    private String sql;

    DBNStatement(T inner, DBNConnection connection) {
        super(inner, ResourceType.STATEMENT, connection.getConnectionId());
        this.connection = WeakRef.of(connection);
    }

    @Nullable
    @Override
    public DBNConnection getConnection() {
        return connection.get();
    }

    @NotNull
    public DBNConnection ensureConnection() {
        return Failsafe.nn(getConnection());
    }

    @Override
    public boolean isObsolete() {
        if (isClosed()) return true;

        DBNConnection connection = getConnection();
        if (connection == null) return true;
        if (connection.isClosed()) return true;

        return false;
    }

    @Override
    public boolean isCancelledInner() throws SQLException {
        return false;
    }

    @Override
    public void cancelInner() throws SQLException {
        inner.cancel();
    }

    @Override
    public boolean isClosedInner() throws SQLException {
        return inner.isClosed();
    }

    @Override
    public void closeInner() throws SQLException {
        inner.close();
    }


    @Override
    public void close() throws SQLException {
        try {
            super.close();
        } finally {
            DBNConnection connection = getConnection();
            if (connection != null) {
                connection.release(this);
            }
        }
    }

    public void park() {
        DBNConnection connection = getConnection();
        if (connection != null) {
            connection.park(this);
        }
    }

    protected DBNResultSet wrap(ResultSet original) throws SQLException {
        if (original == null) {
            resultSet = null;
        } else {
            if (resultSet == null) {
                DBNResultSet resultSet = new DBNResultSet(original, this);
                this.resultSet = WeakRef.of(resultSet);
            } else {
                DBNResultSet resultSet = this.resultSet.get();
                if (resultSet == null || resultSet.inner != original) {
                    resultSet = new DBNResultSet(original, this);
                    this.resultSet = WeakRef.of(resultSet);

                }
            }
        }
        return WeakRef.get(resultSet);
    }

    public long getExecuteDuration() {
        return executeDuration.get();
    }

    protected Object wrap(Object object) {
        if (object instanceof ResultSet) {
            ResultSet resultSet = (ResultSet) object;
            return new DBNResultSet(resultSet, ensureConnection());
        }
        return object;
    }

    protected <R> R managed(ThrowableCallable<R, SQLException> callable) throws SQLException {
        DBNConnection connection = ensureConnection();
        connection.updateLastAccess();
        try {
            connection.set(ACTIVE, true);
            executeDuration.set(-1);
            long init = System.currentTimeMillis();
            try {
                return callable.call();
            } finally {
                executeDuration.set(System.currentTimeMillis() - init);
            }
        } catch (SQLRecoverableException e) {
            conditionallyLog(e);
            Resources.markClosed(connection);
            throw e;
        } catch (SQLException e) {
            conditionallyLog(e);
            Resources.close(DBNStatement.this);
            connection.reevaluateStatus();
            throw e;
        } finally {
            connection.updateLastAccess();
            connection.set(ACTIVE, false);
        }
    }

    /********************************************************************
     *                     Wrapped executions                           *
     ********************************************************************/
    @Override
    @Exploitable
    public boolean execute(String sql) throws SQLException {
        return managed(() -> inner.execute(sql));
    }

    @Override
    @Exploitable
    public DBNResultSet executeQuery(String sql) throws SQLException {
        return managed(() -> wrap(inner.executeQuery(sql)));
    }

    @Override
    @Exploitable
    public int executeUpdate(String sql) throws SQLException {
        return managed(() -> inner.executeUpdate(sql));
    }

    @Override
    @Exploitable
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return managed(() -> inner.executeUpdate(sql, autoGeneratedKeys));
    }

    @Override
    @Exploitable
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return managed(() -> inner.executeUpdate(sql, columnIndexes));
    }

    @Override
    @Exploitable
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return managed(() -> inner.executeUpdate(sql, columnNames));
    }

    @Override
    @Exploitable
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return managed(() -> inner.execute(sql, autoGeneratedKeys));
    }

    @Override
    @Exploitable
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return managed(() -> inner.execute(sql, columnIndexes));
    }

    @Override
    @Exploitable
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return managed(() -> inner.execute(sql, columnNames));
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return managed(() -> inner.executeBatch());
    }


    /********************************************************************
     *                     Wrapped functionality                        *
     ********************************************************************/
    @Override
    public int getMaxFieldSize() throws SQLException {
        return inner.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        inner.setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return inner.getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        inner.setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        inner.setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return inner.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        try {
            seconds = Diagnostics.timeoutAdjustment(seconds);
            inner.setQueryTimeout(seconds);
        } catch (Throwable e) {
            conditionallyLog(e);
            // catch throwable (capture e.g. java.lang.AbstractMethodError)
            // not all databases support it, as this is used on DBN start connection, we must control exception
        }
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return inner.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        inner.clearWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        inner.setCursorName(name);
    }

    @Override
    public DBNResultSet getResultSet() throws SQLException {
        return wrap(inner.getResultSet());
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return inner.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return inner.getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        inner.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return inner.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) {
        // do not allow more than 50 record blocks to avoid network packet-size limits (socket closed exceptions)
        Unsafe.silent(this, s -> s.inner.setFetchSize(min(rows, 50)));
    }

    @Override
    public int getFetchSize() throws SQLException {
        return inner.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return inner.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return inner.getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        inner.addBatch(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        inner.clearBatch();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return inner.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return inner.getGeneratedKeys();
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return inner.getResultSetHoldability();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        inner.setPoolable(poolable);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return inner.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        inner.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return inner.isCloseOnCompletion();
    }


    @Override
    public String enquoteLiteral(String val) throws SQLException {
        return inner.enquoteLiteral(val);
    }

    @Override
    public String enquoteIdentifier(String identifier, boolean alwaysQuote) throws SQLException {
        return inner.enquoteIdentifier(identifier, alwaysQuote);
    }

    @Override
    public boolean isSimpleIdentifier(String identifier) throws SQLException {
        return inner.isSimpleIdentifier(identifier);
    }

    @Override
    public String enquoteNCharLiteral(String val) throws SQLException {
        return inner.enquoteNCharLiteral(val);
    }

    @Override
    public long getLargeUpdateCount() throws SQLException {
        return inner.getLargeUpdateCount();
    }

    @Override
    public void setLargeMaxRows(long max) throws SQLException {
        inner.setLargeMaxRows(max);
    }

    @Override
    public long getLargeMaxRows() throws SQLException {
        return inner.getLargeMaxRows();
    }

    @Override
    public long[] executeLargeBatch() throws SQLException {
        return inner.executeLargeBatch();
    }

    @Override
    public long executeLargeUpdate(String sql) throws SQLException {
        return inner.executeLargeUpdate(sql);
    }

    @Override
    public long executeLargeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return inner.executeLargeUpdate(sql, autoGeneratedKeys);
    }

    @Override
    public long executeLargeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return inner.executeLargeUpdate(sql, columnIndexes);
    }

    @Override
    public long executeLargeUpdate(String sql, String[] columnNames) throws SQLException {
        return inner.executeLargeUpdate(sql, columnNames);
    }


    @Override
    public <W> W unwrap(Class<W> iface) throws SQLException {
        return inner.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return inner.isWrapperFor(iface);
    }

}
