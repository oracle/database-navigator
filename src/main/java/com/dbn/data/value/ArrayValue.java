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
import org.jetbrains.annotations.Nullable;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class ArrayValue extends ValueAdapter<List<String>>{
    private Array array;
    private List<String> values;

    public ArrayValue() {
    }

    public ArrayValue(CallableStatement callableStatement, int parameterIndex) throws SQLException {
        this.array = callableStatement.getArray(parameterIndex);
        values = readArray(array);
    }

    public ArrayValue(ResultSet resultSet, int columnIndex) throws SQLException {
        array = resultSet.getArray(columnIndex);
        values = readArray(array);
    }

    private static List<String> readArray(Array array) throws SQLException {
        List<String> values = null;
        if (array != null) {
            ResultSet arrayResultSet = array.getResultSet();
            while (arrayResultSet.next()) {
                if (values == null) values = new ArrayList<>();
                Object object = arrayResultSet.getObject(2);
                values.add(object == null ? null : object.toString());
            }
            arrayResultSet.close();
        }
        return values;
    }

    @Override
    public GenericDataType getGenericDataType() {
        return GenericDataType.ARRAY;
    }

    @Nullable
    @Override
    public List<String> read() throws SQLException {
        return values;
    }

    @Nullable
    @Override
    public String export() throws SQLException {
        return values == null ? null : values.toString();
    }

    @Override
    public void write(Connection connection, PreparedStatement preparedStatement, int parameterIndex, @Nullable List<String> values) throws SQLException {
        try {
            this.values = values;
            if (values == null) {
                preparedStatement.setArray(parameterIndex, null);
            } else {
                array = connection.createArrayOf("varchar", values.toArray());
                preparedStatement.setArray(parameterIndex, array);
            }
        } catch (Throwable e) {
            conditionallyLog(e);
            throw toSqlException(e, "Could not write array value. Your JDBC driver may not support this feature");
        }

    }

    @Override
    public void write(Connection connection, ResultSet resultSet, int columnIndex, @Nullable List<String> values) throws SQLException {
        try {
            this.values = values;
            if (values == null) {
                resultSet.updateArray(columnIndex, null);
            } else {
                String columnTypeName = resultSet.getMetaData().getColumnTypeName(columnIndex).substring(1);
                array = connection.createArrayOf(columnTypeName, values.toArray());
                resultSet.updateArray(columnIndex, array);
            }
        } catch (Throwable e) {
            conditionallyLog(e);
            throw toSqlException(e, "Could not write array value. Your JDBC driver may not support this feature");
        }
    }

    @Override
    public String getDisplayValue() {
        return values == null ? "" : values.toString();
    }

    @Override
    public String toString() {
        return getDisplayValue();
    }
}
