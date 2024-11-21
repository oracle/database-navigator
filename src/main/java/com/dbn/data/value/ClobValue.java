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

import javax.sql.rowset.serial.SerialClob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClobValue extends ClobValueBase<Clob>{

    public ClobValue() {
    }

    public ClobValue(CallableStatement callableStatement, int parameterIndex) throws SQLException {
        super(callableStatement, parameterIndex);
    }

    public ClobValue(ResultSet resultSet, int columnIndex) throws SQLException {
        super(resultSet, columnIndex);
    }

    @Override
    protected Clob createClob(Connection connection) throws SQLException {
        return connection.createClob();
    }

    @Override
    protected Clob createSerialClob(String charset) throws SQLException {
        if (Strings.isEmpty(charset)) return null;
        SerialClob serialClob = new SerialClob(charset.toCharArray());
        return serialClob;
    }

    @Override
    protected Clob read(CallableStatement callableStatement, int parameterIndex) throws SQLException {
        return callableStatement.getClob(parameterIndex);
    }

    @Override
    protected Clob read(ResultSet resultSet, int columnIndex) throws SQLException {
        return resultSet.getClob(columnIndex);
    }

    @Override
    protected void write(PreparedStatement preparedStatement, int parameterIndex, Clob clob) throws SQLException {
        preparedStatement.setClob(parameterIndex, clob);
    }

    @Override
    protected void write(ResultSet resultSet, int columnIndex, Clob clob) throws SQLException {
        resultSet.updateClob(columnIndex, clob);
    }

    @Override
    public GenericDataType getGenericDataType() {
        return GenericDataType.CLOB;
    }

    @Override
    public String getDisplayValue() {
        return "[CLOB]";
    }
}
