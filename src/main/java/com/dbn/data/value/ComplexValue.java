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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class ComplexValue extends ValueAdapter<String> implements Comparable<ComplexValue>{
    private String displayValue;
    private Object value;

    public ComplexValue() {
    }

    public ComplexValue(CallableStatement callableStatement, int parameterIndex) throws SQLException {
        this.value = callableStatement.getObject(parameterIndex);
        this.displayValue = callableStatement.getString(parameterIndex);
    }

    public ComplexValue(ResultSet resultSet, int columnIndex) throws SQLException {
        this.value = resultSet.getObject(columnIndex);
        this.displayValue = resultSet.getString(columnIndex);
    }

    @Override
    public GenericDataType getGenericDataType() {
        return GenericDataType.COMPLEX;
    }

    @Nullable
    @Override
    public String read() throws SQLException {
        return displayValue;
    }

    @Nullable
    @Override
    public String export() throws SQLException {
        return read();
    }

    @Override
    public void write(Connection connection, PreparedStatement preparedStatement, int parameterIndex, @Nullable String value) throws SQLException {
        try {
            if (value == null) {
                preparedStatement.setObject(parameterIndex, null);
            } else {
                preparedStatement.setString(parameterIndex, value);
            }
        } catch (Throwable e) {
            conditionallyLog(e);
            throw toSqlException(e, "Could not write complex value. Your JDBC driver may not support this feature");
        }

    }

    @Override
    public void write(Connection connection, ResultSet resultSet, int columnIndex, @Nullable String value) throws SQLException {
        try {
            if (value == null) {
                resultSet.updateObject(columnIndex, null);
            } else {
                resultSet.updateString(columnIndex, value);
            }
        } catch (Throwable e) {
            conditionallyLog(e);
            throw toSqlException(e, "Could not write complex value. Your JDBC driver may not support this feature");
        }
    }

    @Override
    public String getDisplayValue() {
        return displayValue;
    }

    @Override
    public String toString() {
        return getDisplayValue();
    }

    @Override
    public int compareTo(ComplexValue that) {
        return nvl(this.displayValue, "").compareTo(nvl(that.displayValue, ""));
    }
}
