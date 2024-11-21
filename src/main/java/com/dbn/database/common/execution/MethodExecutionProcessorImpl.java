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

package com.dbn.database.common.execution;

import com.dbn.common.locale.Formatter;
import com.dbn.common.thread.CancellableDatabaseCall;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.Resources;
import com.dbn.connection.SchemaId;
import com.dbn.connection.SessionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNPreparedStatement;
import com.dbn.data.type.DBDataType;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.ExecutionOption;
import com.dbn.execution.ExecutionOptions;
import com.dbn.execution.ExecutionStatus;
import com.dbn.execution.logging.DatabaseLoggingManager;
import com.dbn.execution.method.MethodExecutionContext;
import com.dbn.execution.method.MethodExecutionInput;
import com.dbn.execution.method.result.MethodExecutionResult;
import com.dbn.object.DBArgument;
import com.dbn.object.DBMethod;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public abstract class MethodExecutionProcessorImpl implements MethodExecutionProcessor {
    private final DBObjectRef<DBMethod> method;

    protected MethodExecutionProcessorImpl(DBMethod method) {
        this.method = DBObjectRef.of(method);
    }

    @Override
    @NotNull
    public DBMethod getMethod() {
        return DBObjectRef.ensure(method);
    }

    public List<DBArgument> getArguments() {
        DBMethod method = getMethod();
        return method.getArguments();
    }

    protected int getArgumentsCount() {
        return getArguments().size();
    }

    protected DBArgument getReturnArgument() {
        DBMethod method = getMethod();
        return method.getReturnArgument();
    }


    @Override
    public void execute(MethodExecutionInput executionInput, DBDebuggerType debuggerType) throws SQLException {
        ConnectionHandler connection = getConnection();
        SessionId targetSessionId = executionInput.getTargetSessionId();
        SchemaId targetSchemaId = executionInput.getTargetSchemaId();
        DBNConnection conn = connection.getConnection(targetSessionId, targetSchemaId);

        if (targetSessionId == SessionId.POOL) {
            Resources.setAutoCommit(conn, false);
        }

        execute(executionInput, conn, debuggerType);
    }

    @Override
    public void execute(MethodExecutionInput executionInput, @NotNull DBNConnection conn, DBDebuggerType debuggerType) throws SQLException {
        MethodExecutionContext context = executionInput.initExecution(debuggerType);
        context.setConnection(conn);
        context.setDebuggerType(debuggerType);
        context.set(ExecutionStatus.EXECUTING, true);

        try {
            initCommand(context);
            initLogging(context);
            initTimeout(context);
            initParameters(context);

            execute(context);
        } catch (SQLException e) {
            conditionallyLog(e);
            Resources.cancel(context.getStatement());
            throw e;
        } finally {

            release(context);
        }
    }

    private void release(MethodExecutionContext context) {
        ConnectionHandler connection = nn(context.getTargetConnection());
        DBNConnection conn = context.getConnection();
        if (context.isLogging()) {
            DatabaseLoggingManager loggingManager = DatabaseLoggingManager.getInstance(getProject());
            loggingManager.disableLogger(connection, conn);
        }

        ExecutionOptions options = context.getOptions();
        if (options.is(ExecutionOption.COMMIT_AFTER_EXECUTION)) {
            Resources.commitSilently(conn);
        }

        if (conn.isDebugConnection()) {
            Resources.close(conn);

        } else if (conn.isPoolConnection()) {
            connection.freePoolConnection(conn);
        }
    }

    private void initCommand(MethodExecutionContext context) throws SQLException {
        MethodExecutionInput executionInput = context.getInput();
        String command = buildExecutionCommand(executionInput);
        DBNConnection conn = context.getConnection();
        DBNPreparedStatement<?> statement = isQuery() ?
                conn.prepareStatement(command) :
                conn.prepareCall(command);

        context.setStatement(statement);
    }

    private void initLogging(MethodExecutionContext context) {
        MethodExecutionInput executionInput = context.getInput();
        DBDebuggerType debuggerType = context.getDebuggerType();
        ExecutionOptions options = executionInput.getOptions();

        ConnectionHandler connection = context.getTargetConnection();
        DBNConnection conn = context.getConnection();

        DatabaseLoggingManager loggingManager = DatabaseLoggingManager.getInstance(getProject());
        boolean logging =
                debuggerType != DBDebuggerType.JDBC &&
                options.is(ExecutionOption.ENABLE_LOGGING) &&
                loggingManager.supportsLogging(connection) &&
                loggingManager.enableLogger(connection, conn);                ;

        context.setLogging(logging);
    }

    private void initParameters(MethodExecutionContext context) throws SQLException {
        MethodExecutionInput executionInput = context.getInput();
        DBNPreparedStatement statement = context.getStatement();
        bindParameters(executionInput, statement);
    }

    private void initTimeout(MethodExecutionContext context) throws SQLException {
        MethodExecutionInput executionInput = context.getInput();
        DBDebuggerType debuggerType = context.getDebuggerType();
        int timeout = debuggerType.isDebug() ?
                executionInput.getDebugExecutionTimeout() :
                executionInput.getExecutionTimeout();

        context.setTimeout(timeout);
        context.getStatement().setQueryTimeout(timeout);

    }

    @NotNull
    private MethodExecutionResult execute(MethodExecutionContext context) throws SQLException {
        ConnectionHandler connection = nd(context.getTargetConnection());
        DBNConnection conn = context.getConnection();

        MethodExecutionResult executionResult = new CancellableDatabaseCall<MethodExecutionResult>(
                connection,
                conn,
                context.getTimeout(),
                TimeUnit.SECONDS) {

            @Override
            public MethodExecutionResult execute() throws SQLException {
                return executeStatement(context, getConnection());
            }

            @Override
            public void cancel() {
                Resources.cancel(context.getStatement());
            }
        }.start();

        MethodExecutionInput executionInput = context.getInput();
        SessionId targetSessionId = executionInput.getTargetSessionId();
        if (targetSessionId != SessionId.POOL) conn.notifyDataChanges(getMethod().getVirtualFile());

        return executionResult;
    }

    @Nullable
    private MethodExecutionResult executeStatement(MethodExecutionContext context, ConnectionHandler connection) throws SQLException {
        DBNPreparedStatement statement = context.getStatement();
        statement.execute();

        MethodExecutionInput executionInput = context.getInput();
        MethodExecutionResult executionResult = executionInput.getExecutionResult();
        if (executionResult != null) {
            loadValues(executionResult, statement);
            executionResult.calculateExecDuration();

            if (context.isLogging()) {
                DatabaseLoggingManager loggingManager = DatabaseLoggingManager.getInstance(context.getProject());
                String logOutput = loggingManager.readLoggerOutput(connection, context.getConnection());
                executionResult.setLogOutput(logOutput);
            }
        }
        return executionResult;
    }

    @NotNull
    private ConnectionHandler getConnection() {
        return getMethod().getConnection();
    }

    protected boolean isQuery() {
        return false;
    }

    protected void bindParameters(MethodExecutionInput executionInput, PreparedStatement preparedStatement) throws SQLException {
        for (DBArgument argument : getArguments()) {
            DBDataType dataType = argument.getDataType();
            if (argument.isInput()) {
                String stringValue = executionInput.getInputValue(argument);
                setParameterValue(preparedStatement, argument.getPosition(), dataType, stringValue);
            }
            if (argument.isOutput() && preparedStatement instanceof CallableStatement) {
                CallableStatement callableStatement = (CallableStatement) preparedStatement;
                callableStatement.registerOutParameter(argument.getPosition(), dataType.getSqlType());
            }
        }
    }

    public void loadValues(MethodExecutionResult executionResult, DBNPreparedStatement<?> preparedStatement) throws SQLException {
        for (DBArgument argument : getArguments()) {
            if (argument.isOutput() && preparedStatement instanceof CallableStatement) {
                CallableStatement callableStatement = (CallableStatement) preparedStatement;
                Object result = callableStatement.getObject(argument.getPosition());
                executionResult.addArgumentValue(argument, result);
            }
        }
    }

    private Project getProject() {
        DBMethod method = getMethod();
        return method.getProject();
    }

    protected void setParameterValue(PreparedStatement preparedStatement, int parameterIndex, DBDataType dataType, String stringValue) throws SQLException {
        try {
            Object value = null;
            if (Strings.isNotEmptyOrSpaces(stringValue))  {
                Formatter formatter = Formatter.getInstance(getProject());
                value = formatter.parseObject(dataType.getTypeClass(), stringValue);
                value = dataType.getNativeType().getDefinition().convert(value);
            }
            dataType.setValueToPreparedStatement(preparedStatement, parameterIndex, value);

        } catch (SQLException e) {
            conditionallyLog(e);
            throw e;
        }  catch (Exception e) {
            conditionallyLog(e);
            throw new SQLException("Invalid value for data type " + dataType.getName() + " provided: \"" + stringValue + "\"");
        }
    }

    public abstract String buildExecutionCommand(MethodExecutionInput executionInput) throws SQLException;
}
