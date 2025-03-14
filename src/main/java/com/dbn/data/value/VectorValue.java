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
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static java.util.Collections.emptyList;

@Getter
public class VectorValue extends ValueAdapter<List<Double>>{
    private List<Double> values;

    public VectorValue() {
    }

    public VectorValue(CallableStatement callableStatement, int parameterIndex) throws SQLException {
        String string = callableStatement.getString(parameterIndex);
        values = toDoubleArray(string);
    }

    public VectorValue(ResultSet resultSet, int columnIndex) throws SQLException {
        String string = resultSet.getString(columnIndex);
        values = toDoubleArray(string);
    }

    private static List<Double> toDoubleArray(String value) throws SQLException {
        if (value == null || value.length() < 2) return emptyList();
        return Arrays.stream(value.substring(1, value.length() - 2).split(",")).map(s -> Double.valueOf(s)).collect(Collectors.toList());
    }

    private static String toString(List<Double> values) {
        if (values == null) return "[]";
        return "[" + values.stream().map(d -> String.valueOf(d)).collect(Collectors.joining(",")) + "]";
    }


    @Override
    public GenericDataType getGenericDataType() {
        return GenericDataType.VECTOR;
    }

    @Nullable
    @Override
    public List<Double> read() throws SQLException {
        return values;
    }

    @Nullable
    @Override
    public String export() throws SQLException {
        return values == null ? null : values.toString();
    }

    @Override
    public void write(Connection connection, PreparedStatement preparedStatement, int parameterIndex, @Nullable List<Double> values) throws SQLException {
        try {
            this.values = values;
            preparedStatement.setString(parameterIndex, toString(values));
        } catch (Throwable e) {
            conditionallyLog(e);
            throw toSqlException(e, "Could not write array value. Your JDBC driver may not support this feature");
        }

    }

    @Override
    public void write(Connection connection, ResultSet resultSet, int columnIndex, @Nullable List<Double> values) throws SQLException {
        try {
            this.values = values;
            resultSet.updateString(columnIndex, toString(values));
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
