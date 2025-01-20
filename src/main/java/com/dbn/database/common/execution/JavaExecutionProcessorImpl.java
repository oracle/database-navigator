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

import com.dbn.common.template.TemplateUtilities;
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
import com.dbn.execution.java.wrapper.JavaComplexType;
import com.dbn.execution.java.wrapper.WrapperBuilder;
import com.dbn.execution.java.wrapper.SqlComplexType;
import com.dbn.execution.java.wrapper.Wrapper;
import com.dbn.execution.java.wrapper.TypeMappingsManager;
import com.dbn.execution.logging.DatabaseLoggingManager;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.DBOrderedObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.exception.Exceptions.toSqlException;
import static com.dbn.common.load.ProgressMonitor.setProgressDetail;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public abstract class JavaExecutionProcessorImpl implements JavaExecutionProcessor {
	private final DBObjectRef<DBJavaMethod> method;
	Set<String> addedTypes = new HashSet<>();

	protected JavaExecutionProcessorImpl(DBJavaMethod method) {
		this.method = DBObjectRef.of(method);
	}

	@Override
	@NotNull
	public DBJavaMethod getMethod() {
		return DBObjectRef.ensure(method);
	}

	public List<DBJavaParameter> getArguments() {
		DBJavaMethod method = getMethod();
		List<DBJavaParameter> parameter = method.getParameters();
		parameter.sort(Comparator.comparingInt(DBOrderedObject::getPosition));
		return parameter;
	}

	protected int getArgumentsCount() {
		return getArguments().size();
	}

	protected String getReturnArgument() {
		DBJavaMethod method = getMethod();
		return method.getReturnType();
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
			Wrapper wrapper = WrapperBuilder.getInstance().build(getMethod());

			try {
				// create java wrapper
				setProgressDetail("Initializing java execution environment");
				initCreateWrapperCommand(context, wrapper);
				initTimeout(context);
				execute(context);

				// call java wrapper
				setProgressDetail("Executing java method");
				initCommand(context, wrapper);
				initLogging(context);
				initTimeout(context);
				if (isQuery())
					initParameters(context, wrapper);
				execute(context);

			} finally {
				// drop java wrapper
				setProgressDetail("Releasing java execution environment");
				initDropWrapperCommand(context, wrapper);
				initTimeout(context);
				execute(context);
			}
		} catch (SQLException e) {
			conditionallyLog(e);
			Resources.cancel(context.getStatement());
			throw e;
		} catch (Exception e) {
			conditionallyLog(e);
			throw toSqlException(e);
		} finally {
			release(context);
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

	private List<String> createSQLTypes(Wrapper wrapper) {
		List<String> sqlTypes = new ArrayList<>();
		Properties properties = new Properties();

		addedTypes.clear();

		for (JavaComplexType jct : wrapper.getArgumentJavaComplexTypes()) {
			SqlComplexType sct = jct.getCorrespondingSqlType();
			if (addedTypes.contains(sct.getName()))
				continue;
			addedTypes.add(sct.getName());

			String fields = sct.getFields().stream()
					.sorted(Comparator.comparingInt(SqlComplexType.Field::getFieldIndex))
					.map(e -> e.getName() + " " + e.getType())
					.collect(Collectors.joining(",\n\t"));

			properties.setProperty("TYPENAME", sct.getName());
			properties.setProperty("FIELDS", fields);
			properties.setProperty("IS_ARRAY", String.valueOf(sct.isArray()));
			if (sct.getContainedTypeName() != null)
				properties.setProperty("ARRAY_TYPE", sct.getContainedTypeName());

			String code = generateCode("DBN - OJVM SQLType.sql", properties);
			sqlTypes.add(code);
		}
		return sqlTypes;
	}

	private List<String> createSQLToJava(Wrapper wrapper) {
		List<String> javaMethods = new ArrayList<>();
		Set<String> addedJavaTypes = new HashSet<>();

		AtomicInteger idx = new AtomicInteger(0);
		for (JavaComplexType jct : wrapper.getArgumentJavaComplexTypes()) {
			if (jct.getAttributeDirection() == JavaComplexType.AttributeDirection.RETURN) continue;

			if (addedJavaTypes.contains(jct.getCorrespondingSqlType().getName()))
				continue;
			addedJavaTypes.add(jct.getCorrespondingSqlType().getName());

			Properties properties = new Properties();

			properties.setProperty("JAVA_COMPLEX_TYPE", WrapperBuilder.convertClassNameToDotNotation(jct.getTypeName()));
			String code;
			if (jct.isArray()) {
				properties.setProperty("SQL_OBJECT_TYPE", jct.getCorrespondingSqlType().getName());
				if(jct.getFields().isEmpty()){
					properties.setProperty("TYPECAST_START", TypeMappingsManager.getCorrespondingSqlType(jct.getTypeName()).getTransformerPrefix());
					properties.setProperty("TYPECAST_END", TypeMappingsManager.getCorrespondingSqlType(jct.getTypeName()).getTransformerSuffix());
				} else {
					properties.setProperty("TYPECAST_START", jct.getFields().get(0).getTypeCastStart());
					properties.setProperty("TYPECAST_END", jct.getFields().get(0).getTypeCastEnd());
				}
				code = generateCode("DBN - OJVM SQLArrayToJava.java", properties);
			} else {
				properties.setProperty("SQL_OBJECT_TYPE", jct.getCorrespondingSqlType().getName());
				properties.setProperty("WRAPPER_METHOD_SIGNATURE", "java.sql.Struct arg" + idx.getAndIncrement());

				String allFieldsCsv = "";
				if (jct.getFields() != null)
					allFieldsCsv = jct.getFields()
							.stream()
							.map(e -> {
								String setterMethod = e.getSetter() ;
								String end;
								if(setterMethod == null || setterMethod.isEmpty()){
									setterMethod = e.getName() + " = ";
									end = " ";
								} else {
									setterMethod += "(";
									end = ")";
								}
								if (e.isComplexType()) {
									return setterMethod + ";" + e.getType() + "toJava( (java.sql.Struct) objArray[ " + e.getFieldIndex() + " ]" + ")" + ";" + end;
								}
								return setterMethod + ";" + e.getTypeCastStart() + " objArray[ " + e.getFieldIndex() + " ]" + e.getTypeCastEnd() + ";" + end;
							})
							.collect(Collectors.joining(","));

				properties.setProperty("FIELDS", allFieldsCsv);
				code = generateCode("DBN - OJVM SQLObjectToJava.java", properties);
			}
			javaMethods.add(code);
		}
		return javaMethods;
	}

	private List<String> createJavaToSQL(Wrapper wrapper) {
		List<String> javaMethods = new ArrayList<>();
		if (wrapper.getReturnType() == null) return javaMethods;

		boolean isComplexReturnType = wrapper.getReturnType().isComplexType();
		if (!isComplexReturnType) return javaMethods;

		Properties properties = new Properties();

		for (JavaComplexType jct : wrapper.getArgumentJavaComplexTypes()) {
			if (jct.getAttributeDirection() == JavaComplexType.AttributeDirection.ARGUMENT) continue;
			properties.setProperty("JAVA_COMPLEX_TYPE", WrapperBuilder.convertClassNameToDotNotation(jct.getTypeName()));
			properties.setProperty("SQL_OBJECT_TYPE", jct.getCorrespondingSqlType().getName());

			String code;
			if (wrapper.getReturnType().isArray()) {
				code = generateCode("DBN - OJVM JavaArrayToSQL.java", properties);
			} else {
				properties.setProperty("TOTAL_FIELDS", String.valueOf(jct.getFields().size()));
				String allFieldsCsv = jct.getFields()
						.stream()
						.map(e -> {
							String getterMethod = e.getGetter();
							if(getterMethod == null || getterMethod.isEmpty()){
								getterMethod = e.getName();
							} else {
								getterMethod += "()";
							}
							if (e.isComplexType()) {
								return e.getFieldIndex() + ";" + getterMethod + ";" + e.getType();
							}
							return e.getFieldIndex() + ";" + getterMethod + ";" + " ";
						})
						.collect(Collectors.joining(","));
				properties.setProperty("FIELDS", allFieldsCsv);
				code = generateCode("DBN - OJVM JavaObjectToSQL.java", properties);
			}

			javaMethods.add(code);
		}
		return javaMethods;
	}

	private String createJavaWrapper(Wrapper wrapper) {
		List<String> sqlMethods = createSQLToJava(wrapper);
		List<String> javaMethods = createJavaToSQL(wrapper);

		Properties properties = new Properties();

		properties.setProperty("SQL_CONVERSION_METHOD", String.join("@", sqlMethods));
		properties.setProperty("JAVA_CONVERSION_METHOD", String.join("@", javaMethods));

		properties.setProperty("JAVA_CLASS", wrapper.getFullyQualifiedClassName());
		properties.setProperty("JAVA_METHOD", wrapper.getWrappedJavaMethodName());

		AtomicInteger idx = new AtomicInteger(0);
		String javaSignature = wrapper.getJavaSignature(true);
		properties.setProperty("WRAPPER_METHOD_SIGNATURE", javaSignature);

		String sqlTypeToJavaType = wrapper.getMethodArguments()
				.stream()
				.map(e -> {
					if (e.isArray()) {
						return e.getTypeName() + "[]" + ";" + e.getCorrespondingSqlTypeName() + ";" + "arg" + idx.getAndIncrement();
					} else if (e.isComplexType()) {
						return e.getTypeName() + ";" + e.getCorrespondingSqlTypeName() + ";" + "arg" + idx.getAndIncrement();
					} else {
						idx.getAndIncrement();
						return "";
					}
				})
				.collect(Collectors.joining(","));

		idx.set(0);
		String callArgs = wrapper.getMethodArguments()
				.stream()
				.map(e -> {
					if (e.isComplexType()) {
						return "java_" + "arg" + idx.getAndIncrement();
					} else {
						return "arg" + idx.getAndIncrement();
					}
				})
				.collect(Collectors.joining(", "));

		properties.setProperty("CONVERT_OBJECTS", sqlTypeToJavaType);
		properties.setProperty("CALL_ARGS", callArgs);

		boolean isArrayReturn = false;
		boolean isComplexReturnType = false;
		String returnType = "";
		String returnConversionMethod = "";
		String arrayConversionMethod = "";
		if (wrapper.getReturnType() != null) {
			isComplexReturnType = wrapper.getReturnType().isComplexType();
			if (wrapper.getReturnType().isArray()) {
				isArrayReturn = true;
				returnType = "java.sql.Array";
				arrayConversionMethod = wrapper.getReturnType().getCorrespondingSqlTypeName();
			} else if (isComplexReturnType) {
				returnType = "java.sql.Struct";
				returnConversionMethod = wrapper.getReturnType().getCorrespondingSqlTypeName();
			} else {
				returnType = wrapper.getReturnType().getTypeName();
			}
		}

		properties.setProperty("METHOD_RETURN_TYPE", returnType);
		properties.setProperty("IS_ARRAY_RETURN", String.valueOf(isArrayReturn));
		properties.setProperty("ARRAY_RETURN_JAVA_CONVERSION", arrayConversionMethod);
		properties.setProperty("IS_COMPLEX_RETURN", String.valueOf(isComplexReturnType));
		properties.setProperty("RETURN_JAVA_CONVERSION", returnConversionMethod);

		return generateCode("DBN - OJVM JavaWrapper.java", properties);
	}

	private String createSQLWrapper(Wrapper wrapper) {
		Properties properties = new Properties();
		boolean isFunction = wrapper.getReturnType() != null && wrapper.getReturnType().getTypeName() != null;
		properties.setProperty("TYPE", isFunction ? "FUNCTION" : "PROCEDURE");
		properties.setProperty("METHOD", wrapper.getWrappedJavaMethodName());

		AtomicInteger idx = new AtomicInteger(0);
		String sqlSignature = wrapper.getMethodArguments()
				.stream()
				.map(e -> "arg_" + idx.getAndIncrement() + " " + e.getCorrespondingSqlTypeName())
				.collect(Collectors.joining(", "));

		String javaSignature = wrapper.getJavaSignature(false);

		properties.setProperty("SQL_SIGNATURE", sqlSignature);
		properties.setProperty("RETURN", isFunction ? wrapper.getReturnType().getCorrespondingSqlTypeName() : "");

		properties.setProperty("JAVA_METHOD_ARGS", javaSignature);

		String methodReturnType = "";
		if (wrapper.getReturnType() != null) {
			if (wrapper.getReturnType().isArray())
				methodReturnType = "java.sql.Array";
			else if (wrapper.getReturnType().isComplexType())
				methodReturnType = "java.sql.Struct";
			else
				methodReturnType = wrapper.getReturnType().getTypeName();
		}
		properties.setProperty("JAVA_METHOD_RETURN", methodReturnType);

		return generateCode("DBN - OJVM SQLWrapper.sql", properties);
	}

	private void initCreateWrapperCommand(JavaExecutionContext context, Wrapper wrapper) throws SQLException {
		List<String> sqlTypes = createSQLTypes(wrapper);
		String javaCode = createJavaWrapper(wrapper);
		String sqlWrapper = createSQLWrapper(wrapper);

		String sqlCode = "BEGIN" + "\n";
		if (!sqlTypes.isEmpty()) {
			sqlCode += String.join("\n", sqlTypes);
			sqlCode += "\n";
		}

		if (!javaCode.isEmpty()) {
			sqlCode += javaCode;
			sqlCode += "\n";
		}

		sqlCode += sqlWrapper;
		sqlCode += "END;";

		DBNConnection conn = context.getConnection();
		DBNPreparedStatement<?> statement = conn.prepareCall(sqlCode);
		context.setStatement(statement);
	}

	private void initDropWrapperCommand(JavaExecutionContext context, Wrapper wrapper) throws SQLException {
		Properties properties = new Properties();
		DBNConnection conn = context.getConnection();

		boolean isFunction = wrapper.getReturnType() != null && wrapper.getReturnType().getTypeName() != null;
		properties.setProperty("TYPE", isFunction ? "FUNCTION" : "PROCEDURE");

		String allTypes = String.join(",", addedTypes);
		properties.setProperty("SQLTYPES", allTypes);

		String cleanup = generateCode("DBN - OJVM SQLCleanup.sql", properties);
		DBNPreparedStatement<?> statement = conn.prepareCall(cleanup);
		context.setStatement(statement);
	}

	private void initCommand(JavaExecutionContext context, Wrapper wrapper) throws SQLException {
		JavaExecutionInput executionInput = context.getInput();
		String command = buildExecutionCommand(executionInput, wrapper);
		DBNConnection conn = context.getConnection();
		DBNPreparedStatement<?> statement = isQuery() ?
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

	private void initParameters(JavaExecutionContext context, Wrapper wrapper) throws SQLException {
		JavaExecutionInput executionInput = context.getInput();
		DBNPreparedStatement<?> statement = context.getStatement();
		bindParameters(executionInput, statement, wrapper);
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

	private void execute(JavaExecutionContext context) throws SQLException {
		ConnectionHandler connection = nd(context.getTargetConnection());
		DBNConnection conn = context.getConnection();

		new CancellableDatabaseCall<JavaExecutionResult>(
				connection,
				conn,
				context.getTimeout(),
				TimeUnit.SECONDS) {

			@Override
			public JavaExecutionResult execute() throws SQLException {
				return executeStatement(context, getConnection());
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
	private JavaExecutionResult executeStatement(JavaExecutionContext context, ConnectionHandler connection) throws SQLException {
		DBNPreparedStatement<?> statement = context.getStatement();
		statement.execute();

		JavaExecutionInput executionInput = context.getInput();
		JavaExecutionResult executionResult = executionInput.getExecutionResult();
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
		return getArgumentsCount() > 0;
	}

	protected void bindParameters(JavaExecutionInput executionInput, DBNPreparedStatement<?> preparedStatement, Wrapper wrapper) {

	}

	public void loadValues(JavaExecutionResult executionResult, DBNPreparedStatement<?> preparedStatement) throws SQLException {
		for (DBJavaParameter argument : getArguments()) {
			if (preparedStatement instanceof CallableStatement) {
				CallableStatement callableStatement = (CallableStatement) preparedStatement;
				Object result = callableStatement.getObject(argument.getPosition());
				executionResult.addArgumentValue(argument, result);
			}
		}
	}

	private Project getProject() {
		DBJavaMethod method = getMethod();
		return method.getProject();
	}

	public abstract String buildExecutionCommand(JavaExecutionInput executionInput, Wrapper wrapper) throws SQLException;

	private String generateCode(String templateName, Properties properties) {
		return TemplateUtilities.generateCode(getProject(), templateName, properties);
	}
}
