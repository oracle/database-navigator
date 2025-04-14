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
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.common.util.Commons.nvl;

@Getter
@NoArgsConstructor
public class JsonValue extends LargeObjectValue{
    private String data;

    public JsonValue(String data) {
        this.data = data;
    }

    public JsonValue(CallableStatement callableStatement, int parameterIndex) throws SQLException {
        data = callableStatement.getString(parameterIndex);
    }

    public JsonValue(ResultSet resultSet, int columnIndex) throws SQLException {
        data = resultSet.getString(columnIndex);
    }

    @Override
    public GenericDataType getGenericDataType() {
        return GenericDataType.JSON;
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
    @Nullable
    public String read(int maxSize) throws SQLException {
        return data;
    }


    @Override
    public void write(Connection connection, PreparedStatement preparedStatement, int parameterIndex, @Nullable String value) throws SQLException {
        this.data = value;
        preparedStatement.setString(parameterIndex, value);
    }

    @Override
    public void write(Connection connection, ResultSet resultSet, int columnIndex, @Nullable String value) throws SQLException {
        this.data = value;
        resultSet.updateString(columnIndex, value);
    }

    @Override
    public void release() {

    }

    @Override
    public long size() throws SQLException {
        return 0;
    }

    @Override
    public String getDisplayValue() {
        return "[JSON]";
    }

    @Override
    public String toString() {
        return nvl(data, "");
    }
}
