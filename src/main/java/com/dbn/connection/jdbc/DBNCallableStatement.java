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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

public class DBNCallableStatement extends DBNPreparedStatement<CallableStatement> implements CallableStatement{
    DBNCallableStatement(CallableStatement inner, DBNConnection connection) {
        super(inner, connection);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
        inner.registerOutParameter(parameterIndex, sqlType);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
        inner.registerOutParameter(parameterIndex, sqlType, scale);
    }

    @Override
    public boolean wasNull() throws SQLException {
        return inner.wasNull();
    }

    @Override
    public String getString(int parameterIndex) throws SQLException {
        return inner.getString(parameterIndex);
    }

    @Override
    public boolean getBoolean(int parameterIndex) throws SQLException {
        return inner.getBoolean(parameterIndex);
    }

    @Override
    public byte getByte(int parameterIndex) throws SQLException {
        return inner.getByte(parameterIndex);
    }

    @Override
    public short getShort(int parameterIndex) throws SQLException {
        return inner.getShort(parameterIndex);
    }

    @Override
    public int getInt(int parameterIndex) throws SQLException {
        return inner.getInt(parameterIndex);
    }

    @Override
    public long getLong(int parameterIndex) throws SQLException {
        return inner.getLong(parameterIndex);
    }

    @Override
    public float getFloat(int parameterIndex) throws SQLException {
        return inner.getFloat(parameterIndex);
    }

    @Override
    public double getDouble(int parameterIndex) throws SQLException {
        return inner.getDouble(parameterIndex);
    }

    @Override
    @Deprecated
    public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
        return inner.getBigDecimal(parameterIndex, scale);
    }

    @Override
    public byte[] getBytes(int parameterIndex) throws SQLException {
        return inner.getBytes(parameterIndex);
    }

    @Override
    public Date getDate(int parameterIndex) throws SQLException {
        return inner.getDate(parameterIndex);
    }

    @Override
    public Time getTime(int parameterIndex) throws SQLException {
        return inner.getTime(parameterIndex);
    }

    @Override
    public Timestamp getTimestamp(int parameterIndex) throws SQLException {
        return inner.getTimestamp(parameterIndex);
    }

    @Override
    public Object getObject(int parameterIndex) throws SQLException {
        return wrap(inner.getObject(parameterIndex));
    }

    @Override
    public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
        return inner.getBigDecimal(parameterIndex);
    }

    @Override
    public Object getObject(int parameterIndex, Map<String, Class<?>> map) throws SQLException {
        return wrap(inner.getObject(parameterIndex, map));
    }

    @Override
    public Ref getRef(int parameterIndex) throws SQLException {
        return inner.getRef(parameterIndex);
    }

    @Override
    public Blob getBlob(int parameterIndex) throws SQLException {
        return inner.getBlob(parameterIndex);
    }

    @Override
    public Clob getClob(int parameterIndex) throws SQLException {
        return inner.getClob(parameterIndex);
    }

    @Override
    public Array getArray(int parameterIndex) throws SQLException {
        return inner.getArray(parameterIndex);
    }

    @Override
    public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
        return inner.getDate(parameterIndex, cal);
    }

    @Override
    public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
        return inner.getTime(parameterIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
        return inner.getTimestamp(parameterIndex, cal);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType, String typeName) throws SQLException {
        inner.registerOutParameter(parameterIndex, sqlType, typeName);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
        inner.registerOutParameter(parameterName, sqlType);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
        inner.registerOutParameter(parameterName, sqlType, scale);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
        inner.registerOutParameter(parameterName, sqlType, typeName);
    }

    @Override
    public URL getURL(int parameterIndex) throws SQLException {
        return inner.getURL(parameterIndex);
    }

    @Override
    public void setURL(String parameterName, URL val) throws SQLException {
        inner.setURL(parameterName, val);
    }

    @Override
    public void setNull(String parameterName, int sqlType) throws SQLException {
        inner.setNull(parameterName, sqlType);
    }

    @Override
    public void setBoolean(String parameterName, boolean x) throws SQLException {
        inner.setBoolean(parameterName, x);
    }

    @Override
    public void setByte(String parameterName, byte x) throws SQLException {
        inner.setByte(parameterName, x);
    }

    @Override
    public void setShort(String parameterName, short x) throws SQLException {
        inner.setShort(parameterName, x);
    }

    @Override
    public void setInt(String parameterName, int x) throws SQLException {
        inner.setInt(parameterName, x);
    }

    @Override
    public void setLong(String parameterName, long x) throws SQLException {
        inner.setLong(parameterName, x);
    }

    @Override
    public void setFloat(String parameterName, float x) throws SQLException {
        inner.setFloat(parameterName, x);
    }

    @Override
    public void setDouble(String parameterName, double x) throws SQLException {
        inner.setDouble(parameterName, x);
    }

    @Override
    public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
        inner.setBigDecimal(parameterName, x);
    }

    @Override
    public void setString(String parameterName, String x) throws SQLException {
        inner.setString(parameterName, x);
    }

    @Override
    public void setBytes(String parameterName, byte[] x) throws SQLException {
        inner.setBytes(parameterName, x);
    }

    @Override
    public void setDate(String parameterName, Date x) throws SQLException {
        inner.setDate(parameterName, x);
    }

    @Override
    public void setTime(String parameterName, Time x) throws SQLException {
        inner.setTime(parameterName, x);
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
        inner.setTimestamp(parameterName, x);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {
        inner.setAsciiStream(parameterName, x, length);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
        inner.setBinaryStream(parameterName, x, length);
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {
        inner.setObject(parameterName, x, targetSqlType, scale);
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
        inner.setObject(parameterName, x, targetSqlType);
    }

    @Override
    public void setObject(String parameterName, Object x) throws SQLException {
        inner.setObject(parameterName, x);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
        inner.setCharacterStream(parameterName, reader, length);
    }

    @Override
    public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
        inner.setDate(parameterName, x, cal);
    }

    @Override
    public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
        inner.setTime(parameterName, x, cal);
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
        inner.setTimestamp(parameterName, x, cal);
    }

    @Override
    public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
        inner.setNull(parameterName, sqlType, typeName);
    }

    @Override
    public String getString(String parameterName) throws SQLException {
        return inner.getString(parameterName);
    }

    @Override
    public boolean getBoolean(String parameterName) throws SQLException {
        return inner.getBoolean(parameterName);
    }

    @Override
    public byte getByte(String parameterName) throws SQLException {
        return inner.getByte(parameterName);
    }

    @Override
    public short getShort(String parameterName) throws SQLException {
        return inner.getShort(parameterName);
    }

    @Override
    public int getInt(String parameterName) throws SQLException {
        return inner.getInt(parameterName);
    }

    @Override
    public long getLong(String parameterName) throws SQLException {
        return inner.getLong(parameterName);
    }

    @Override
    public float getFloat(String parameterName) throws SQLException {
        return inner.getFloat(parameterName);
    }

    @Override
    public double getDouble(String parameterName) throws SQLException {
        return inner.getDouble(parameterName);
    }

    @Override
    public byte[] getBytes(String parameterName) throws SQLException {
        return inner.getBytes(parameterName);
    }

    @Override
    public Date getDate(String parameterName) throws SQLException {
        return inner.getDate(parameterName);
    }

    @Override
    public Time getTime(String parameterName) throws SQLException {
        return inner.getTime(parameterName);
    }

    @Override
    public Timestamp getTimestamp(String parameterName) throws SQLException {
        return inner.getTimestamp(parameterName);
    }

    @Override
    public Object getObject(String parameterName) throws SQLException {
        return wrap(inner.getObject(parameterName));
    }

    @Override
    public BigDecimal getBigDecimal(String parameterName) throws SQLException {
        return inner.getBigDecimal(parameterName);
    }

    @Override
    public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
        return wrap(inner.getObject(parameterName, map));
    }

    @Override
    public Ref getRef(String parameterName) throws SQLException {
        return inner.getRef(parameterName);
    }

    @Override
    public Blob getBlob(String parameterName) throws SQLException {
        return inner.getBlob(parameterName);
    }

    @Override
    public Clob getClob(String parameterName) throws SQLException {
        return inner.getClob(parameterName);
    }

    @Override
    public Array getArray(String parameterName) throws SQLException {
        return inner.getArray(parameterName);
    }

    @Override
    public Date getDate(String parameterName, Calendar cal) throws SQLException {
        return inner.getDate(parameterName, cal);
    }

    @Override
    public Time getTime(String parameterName, Calendar cal) throws SQLException {
        return inner.getTime(parameterName, cal);
    }

    @Override
    public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
        return inner.getTimestamp(parameterName, cal);
    }

    @Override
    public URL getURL(String parameterName) throws SQLException {
        return inner.getURL(parameterName);
    }

    @Override
    public RowId getRowId(int parameterIndex) throws SQLException {
        return inner.getRowId(parameterIndex);
    }

    @Override
    public RowId getRowId(String parameterName) throws SQLException {
        return inner.getRowId(parameterName);
    }

    @Override
    public void setRowId(String parameterName, RowId x) throws SQLException {
        inner.setRowId(parameterName, x);
    }

    @Override
    public void setNString(String parameterName, String value) throws SQLException {
        inner.setNString(parameterName, value);
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
        inner.setNCharacterStream(parameterName, value, length);
    }

    @Override
    public void setNClob(String parameterName, NClob value) throws SQLException {
        inner.setNClob(parameterName, value);
    }

    @Override
    public void setClob(String parameterName, Reader reader, long length) throws SQLException {
        inner.setClob(parameterName, reader, length);
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
        inner.setBlob(parameterName, inputStream, length);
    }

    @Override
    public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
        inner.setNClob(parameterName, reader, length);
    }

    @Override
    public NClob getNClob(int parameterIndex) throws SQLException {
        return inner.getNClob(parameterIndex);
    }

    @Override
    public NClob getNClob(String parameterName) throws SQLException {
        return inner.getNClob(parameterName);
    }

    @Override
    public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
        inner.setSQLXML(parameterName, xmlObject);
    }

    @Override
    public SQLXML getSQLXML(int parameterIndex) throws SQLException {
        return inner.getSQLXML(parameterIndex);
    }

    @Override
    public SQLXML getSQLXML(String parameterName) throws SQLException {
        return inner.getSQLXML(parameterName);
    }

    @Override
    public String getNString(int parameterIndex) throws SQLException {
        return inner.getNString(parameterIndex);
    }

    @Override
    public String getNString(String parameterName) throws SQLException {
        return inner.getNString(parameterName);
    }

    @Override
    public Reader getNCharacterStream(int parameterIndex) throws SQLException {
        return inner.getNCharacterStream(parameterIndex);
    }

    @Override
    public Reader getNCharacterStream(String parameterName) throws SQLException {
        return inner.getNCharacterStream(parameterName);
    }

    @Override
    public Reader getCharacterStream(int parameterIndex) throws SQLException {
        return inner.getCharacterStream(parameterIndex);
    }

    @Override
    public Reader getCharacterStream(String parameterName) throws SQLException {
        return inner.getCharacterStream(parameterName);
    }

    @Override
    public void setBlob(String parameterName, Blob x) throws SQLException {
        inner.setBlob(parameterName, x);
    }

    @Override
    public void setClob(String parameterName, Clob x) throws SQLException {
        inner.setClob(parameterName, x);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
        inner.setAsciiStream(parameterName, x, length);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
        inner.setBinaryStream(parameterName, x, length);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
        inner.setCharacterStream(parameterName, reader, length);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
        inner.setAsciiStream(parameterName, x);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
        inner.setBinaryStream(parameterName, x);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
        inner.setCharacterStream(parameterName, reader);
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
        inner.setNCharacterStream(parameterName, value);
    }

    @Override
    public void setClob(String parameterName, Reader reader) throws SQLException {
        inner.setClob(parameterName, reader);
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
        inner.setBlob(parameterName, inputStream);
    }

    @Override
    public void setNClob(String parameterName, Reader reader) throws SQLException {
        inner.setNClob(parameterName, reader);
    }

    @Override
    public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
        return inner.getObject(parameterIndex, type);
    }

    @Override
    public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
        return inner.getObject(parameterName, type);
    }
}
