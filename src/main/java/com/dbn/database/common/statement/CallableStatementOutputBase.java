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

package com.dbn.database.common.statement;

import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.SQLException;

public abstract class CallableStatementOutputBase implements CallableStatementOutput{
    private int parameterIndexOffset;

    protected int shifted(int parameterIndex) {
        return parameterIndex + parameterIndexOffset;
    }

    @Override
    public void shiftParameterIndex(int shift) {
        parameterIndexOffset = shift;
    }

    /*************************************************************
     *         shifted parameter statement utilities             *
     *************************************************************/

    protected void registerOutParameter(CallableStatement statement, int parameterIndex, int sqlType) throws SQLException {
        statement.registerOutParameter(shifted(parameterIndex), sqlType);
    }

    protected String getString(CallableStatement statement, int parameterIndex) throws SQLException {
        return statement.getString(shifted(parameterIndex));
    }

    protected int getInt(CallableStatement statement, int parameterIndex) throws SQLException {
        return statement.getInt(shifted(parameterIndex));
    }

    protected Object getObject(CallableStatement statement, int parameterIndex) throws SQLException {
        return statement.getObject(shifted(parameterIndex));
    }

    protected Blob getBlob(CallableStatement statement, int parameterIndex) throws SQLException {
        return statement.getBlob(shifted(parameterIndex));
    }

    protected Clob getClob(CallableStatement statement, int parameterIndex) throws SQLException {
        return statement.getClob(shifted(parameterIndex));
    }
}
