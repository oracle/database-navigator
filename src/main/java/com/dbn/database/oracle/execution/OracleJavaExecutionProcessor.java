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

package com.dbn.database.oracle.execution;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.connection.SessionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNPreparedStatement;
import com.dbn.database.common.execution.JavaExecutionProcessorImpl;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.result.JavaExecutionResult;
import com.dbn.execution.java.wrapper.Wrapper;
import com.dbn.execution.java.wrapper.Wrapper.MethodAttribute;
import com.dbn.execution.java.wrapper.WrapperBuilder;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import static com.dbn.common.util.Lists.sortedCopy;
import static com.dbn.common.util.Strings.firstCharacter;
import static com.dbn.object.DBOrderedObject.POSITION_COMPARATOR;
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;

public class OracleJavaExecutionProcessor extends JavaExecutionProcessorImpl {

	private int sqlType = 0;
	public OracleJavaExecutionProcessor(DBJavaMethod method) {
		super(method);
	}

	protected void preHookExecutionCommand(StringBuilder buffer) {}
	protected void postHookExecutionCommand(StringBuilder buffer) {}

	@Override
	public String buildExecutionCommand(JavaExecutionInput executionInput, Wrapper wrapper) {
		String wrapperName = "DBN_OJVM_SQL_WRAPPER";

		String returnArgument = getReturnArgument();
		List<DBJavaParameter> arguments = getArguments();

		boolean isProcedure = returnArgument.equals("void");
		StringBuilder buffer = new StringBuilder();

		StringBuilder methodCallPrepare = new StringBuilder();

		for (DBJavaParameter argument : arguments) {
			methodCallPrepare.append("?");

			boolean isLast = arguments.indexOf(argument) == arguments.size() - 1;
			if (!isLast) {
				methodCallPrepare.append(", ");
			}
		}

		buffer.append("declare\n");
		if(!isProcedure){
			MethodAttribute returnType = wrapper.getReturnType();
			buffer.append("output_arg ")
					.append(returnType.getSqlTypeName())
					.append(returnType.getSqlDeclarationSuffix())
					.append(";")
					.append("\n");
		}
		buffer.append("begin \n");
		buffer.append("dbms_java.set_output(100000);\n");

		preHookExecutionCommand(buffer);

		if(isProcedure){
			buffer.append(wrapperName);
			if(!methodCallPrepare.toString().isEmpty()) {
				buffer.append("(")
					.append(methodCallPrepare)
					.append(");");
			}
			buffer.append("\n");
		} else {
			buffer.append("output_arg :=")
					.append(wrapperName)
					.append("(")
					.append(methodCallPrepare)
					.append(");\n");
			buffer.append("? := output_arg ;\n");
		}
		postHookExecutionCommand(buffer);
		buffer.append("end;\n");
		return buffer.toString();
	}

	@SneakyThrows
	@Override
	protected void bindParameters(JavaExecutionInput executionInput, PreparedStatement callableStatement, Wrapper wrapper) {
		// bind input variables
		int parameterIndex = 1;
		for (DBJavaParameter parameter : getArguments()) {

			String parameterName = parameter.getName();
			if (parameter.isArray()) {
				String objectName = wrapper.getMethodArguments().get(parameterIndex - 1).getSqlTypeName();
				Array arrObj = getArrayObject(executionInput, parameter.getJavaClass().getFields(), wrapper, objectName, parameterName);
				callableStatement.setArray(parameterIndex, arrObj);

			} else if (!parameter.isPlainValue()) { // TODO support pseudo-primitives com.dbn.object.type.DBJavaValueType
				String objectName = wrapper.getMethodArguments().get(parameterIndex - 1).getSqlTypeName();
				Object structObj = getStructObject(executionInput, parameter.getJavaClass().getFields(), wrapper, objectName, parameterName);
				callableStatement.setObject(parameterIndex, structObj);

			} else {
				String clazz = parameter.getJavaClassRef().getObjectName();
				String value = executionInput.getInputValue(parameterName);
				if (value == null) callableStatement.setObject(parameterIndex, null);
				else if (clazz.equals("String")) callableStatement.setString(parameterIndex, value);
				else if (clazz.equals("byte")) callableStatement.setByte(parameterIndex, Byte.parseByte(value));
				else if (clazz.equals("short")) callableStatement.setShort(parameterIndex, Short.parseShort(value));
				else if (clazz.equals("int")) callableStatement.setInt(parameterIndex, Integer.parseInt(value));
				else if (clazz.equals("long")) callableStatement.setLong(parameterIndex, Long.parseLong(value));
				else if (clazz.equals("float")) callableStatement.setFloat(parameterIndex, Float.parseFloat(value));
				else if (clazz.equals("double")) callableStatement.setDouble(parameterIndex, Double.parseDouble(value));
				else if (clazz.equals("boolean"))
					callableStatement.setBoolean(parameterIndex, Boolean.getBoolean(value));
				else
					callableStatement.setObject(parameterIndex, value);

			}
			parameterIndex++;
		}
		String returnArgument = getReturnArgument();
		boolean isProcedure = returnArgument.equals("void");
		if(!isProcedure) {
			sqlType = getSQLTypes(returnArgument);
			if(sqlType == Types.STRUCT) {
				String returnTypeSQL = wrapper.getReturnType().getSqlTypeName();
				((CallableStatement) callableStatement).registerOutParameter(parameterIndex, sqlType, returnTypeSQL);
			} else
				((CallableStatement) callableStatement).registerOutParameter(parameterIndex, sqlType);
		}
	}

	@Override
	public void loadValues(JavaExecutionResult executionResult, DBNPreparedStatement<?> preparedStatement) throws SQLException {
		if (preparedStatement instanceof CallableStatement) {
			int outputIndex = getArgumentsCount() + 1;
			CallableStatement callableStatement = (CallableStatement) preparedStatement;
			Object result = getResult(callableStatement, outputIndex);
			executionResult.addArgumentValue(getReturnArgument(), result);
		}
	}

	@SneakyThrows
	private Object getStructObject(JavaExecutionInput executionInput, List<DBJavaField> fields, Wrapper wrapper, String objectName, String fieldPath){
		ConnectionHandler connection = getMethod().getConnection();
		SessionId targetSessionId = executionInput.getTargetSessionId();
		SchemaId targetSchemaId = executionInput.getTargetSchemaId();
		DBNConnection conn = connection.getConnection(targetSessionId, targetSchemaId);

		fields = sortedCopy(fields, POSITION_COMPARATOR);
		Object[] customTypeAttributes = new Object[fields.size()];
		int i = 0;
		for (DBJavaField field : fields) {
			String newFieldPath = fieldPath + "." + field.getName();

			String value = executionInput.getInputValue(newFieldPath);
			customTypeAttributes[i] = parseValue(executionInput, wrapper, field, newFieldPath, value);
			i++;
		}

		ClassLoader cl =  conn.getInner().getClass().getClassLoader();
		Class<?> structDescriptorClass = Class.forName("oracle.sql.StructDescriptor",true, cl);
		Method createDescriptorMethod = structDescriptorClass.getMethod("createDescriptor", String.class, Connection.class);
		Object structDescriptor =  createDescriptorMethod.invoke(null, objectName, conn.getInner());

		Class<?> structClass = Class.forName("oracle.sql.STRUCT", true, cl);
		Constructor<?> structCtr = structClass.getConstructor(structDescriptorClass, Connection.class, Object[].class);

		return structCtr.newInstance(structDescriptor, conn.getInner(), customTypeAttributes);
	}

	@SneakyThrows
	private Array getArrayObject(JavaExecutionInput executionInput, List<DBJavaField> fields, Wrapper wrapper, String objectName, String fieldPath){
		ConnectionHandler connection = getMethod().getConnection();
		SessionId targetSessionId = executionInput.getTargetSessionId();
		SchemaId targetSchemaId = executionInput.getTargetSchemaId();
		DBNConnection conn = connection.getConnection(targetSessionId, targetSchemaId);

		fields = sortedCopy(fields, POSITION_COMPARATOR);
		Object[] customTypeAttributes = new Object[fields.size()];
		int i = 0;
		for (DBJavaField field : fields) {
			String value = executionInput.getInputValue(fieldPath);
			customTypeAttributes[i] = parseValue(executionInput, wrapper, field, fieldPath, value);
			i++;
		}

		ClassLoader cl =  conn.getInner().getClass().getClassLoader();
		Class<?> structDescriptorClass = Class.forName("oracle.sql.ArrayDescriptor",true, cl);
		Method createDescriptorMethod = structDescriptorClass.getMethod("createDescriptor", String.class, Connection.class);
		Object structDescriptor =  createDescriptorMethod.invoke(null, objectName, conn.getInner());

		Class<?> structClass = Class.forName("oracle.sql.ARRAY", true, cl);
		Constructor<?> structCtr = structClass.getConstructor(structDescriptorClass, Connection.class, Object[].class);

		return (Array) structCtr.newInstance(structDescriptor, conn.getInner(), customTypeAttributes);
	}

	@Nullable
	private Object parseValue(JavaExecutionInput executionInput, Wrapper wrapper, DBJavaField field, String fieldPath, String fieldValue) {
		if (field == null) return null;

		String className = getCanonicalName(field.getJavaClassRef());

		// TODO isolate this logic and safely handle null fieldValue
		switch (className) {
			// primitives
			case "boolean": return Boolean.parseBoolean(fieldValue);
			case "byte": return Byte.parseByte(fieldValue);
			case "char": return firstCharacter(fieldValue);
			case "double": return Double.parseDouble(fieldValue);
			case "float": return Float.parseFloat(fieldValue);
			case "int": return Integer.parseInt(fieldValue);
			case "long": return Long.parseLong(fieldValue);
			case "short": return Short.parseShort(fieldValue);

			// pseudo-primitives (prevent expensive class-details load from SYS schema)
			case "java.lang.Boolean": return Boolean.parseBoolean(fieldValue);
			case "java.lang.Byte": return Byte.parseByte(fieldValue);
			case "java.lang.Character": firstCharacter(fieldValue);
			case "java.lang.Double": return Double.parseDouble(fieldValue);
			case "java.lang.Float": return Float.parseFloat(fieldValue);
			case "java.lang.Integer": return Integer.parseInt(fieldValue);
			case "java.lang.Long": return Long.parseLong(fieldValue);
			case "java.lang.Short": return Short.parseShort(fieldValue);
			case "java.lang.String": return fieldValue;
			case "java.math.BigDecimal": return new BigDecimal(fieldValue);
			case "java.math.BigInteger": return new BigInteger(fieldValue);
			//...
			default:
				if (field.isClass()) {
					DBJavaClass javaClass = field.getJavaClass();
					int typeIndex = wrapper.getSqlTypeIndex(javaClass.getCanonicalName(), field.getArrayDepth());
					String innerObjectName = WrapperBuilder.DBN_TYPE_SUFFIX + typeIndex;
					return getStructObject(executionInput, javaClass.getFields(), wrapper, innerObjectName, fieldPath);
				}
				return fieldValue;
		}
	}

	private int getSQLTypes(String javaType) {
		switch (javaType) {
			case "int" : return Types.INTEGER;
			case "float" : return Types.FLOAT;
			case "double": return Types.DOUBLE;
			case "boolean": return Types.BIT;
			case "byte": return Types.TINYINT;
			case "short": return Types.SMALLINT;
			case "long": return Types.BIGINT;
			case "char": return Types.CHAR;
			case "String":
			case "java/lang/String":
			case "java.lang.String": return Types.VARCHAR;
			case "java.math.BigDecimal": return Types.DECIMAL;
			case "java.math.BigInteger": return Types.BIGINT;
			default: return Types.STRUCT;
		}
	}

	private Object getResult(CallableStatement callableStatement, int outputIndex) throws SQLException {
		switch (sqlType) {
			case Types.INTEGER: return callableStatement.getInt(outputIndex);
			case Types.FLOAT: return callableStatement.getFloat(outputIndex);
			case Types.DOUBLE: return callableStatement.getDouble(outputIndex);
			case Types.BIT: return callableStatement.getBoolean(outputIndex);
			case Types.TINYINT: return callableStatement.getByte(outputIndex);
			case Types.SMALLINT: return callableStatement.getShort(outputIndex);
			case Types.BIGINT: return callableStatement.getLong(outputIndex);
			case Types.CHAR: return callableStatement.getString(outputIndex).charAt(0);
			case Types.VARCHAR: return callableStatement.getString(outputIndex);
			case Types.DECIMAL: return callableStatement.getBigDecimal(outputIndex);
			default: return callableStatement.getObject(outputIndex);
		}
	}
}
