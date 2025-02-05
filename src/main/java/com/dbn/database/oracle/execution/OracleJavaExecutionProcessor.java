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
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

public class OracleJavaExecutionProcessor extends JavaExecutionProcessorImpl {


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
				String clazz = parameter.getJavaClassName();
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
	}

	@Override
	public void loadValues(JavaExecutionResult executionResult, DBNPreparedStatement<?> preparedStatement) throws SQLException {

	}

	@SneakyThrows
	private Object getStructObject(JavaExecutionInput executionInput, List<DBJavaField> fields, Wrapper wrapper, String objectName, String fieldPath){
		ConnectionHandler connection = getMethod().getConnection();
		SessionId targetSessionId = executionInput.getTargetSessionId();
		SchemaId targetSchemaId = executionInput.getTargetSchemaId();
		DBNConnection conn = connection.getConnection(targetSessionId, targetSchemaId);

		fields.sort(Comparator.comparingInt(DBJavaField::getIndex));
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

		fields.sort(Comparator.comparingInt(DBJavaField::getIndex));
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
		if (fieldValue == null) return null;

		switch(field.getJavaClassName()){
			case "int": return Integer.parseInt(fieldValue);
			case "float": return Float.parseFloat(fieldValue);
			case "double": return Double.parseDouble(fieldValue);
			case "byte": return Byte.parseByte(fieldValue);
			case "short": return Short.parseShort(fieldValue);
			case "long": return Long.parseLong(fieldValue);
			case "boolean": return Boolean.parseBoolean(fieldValue);
			case "class":
				DBJavaClass javaClass = field.getJavaClass();
				int typeIndex = wrapper.getSqlTypeIndex(javaClass.getCanonicalName(), field.getArrayDepth());
				String innerObjectName = WrapperBuilder.DBN_TYPE_SUFFIX + typeIndex;
				return getStructObject(executionInput, javaClass.getFields(), wrapper, innerObjectName, fieldPath);
			default:return fieldValue;
		}
	}
}
