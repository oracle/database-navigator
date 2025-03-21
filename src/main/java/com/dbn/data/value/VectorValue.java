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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Getter
public class VectorValue extends ValueAdapter<double[]>{
    private double[] values = new double[0];

    public VectorValue() {
    }

    public VectorValue(CallableStatement callableStatement, int parameterIndex) throws SQLException {
        values = callableStatement.getObject(parameterIndex, double[].class);
    }

    public VectorValue(ResultSet resultSet, int columnIndex) throws SQLException {
        values = resultSet.getObject(columnIndex, double[].class);
    }

    @Override
    public GenericDataType getGenericDataType() {
        return GenericDataType.VECTOR;
    }

    @Override
    public double[] read() throws SQLException {
        return values;
    }

    @Nullable
    @Override
    public String export() throws SQLException {
        return values == null ? null : Arrays.toString(values);
    }

    @Override
    public void write(Connection connection, PreparedStatement preparedStatement, int parameterIndex, double[] values) throws SQLException {
        try {
            this.values = values;
            preparedStatement.setObject(parameterIndex, values);
        } catch (Throwable e) {
            conditionallyLog(e);
            throw toSqlException(e, "Could not write array value. Your JDBC driver may not support this feature");
        }

    }

    @Override
    public void write(Connection connection, ResultSet resultSet, int columnIndex, double[] values) throws SQLException {
        try {
            this.values = values;
            resultSet.updateString(columnIndex, Arrays.toString(values));
        } catch (Throwable e) {
            conditionallyLog(e);
            throw toSqlException(e, "Could not write array value. Your JDBC driver may not support this feature");
        }
    }

    @Override
    public String getDisplayValue() {
        List<String> values = new ArrayList<>();
        int length = Math.min(this.values.length, 3);
        for (int i = 0; i< length; i++) {
            values.add(Double.toString(this.values[i]));
        }
        if (this.values.length > length) values.add("...");
        return values.toString();
    }

    public String[] getStringValues() {
        return Arrays.stream(values).mapToObj(d -> Double.toString(d)).toArray(String[]::new);
    }

    @Override
    public String toString() {
        return getDisplayValue();
    }
}
