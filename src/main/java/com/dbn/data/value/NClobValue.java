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

import com.dbn.common.util.Strings;
import com.dbn.data.type.GenericDataType;
import lombok.extern.slf4j.Slf4j;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class NClobValue extends ClobValueBase<NClob> {
    public NClobValue() {
    }

    public NClobValue(CallableStatement callableStatement, int parameterIndex) throws SQLException {
        super(callableStatement, parameterIndex);
    }

    public NClobValue(ResultSet resultSet, int columnIndex) throws SQLException {
        super(resultSet, columnIndex);
    }

    @Override
    protected NClob createClob(Connection connection) throws SQLException {
        return connection.createNClob();
    }

    protected NClob createSerialClob(String charset) throws SQLException {
        if (Strings.isEmpty(charset)) return null;
        return new SerialNClob(charset.toCharArray());
    }

    @Override
    protected NClob read(CallableStatement callableStatement, int parameterIndex) throws SQLException {
        return callableStatement.getNClob(parameterIndex);
    }

    @Override
    protected NClob read(ResultSet resultSet, int columnIndex) throws SQLException {
        return resultSet.getNClob(columnIndex);
    }

    @Override
    protected void write(PreparedStatement preparedStatement, int parameterIndex, NClob clob) throws SQLException {
        preparedStatement.setNClob(parameterIndex, clob);
    }

    @Override
    protected void write(ResultSet resultSet, int columnIndex, NClob clob) throws SQLException {
        resultSet.updateNClob(columnIndex, clob);
    }

    @Override
    public GenericDataType getGenericDataType() {
        return GenericDataType.NCLOB;
    }


    @Override
    public String getDisplayValue() {
        return "[NCLOB]";
    }
}
