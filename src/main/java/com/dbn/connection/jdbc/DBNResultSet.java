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

import com.dbn.common.ref.WeakRef;
import com.dbn.common.routine.ThrowableCallable;
import com.dbn.common.routine.ThrowableRunnable;
import com.dbn.connection.Resources;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static java.lang.Math.min;

public class DBNResultSet extends DBNResource<ResultSet> implements ResultSet, CloseableResource {
    private WeakRef<DBNStatement> statement;
    private WeakRef<DBNConnection> connection;
    private ResultSetMetaData metaData;

    @Getter
    @Setter
    private String identifier;


    public DBNResultSet(ResultSet inner, DBNConnection connection) {
        super(inner, ResourceType.RESULT_SET);
        this.connection = WeakRef.of(connection);
    }

    public DBNResultSet(ResultSet inner, DBNStatement statement) {
        super(inner, ResourceType.RESULT_SET);
        this.statement = WeakRef.of(statement);
    }

    public static String getIdentifier(ResultSet resultSet) {
        if (resultSet instanceof DBNResultSet) {
            DBNResultSet dbnResultSet = (DBNResultSet) resultSet;
            return dbnResultSet.getIdentifier();
        }
        return "UNKNOWN";
    }

    public static boolean setIdentifier(ResultSet resultSet, String identifier) {
        if (resultSet instanceof DBNResultSet) {
            DBNResultSet dbnResultSet = (DBNResultSet) resultSet;
            dbnResultSet.setIdentifier(identifier);
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public DBNStatement getStatement() {
        return WeakRef.get(statement);
    }

    @Nullable
    public DBNConnection getConnection() {
        DBNConnection connection = WeakRef.get(this.connection);
        if (connection != null) return connection;

        DBNStatement statement = getStatement();
        if (statement != null) return statement.getConnection();

        return null;
    }

    @Override
    public void close() throws SQLException {
        try {
            super.close();
        } finally {
            DBNStatement statement = getStatement();
            if (statement != null) {
                if (statement.isCached()) {
                    statement.park();
                } else {
                    statement.close();
                }
            }
        }
    }

    public void release() {
        DBNConnection connection = getConnection();
        if (connection != null) {
            connection.release(this);
        }
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
    public boolean isClosedInner() throws SQLException {
        return Resources.isClosed(inner);
    }

    @Override
    public void closeInner() throws SQLException {
        handled(() -> inner.close());
    }

    public static ResultSet getInner(ResultSet resultSet) {
        if (resultSet instanceof DBNResultSet) {
            DBNResultSet dbnResultSet = (DBNResultSet) resultSet;
            return dbnResultSet.getInner();
        }
        return resultSet;
    }

    /********************************************************************
     *                     Wrapped functionality                        *
     ********************************************************************/
    @Override
    public boolean next() throws SQLException {
        return handled(() -> inner.next());
    }

    @Override
    public boolean wasNull() throws SQLException {
        return handled(() -> inner.wasNull());
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        return handled(() -> inner.getString(columnIndex));
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        return handled(() -> inner.getBoolean(columnIndex));
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        return handled(() -> inner.getByte(columnIndex));
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        return handled(() -> inner.getShort(columnIndex));
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        return handled(() -> inner.getInt(columnIndex));
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        return handled(() -> inner.getLong(columnIndex));
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        return handled(() -> inner.getFloat(columnIndex));
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        return handled(() -> inner.getDouble(columnIndex));
    }

    @Override
    @Deprecated
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        return handled(() -> inner.getBigDecimal(columnIndex, scale));
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        return handled(() -> inner.getBytes(columnIndex));
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        return handled(() -> inner.getDate(columnIndex));
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        return handled(() -> inner.getTime(columnIndex));
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        return handled(() -> inner.getTimestamp(columnIndex));
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        return inner.getAsciiStream(columnIndex);
    }

    @Override
    @Deprecated
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        return handled(() -> inner.getUnicodeStream(columnIndex));
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        return handled(() -> inner.getBinaryStream(columnIndex));
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        return handled(() -> inner.getString(columnLabel));
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        return handled(() -> inner.getBoolean(columnLabel));
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        return handled(() -> inner.getByte(columnLabel));
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        return handled(() -> inner.getShort(columnLabel));
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        return handled(() -> inner.getInt(columnLabel));
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        return handled(() -> inner.getLong(columnLabel));
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        return handled(() -> inner.getFloat(columnLabel));
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        return handled(() -> inner.getDouble(columnLabel));
    }

    @Override
    @Deprecated
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        return handled(() -> inner.getBigDecimal(columnLabel, scale));
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        return handled(() -> inner.getBytes(columnLabel));
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        return handled(() -> inner.getDate(columnLabel));
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        return handled(() -> inner.getTime(columnLabel));
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        return handled(() -> inner.getTimestamp(columnLabel));
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        return handled(() -> inner.getAsciiStream(columnLabel));
    }

    @Override
    @Deprecated
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        return handled(() -> inner.getUnicodeStream(columnLabel));
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        return handled(() -> inner.getBinaryStream(columnLabel));
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return handled(() -> inner.getWarnings());
    }

    @Override
    public void clearWarnings() throws SQLException {
        handled(() -> inner.clearWarnings());
    }

    @Override
    public String getCursorName() throws SQLException {
        return handled(() -> inner.getCursorName());
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return handled(() -> {
            if (metaData == null) {
                metaData = inner.getMetaData();
            }
            return metaData;
        });
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        return handled(() -> inner.getObject(columnIndex));
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return handled(() -> inner.getObject(columnLabel));
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        return handled(() -> inner.findColumn(columnLabel));
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        return handled(() -> inner.getCharacterStream(columnIndex));
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        return handled(() -> inner.getCharacterStream(columnLabel));
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        return handled(() -> inner.getBigDecimal(columnIndex));
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        return handled(() -> inner.getBigDecimal(columnLabel));
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return handled(() -> inner.isBeforeFirst());
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return handled(() -> inner.isAfterLast());
    }

    @Override
    public boolean isFirst() throws SQLException {
        return handled(() -> inner.isFirst());
    }

    @Override
    public boolean isLast() throws SQLException {
        return handled(() -> inner.isLast());
    }

    @Override
    public void beforeFirst() throws SQLException {
        handled(() -> inner.beforeFirst());
    }

    @Override
    public void afterLast() throws SQLException {
        handled(() -> inner.afterLast());
    }

    @Override
    public boolean first() throws SQLException {
        return handled(() -> inner.first());
    }

    @Override
    public boolean last() throws SQLException {
        return handled(() -> inner.last());
    }

    @Override
    public int getRow() throws SQLException {
        return handled(() -> inner.getRow());
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        return handled(() -> inner.absolute(row));
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        return handled(() -> inner.relative(rows));
    }

    @Override
    public boolean previous() throws SQLException {
        return handled(() -> inner.previous());
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        handled(() -> inner.setFetchDirection(direction));
    }

    @Override
    @SuppressWarnings("MagicConstant")
    public int getFetchDirection() throws SQLException {
        return handled(() -> inner.getFetchDirection());
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        // do not allow more than 50 record blocks to avoid network packet-size limits (socket closed exceptions)
        handled(() -> inner.setFetchSize(min(rows, 50)));
    }

    @Override
    public int getFetchSize() throws SQLException {
        return handled(() -> inner.getFetchSize());
    }

    @Override
    @SuppressWarnings("MagicConstant")
    public int getType() throws SQLException {
        return handled(() -> inner.getType());
    }

    @Override
    @SuppressWarnings("MagicConstant")
    public int getConcurrency() throws SQLException {
        return handled(() -> inner.getConcurrency());
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        return handled(() -> inner.rowUpdated());
    }

    @Override
    public boolean rowInserted() throws SQLException {
        return handled(() -> inner.rowInserted());
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        return handled(() -> inner.rowDeleted());
    }

    @Override
    public void updateNull(int columnIndex) throws SQLException {
        handled(() -> inner.updateNull(columnIndex));
    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        handled(() -> inner.updateBoolean(columnIndex, x));
    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        handled(() -> inner.updateByte(columnIndex, x));
    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        handled(() -> inner.updateShort(columnIndex, x));
    }

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        handled(() -> inner.updateInt(columnIndex, x));
    }

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        handled(() -> inner.updateLong(columnIndex, x));
    }

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        handled(() -> inner.updateFloat(columnIndex, x));
    }

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        handled(() -> inner.updateDouble(columnIndex, x));
    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        handled(() -> inner.updateBigDecimal(columnIndex, x));
    }

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        handled(() -> inner.updateString(columnIndex, x));
    }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        handled(() -> inner.updateBytes(columnIndex, x));
    }

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        handled(() -> inner.updateDate(columnIndex, x));
    }

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        handled(() -> inner.updateTime(columnIndex, x));
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        handled(() -> inner.updateTimestamp(columnIndex, x));
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        handled(() -> inner.updateAsciiStream(columnIndex, x, length));
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        handled(() -> inner.updateBinaryStream(columnIndex, x, length));
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        handled(() -> inner.updateCharacterStream(columnIndex, x, length));
    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        handled(() -> inner.updateObject(columnIndex, x, scaleOrLength));
    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        handled(() -> inner.updateObject(columnIndex, x));
    }

    @Override
    public void updateNull(String columnLabel) throws SQLException {
        handled(() -> inner.updateNull(columnLabel));
    }

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        handled(() -> inner.updateBoolean(columnLabel, x));
    }

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        handled(() -> inner.updateByte(columnLabel, x));
    }

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        handled(() -> inner.updateShort(columnLabel, x));
    }

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        handled(() -> inner.updateInt(columnLabel, x));
    }

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        handled(() -> inner.updateLong(columnLabel, x));
    }

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        handled(() -> inner.updateFloat(columnLabel, x));
    }

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        handled(() -> inner.updateDouble(columnLabel, x));
    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        handled(() -> inner.updateBigDecimal(columnLabel, x));
    }

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        handled(() -> inner.updateString(columnLabel, x));
    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        handled(() -> inner.updateBytes(columnLabel, x));
    }

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        handled(() -> inner.updateDate(columnLabel, x));
    }

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        handled(() -> inner.updateTime(columnLabel, x));
    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        handled(() -> inner.updateTimestamp(columnLabel, x));
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        handled(() -> inner.updateAsciiStream(columnLabel, x, length));
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        handled(() -> inner.updateBinaryStream(columnLabel, x, length));
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        handled(() -> inner.updateCharacterStream(columnLabel, reader, length));
    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        handled(() -> inner.updateObject(columnLabel, x, scaleOrLength));
    }

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        handled(() -> inner.updateObject(columnLabel, x));
    }

    @Override
    public void insertRow() throws SQLException {
        handled(() -> inner.insertRow());
    }

    @Override
    public void updateRow() throws SQLException {
        handled(() -> inner.updateRow());
    }

    @Override
    public void deleteRow() throws SQLException {
        handled(() -> inner.deleteRow());
    }

    @Override
    public void refreshRow() throws SQLException {
        handled(() -> inner.refreshRow());
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        handled(() -> inner.cancelRowUpdates());
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        handled(() -> inner.moveToInsertRow());
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        handled(() -> inner.moveToCurrentRow());
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        return handled(() -> inner.getObject(columnIndex, map));
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        return handled(() -> inner.getRef(columnIndex));
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        return handled(() -> inner.getBlob(columnIndex));
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        return handled(() -> inner.getClob(columnIndex));
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        return handled(() -> inner.getArray(columnIndex));
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        return handled(() -> inner.getObject(columnLabel, map));
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        return handled(() -> inner.getRef(columnLabel));
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        return handled(() -> inner.getBlob(columnLabel));
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        return handled(() -> inner.getClob(columnLabel));
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        return handled(() -> inner.getArray(columnLabel));
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        return handled(() -> inner.getDate(columnIndex, cal));
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        return handled(() -> inner.getDate(columnLabel, cal));
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        return handled(() -> inner.getTime(columnIndex, cal));
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        return handled(() -> inner.getTime(columnLabel, cal));
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        return handled(() -> inner.getTimestamp(columnIndex, cal));
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        return handled(() -> inner.getTimestamp(columnLabel, cal));
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        return handled(() -> inner.getURL(columnIndex));
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        return handled(() -> inner.getURL(columnLabel));
    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        handled(() -> inner.updateRef(columnIndex, x));
    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        handled(() -> inner.updateRef(columnLabel, x));
    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        handled(() -> inner.updateBlob(columnIndex, x));
    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        handled(() -> inner.updateBlob(columnLabel, x));
    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        handled(() -> inner.updateClob(columnIndex, x));
    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        handled(() -> inner.updateClob(columnLabel, x));
    }

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        handled(() -> inner.updateArray(columnIndex, x));
    }

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        handled(() -> inner.updateArray(columnLabel, x));
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        return handled(() -> inner.getRowId(columnIndex));
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        return handled(() -> inner.getRowId(columnLabel));
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        handled(() -> inner.updateRowId(columnIndex, x));
    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        handled(() -> inner.updateRowId(columnLabel, x));
    }

    @Override
    @SuppressWarnings("MagicConstant")
    public int getHoldability() throws SQLException {
        return handled(() -> inner.getHoldability());
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        handled(() -> inner.updateNString(columnIndex, nString));
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        handled(() -> inner.updateNString(columnLabel, nString));
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        handled(() -> inner.updateNClob(columnIndex, nClob));
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        handled(() -> inner.updateNClob(columnLabel, nClob));
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        return handled(() -> inner.getNClob(columnIndex));
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        return handled(() -> inner.getNClob(columnLabel));
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        return handled(() -> inner.getSQLXML(columnIndex));
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        return handled(() -> inner.getSQLXML(columnLabel));
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        handled(() -> inner.updateSQLXML(columnIndex, xmlObject));
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        handled(() -> inner.updateSQLXML(columnLabel, xmlObject));
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        return handled(() -> inner.getNString(columnIndex));
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        return handled(() -> inner.getNString(columnLabel));
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        return handled(() -> inner.getNCharacterStream(columnIndex));
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        return handled(() -> inner.getNCharacterStream(columnLabel));
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        handled(() -> inner.updateNCharacterStream(columnIndex, x, length));
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        handled(() -> inner.updateNCharacterStream(columnLabel, reader, length));
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        handled(() -> inner.updateAsciiStream(columnIndex, x, length));
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        handled(() -> inner.updateBinaryStream(columnIndex, x, length));
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        handled(() -> inner.updateCharacterStream(columnIndex, x, length));
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        handled(() -> inner.updateAsciiStream(columnLabel, x, length));
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        handled(() -> inner.updateBinaryStream(columnLabel, x, length));
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        handled(() -> inner.updateCharacterStream(columnLabel, reader, length));
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        handled(() -> inner.updateBlob(columnIndex, inputStream, length));
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        handled(() -> inner.updateBlob(columnLabel, inputStream, length));
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        handled(() -> inner.updateClob(columnIndex, reader, length));
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        handled(() -> inner.updateClob(columnLabel, reader, length));
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        handled(() -> inner.updateNClob(columnIndex, reader, length));
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        handled(() -> inner.updateNClob(columnLabel, reader, length));
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        handled(() -> inner.updateNCharacterStream(columnIndex, x));
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        handled(() -> inner.updateNCharacterStream(columnLabel, reader));
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        handled(() -> inner.updateAsciiStream(columnIndex, x));
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        handled(() -> inner.updateBinaryStream(columnIndex, x));
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        handled(() -> inner.updateCharacterStream(columnIndex, x));
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        handled(() -> inner.updateAsciiStream(columnLabel, x));
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        handled(() -> inner.updateBinaryStream(columnLabel, x));
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        handled(() -> inner.updateCharacterStream(columnLabel, reader));
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        handled(() -> inner.updateBlob(columnIndex, inputStream));
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        handled(() -> inner.updateBlob(columnLabel, inputStream));
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        handled(() -> inner.updateClob(columnIndex, reader));
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        handled(() -> inner.updateClob(columnLabel, reader));
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        handled(() -> inner.updateNClob(columnIndex, reader));
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        handled(() -> inner.updateNClob(columnLabel, reader));
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        return handled(() -> inner.getObject(columnIndex, type));
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        return handled(() -> inner.getObject(columnLabel, type));
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return inner.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return inner.isWrapperFor(iface);
    }

    private <T> T handled(ThrowableCallable<T, Throwable> callable) throws SQLException {
        try {
            return callable.call();
        } catch (SQLRecoverableException e) {
            conditionallyLog(e);
            Resources.markClosed(getConnection());
            throw e;
        } catch (Throwable e) {
            conditionallyLog(e);
            throw toSqlException(e);
        }
    }

    private void handled(ThrowableRunnable<Throwable> runnable) throws SQLException {
        try {
            runnable.run();
        } catch (SQLRecoverableException e) {
            conditionallyLog(e);
            Resources.markClosed(getConnection());
            throw e;
        } catch (Throwable e) {
            conditionallyLog(e);
            throw toSqlException(e);
        }
    }
}
