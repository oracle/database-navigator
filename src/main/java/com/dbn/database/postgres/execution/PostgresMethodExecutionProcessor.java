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

package com.dbn.database.postgres.execution;

import com.dbn.connection.jdbc.DBNPreparedStatement;
import com.dbn.data.type.DBDataType;
import com.dbn.database.common.execution.MethodExecutionProcessorImpl;
import com.dbn.execution.method.MethodExecutionInput;
import com.dbn.execution.method.result.MethodExecutionResult;
import com.dbn.object.DBArgument;
import com.dbn.object.DBMethod;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class PostgresMethodExecutionProcessor extends MethodExecutionProcessorImpl {
    private Boolean isQuery;

    public PostgresMethodExecutionProcessor(DBMethod method) {
        super(method);
    }

    @Override
    public String buildExecutionCommand(MethodExecutionInput executionInput) throws SQLException {
        StringBuilder buffer = new StringBuilder();
        String methodName = getMethod().getQualifiedName(true);
        if (isQuery()) {
            buffer.append("select * from ");
            buffer.append(methodName);
            buffer.append("(");
            for (int i = 1; i < getArgumentsCount(); i++) {
                if (i > 1) buffer.append(",");
                buffer.append("?");
            }
            buffer.append(")");

        } else {
            buffer.append("{ ? = call ");
            buffer.append(methodName);
            buffer.append("(");
            for (int i = 1; i < getArgumentsCount(); i++) {
                if (i > 1) buffer.append(",");
                buffer.append("?");
            }
            buffer.append(")}");
        }

        return buffer.toString();
    }

    @Override
    protected void bindParameters(MethodExecutionInput executionInput, PreparedStatement preparedStatement) throws SQLException {
        if (isQuery()) {
            List<DBArgument> arguments = getArguments();
            for (int i = 1; i < arguments.size(); i++) {
                DBArgument argument = arguments.get(i);
                DBDataType dataType = argument.getDataType();
                if (argument.isInput()) {
                    String stringValue = executionInput.getInputValue(argument);
                    setParameterValue(preparedStatement, argument.getPosition()-1, dataType, stringValue);
                } else if (argument.isOutput()) {
                    setParameterValue(preparedStatement, argument.getPosition()-1, dataType, "1");
                }
            }

        } else {
            super.bindParameters(executionInput, preparedStatement);
        }
    }

    @Override
    public void loadValues(MethodExecutionResult executionResult, DBNPreparedStatement preparedStatement) throws SQLException {
        if (isQuery()) {
            DBArgument returnArgument = getReturnArgument();
            executionResult.addArgumentValue(returnArgument, preparedStatement.getResultSet());
        } else {
            super.loadValues(executionResult, preparedStatement);
        }
    }

    @Override
    protected boolean isQuery() {
        if (isQuery == null) {
            DBMethod method = getMethod();
            DBArgument returnArgument = method.getReturnArgument();
            isQuery = returnArgument != null && returnArgument.getDataType().isSet() && !hasOutputArguments();
        }
        return isQuery;
    }

    private boolean hasOutputArguments() {
        List<DBArgument> arguments = getArguments();
        for (int i=1; i< arguments.size(); i++) {
            if (arguments.get(i).isOutput() && !arguments.get(i).isInput()) {
                return true;
            }
        }
        return false;
    }
}