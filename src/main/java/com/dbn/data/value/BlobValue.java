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

package com.dbn.data.value;

import com.dbn.data.type.GenericDataType;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
public class BlobValue extends LargeObjectValue {
    private Blob blob;
    private InputStream inputStream;

    public BlobValue() {}

    private Blob createSerialBlob(String charset) throws SQLException {
        if (isEmpty(charset)) return null;

        return new SerialBlob(charset.getBytes());
    }

    public BlobValue(CallableStatement callableStatement, int parameterIndex) throws SQLException {
        try {
            blob = callableStatement.getBlob(parameterIndex);
        } catch (SQLFeatureNotSupportedException e) {
            conditionallyLog(e);
            String charset = callableStatement.getString(parameterIndex);
            blob = createSerialBlob(charset);
        }


    }

    public BlobValue(ResultSet resultSet, int columnIndex) throws SQLException {
        try {
            blob = resultSet.getBlob(columnIndex);
        } catch (SQLFeatureNotSupportedException e) {
            conditionallyLog(e);
            String charset = resultSet.getString(columnIndex);
            blob = createSerialBlob(charset);
        }
    }

    @Override
    public void write(Connection connection, PreparedStatement preparedStatement, int parameterIndex, @Nullable String value)
            throws SQLException {
        try {
            if (value == null) {
                preparedStatement.setBlob(parameterIndex, (Blob) null);
            } else {
                blob = connection.createBlob();
                blob.setBytes(1, value.getBytes());
                preparedStatement.setBlob(parameterIndex, blob);
            }
        } catch (SQLFeatureNotSupportedException e) {
            conditionallyLog(e);
            if (value == null) {
                preparedStatement.setString(parameterIndex, null);
            } else {
                blob = createSerialBlob(value);
                preparedStatement.setString(parameterIndex, value);
            }
        }
    }

    @Override
    public void write(Connection connection, ResultSet resultSet, int columnIndex, @Nullable String value) throws SQLException {
        try {
            if (isEmpty(value)) {
                blob = null;
            } else {
                blob = connection.createBlob();
                blob.setBytes(1, value.getBytes());
            }
            resultSet.updateBlob(columnIndex, blob);
        } catch (SQLFeatureNotSupportedException e) {
            conditionallyLog(e);
            if (isEmpty(value)) {
                blob = null;
            } else {
                blob = createSerialBlob(value);
            }
            resultSet.updateString(columnIndex, value);

        }
    }

    @Override
    public GenericDataType getGenericDataType() {
        return GenericDataType.BLOB;
    }

    @Override
    @Nullable
    public String read() throws SQLException {
        return read(0);
    }

    @Nullable
    @Override
    public String export() throws SQLException {
        return read();
    }

    @Override
    public String read(int maxSize) throws SQLException {
        if (blob == null) return null;

        long totalLength = blob.length();
        int size = (int) (maxSize == 0 ? totalLength : Math.min(maxSize, totalLength));
        try {
            byte[] buffer = new byte[size];
            inputStream = blob.getBinaryStream();
            inputStream.read(buffer, 0, size);
            return new String(buffer);
        } catch (IOException e) {
            conditionallyLog(e);
            throw new SQLException("Could not read value from BLOB.");
        } finally {
            if (totalLength <= size) {
                release();
            }
        }
    }

    @Override
    public void release() {
        if (inputStream != null) {
            try {
                inputStream.close();
                inputStream = null;
            } catch (IOException e) {
                conditionallyLog(e);
                log.error("Could not close BLOB input stream.", e);
            }
        }
    }


    @Override
    public long size() throws SQLException {
        return blob == null ? 0 : blob.length();
    }

    @Override
    public String getDisplayValue() {
        /*try {
            return "[BLOB] " + size() + "";
        } catch (SQLException e) {
            return "[BLOB]";
        }*/

        return "[BLOB]";
    }
}
