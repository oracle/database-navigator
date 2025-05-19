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

import com.dbn.common.data.Data;
import com.dbn.data.type.GenericDataType;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.data.Data.asDoubleList;
import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Getter
public class VectorValue extends ValueAdapter<List<Double>>{
    public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    private List<Double> values = Collections.emptyList();

    public VectorValue() {
    }

    public VectorValue(CallableStatement callableStatement, int parameterIndex) throws SQLException {
        double[] doubles = callableStatement.getObject(parameterIndex, double[].class);
        values = asDoubleList(doubles);
    }

    public VectorValue(ResultSet resultSet, int columnIndex) throws SQLException {
        double[] doubles = resultSet.getObject(columnIndex, double[].class);
        values = asDoubleList(doubles);
    }

    @Override
    public GenericDataType getGenericDataType() {
        return GenericDataType.VECTOR;
    }

    @Override
    public List<Double> read() throws SQLException {
        return values;
    }

    @Nullable
    @Override
    public String export() throws SQLException {
        return values == null ? null : Objects.toString(values);
    }

    @Override
    public void write(Connection connection, PreparedStatement preparedStatement, int parameterIndex, List<Double> values) throws SQLException {
        try {
            this.values = asDoubleList(values);
            preparedStatement.setObject(parameterIndex, values);
        } catch (Throwable e) {
            conditionallyLog(e);
            throw toSqlException(e, "Could not write array value. Your JDBC driver may not support this feature");
        }

    }

    @Override
    public void write(Connection connection, ResultSet resultSet, int columnIndex, List<Double> values) throws SQLException {
        try {
            this.values = asDoubleList(values);
            if (values == null) {
                resultSet.updateObject(columnIndex, null);
            } else {
                resultSet.updateString(columnIndex, Objects.toString(values));
            }
        } catch (Throwable e) {
            conditionallyLog(e);
            throw toSqlException(e, "Could not write array value. Your JDBC driver may not support this feature");
        }
    }

    @Override
    public String getDisplayValue() {
        if (values == null) return "";

        List<String> values = new ArrayList<>();
        int size = this.values.size();
        int length = Math.min(size, 3);
        for (int i = 0; i< length; i++) {
            values.add(Data.asString(this.values.get(i)));
        }
        if (size > length) values.add("...");
        return values.toString();
    }

    @Nullable
    public List<String> getStringValues() {
        if (values == null) return null;
        return Data.asStringList(values);
    }

    @Override
    public String toString() {
        return getDisplayValue();
    }
}
