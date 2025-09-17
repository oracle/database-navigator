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
 *
 */

package com.dbn.database.common.execution;

import com.dbn.common.thread.CancellableDatabaseCall;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.Resources;
import com.dbn.connection.SchemaId;
import com.dbn.connection.SessionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNPreparedStatement;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.ExecutionOption;
import com.dbn.execution.ExecutionOptions;
import com.dbn.execution.ExecutionStatus;
import com.dbn.execution.java.JavaExecutionContext;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.result.JavaExecutionResult;
import com.dbn.execution.java.wrapper.WrapperModel;
import com.dbn.execution.logging.DatabaseLoggingManager;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.common.load.ProgressMonitor.setProgressDetail;
import static com.dbn.common.util.Lists.sortedCopy;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.object.DBOrderedObject.POSITION_COMPARATOR;
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;

@Slf4j
public abstract class JavaExecutionProcessorImpl implements JavaExecutionProcessor {
	private final DBObjectRef<DBJavaMethod> method;

	public JavaExecutionProcessorImpl(DBJavaMethod method) {
		this.method = DBObjectRef.of(method);
	}

	@Override
	@NotNull
	public DBJavaMethod getMethod() {
		return DBObjectRef.ensure(method);
	}

	public List<DBJavaParameter> getArguments() {
		DBJavaMethod method = getMethod();
		List<DBJavaParameter> parameters = method.getParameters();
		parameters = sortedCopy(parameters, POSITION_COMPARATOR);
		return parameters;
	}

	protected int getArgumentsCount() {
		return getArgumentsCount(null);
	}

	protected  int getArgumentsCount(JavaExecutionContext context) {
		int excludedParamCount  = 0;
		if(context != null) {
			if(context.getWrapperModel().getInput().getJavaInjectedParameters() != null) {
				excludedParamCount = context.getWrapperModel().getInput()
													.getJavaInjectedParameters().size();
			}
		}
		return getArguments().size() - excludedParamCount;
	}

	protected String getReturnArgument() {
		DBJavaMethod method = getMethod();
		return getCanonicalName(method.getReturnClassRef());
	}

	protected boolean isProcedure() {
		return getMethod().isReturningVoid();
	}

	@Override
	public void execute(JavaExecutionInput executionInput, DBDebuggerType debuggerType) throws SQLException {
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
	public void execute(JavaExecutionInput executionInput, @NotNull DBNConnection conn, DBDebuggerType debuggerType) throws SQLException {
		JavaExecutionContext context = executionInput.initExecution(debuggerType);
		context.setConnection(conn);
		context.setDebuggerType(debuggerType);
		context.set(ExecutionStatus.EXECUTING, true);

		try {
			initExecutionWrappers(context);
			triggerExecution(context);

		} catch (SQLException e) {
			conditionallyLog(e);
			Resources.cancel(context.getStatement());
			throw e;
		} catch (Exception e) {
			conditionallyLog(e);
			throw toSqlException(e);
		} finally {
			releaseExecutionWrappers(context);
			release(context);
		}
	}

	public void initExecutionWrappers(JavaExecutionContext context) throws SQLException {
        // the debugger engine creates the wrappers before triggering the method execution
        // (to allow breakpoints to be set on the wrapper methods)
        if (context.getDebuggerType() == DBDebuggerType.JDWP) return;

        // create java wrapper
        setProgressDetail("Initializing java execution environment");
        context.createExecutionWrappers();
	}

	private void triggerExecution(JavaExecutionContext context) throws SQLException {
		// call java wrapper
		setProgressDetail("Executing java method");
		initCommand(context);
		initLogging(context);
		initTimeout(context);
		initParameters(context);
		if (isQuery(context)) {
			boolean hasReturnType = isReturnType();
			execute(context, hasReturnType);
		} else {
			execute(context, false);
		}
	}

	private void releaseExecutionWrappers(JavaExecutionContext context) {
		try {
			// drop java wrapper
			setProgressDetail("Releasing java execution environment");
			context.discardExecutionWrappers();
		} catch (ProcessCanceledException e) {
			conditionallyLog(e);
		} catch (Throwable t) {
			log.warn("Error cleaning up java wrappers", t);
			// do not propagate exception to the surrounding block
		}
	}

	private void release(JavaExecutionContext context) {
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

		Resources.close(conn);

		if (conn.isPoolConnection()) {
			connection.freePoolConnection(conn);
		}
	}

	private void initCommand(JavaExecutionContext context) throws SQLException {
		WrapperModel wrapperModel = context.getWrapperModel();
		JavaExecutionInput executionInput = context.getInput();
		String command = buildExecutionCommand(executionInput, wrapperModel);
		DBNConnection conn = context.getConnection();
		DBNPreparedStatement<?> statement = !isQuery(context) ?
				conn.prepareStatement(command) :
				conn.prepareCall(command);

		context.setStatement(statement);
	}

	private void initLogging(JavaExecutionContext context) {
		JavaExecutionInput executionInput = context.getInput();
		DBDebuggerType debuggerType = context.getDebuggerType();
		ExecutionOptions options = executionInput.getOptions();

		ConnectionHandler connection = context.getTargetConnection();
		DBNConnection conn = context.getConnection();

		DatabaseLoggingManager loggingManager = DatabaseLoggingManager.getInstance(getProject());
		boolean logging =
				debuggerType != DBDebuggerType.JDBC &&
						options.is(ExecutionOption.ENABLE_LOGGING) &&
						loggingManager.supportsLogging(connection) &&
						loggingManager.enableLogger(connection, conn);

		context.setLogging(logging);
	}

	private void initParameters(JavaExecutionContext context) {
		if (!isQuery(context)) return;

		WrapperModel wrapperModel = context.getWrapperModel();
		JavaExecutionInput executionInput = context.getInput();
		DBNPreparedStatement statement = context.getStatement();
		bindParameters(executionInput, statement, wrapperModel);
	}

	private void initTimeout(JavaExecutionContext context) throws SQLException {
		JavaExecutionInput executionInput = context.getInput();
		DBDebuggerType debuggerType = context.getDebuggerType();
		int timeout = debuggerType.isDebug() ?
				executionInput.getDebugExecutionTimeout() :
				executionInput.getExecutionTimeout();

		context.setTimeout(timeout);
		context.getStatement().setQueryTimeout(timeout);

	}

	private void execute(JavaExecutionContext context, boolean catchResult) throws SQLException {
		ConnectionHandler connection = nd(context.getTargetConnection());
		DBNConnection conn = context.getConnection();

		new CancellableDatabaseCall<JavaExecutionResult>(
				connection,
				conn,
				context.getTimeout(),
				TimeUnit.SECONDS) {

			@Override
			public JavaExecutionResult execute() throws SQLException {
				return executeStatement(context, getConnection(), catchResult);
			}

			@Override
			public void cancel() {
				Resources.cancel(context.getStatement());
			}
		}.start();

		JavaExecutionInput executionInput = context.getInput();
		SessionId targetSessionId = executionInput.getTargetSessionId();
		if (targetSessionId != SessionId.POOL) conn.notifyDataChanges(getMethod().getVirtualFile());

	}

	@Nullable
	private JavaExecutionResult executeStatement(JavaExecutionContext context, ConnectionHandler connection, boolean catchResult) throws SQLException {
		DBNPreparedStatement<?> statement = context.getStatement();
		statement.execute();

		JavaExecutionInput executionInput = context.getInput();
		JavaExecutionResult executionResult = executionInput.getExecutionResult();
		if (executionResult != null) {
			if(catchResult)
				loadValues(context, executionResult, statement);
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
		return isQuery(null);
	}

	protected boolean isQuery(JavaExecutionContext context) {
		return getArgumentsCount(context) > 0 || isReturnType();
	}

	private boolean isReturnType(){
		return !getMethod().getSignature().split(":")[1].trim().equals("void");
	}

	protected void bindParameters(JavaExecutionInput executionInput, PreparedStatement preparedStatement, WrapperModel wrapperModel) {

	}

	public void loadValues(JavaExecutionContext context, JavaExecutionResult executionResult, DBNPreparedStatement<?> preparedStatement) throws SQLException {

	}

	private Project getProject() {
		DBJavaMethod method = getMethod();
		return method.getProject();
	}

	public abstract String buildExecutionCommand(JavaExecutionInput executionInput, WrapperModel wrapperModel) throws SQLException;
}
