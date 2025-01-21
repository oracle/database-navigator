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
import com.dbn.execution.java.wrapper.WrapperBuilder;
import com.dbn.execution.java.wrapper.Wrapper;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.Connection;
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
		if(! isProcedure){
			buffer.append("output_arg ")
					.append(wrapper.getReturnType().getCorrespondingSqlTypeName())
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
	protected void bindParameters(JavaExecutionInput executionInput, DBNPreparedStatement<?> callableStatement, Wrapper wrapper) {

		// bind input variables
		int parameterIndex = 1;
		for (DBJavaParameter argument : getArguments()) {

			String clazz = argument.getParameterType();
			boolean isArray = argument.getArrayDepth() > 0;
			if(isArray){
				String objectName = wrapper.getMethodArguments().get(parameterIndex- 1).getCorrespondingSqlTypeName();
				Array arrObj = getArrayObject(executionInput, argument.getParameterClass().getFields(), wrapper, objectName);
				callableStatement.setArray(parameterIndex,  arrObj);

			}else if(clazz.equals("class")) {
				String objectName = wrapper.getMethodArguments().get(parameterIndex- 1).getCorrespondingSqlTypeName();
				Object structObj = getStructObject(executionInput, argument.getParameterClass().getFields(), wrapper, objectName);
				callableStatement.setObject(parameterIndex, structObj);

			} else {
				String value = executionInput.getInputValue(argument);
				if (clazz.equals("String")) callableStatement.setString(parameterIndex, value);
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
	private Object getStructObject(JavaExecutionInput executionInput, List<DBJavaField> fields, Wrapper wrapper, String objectName){
		ConnectionHandler connection = getMethod().getConnection();
		SessionId targetSessionId = executionInput.getTargetSessionId();
		SchemaId targetSchemaId = executionInput.getTargetSchemaId();
		DBNConnection conn = connection.getConnection(targetSessionId, targetSchemaId);

		fields.sort(Comparator.comparingInt(DBJavaField::getIndex));
		Object[] customTypeAttributes = new Object[fields.size()];
		int i = 0;
		for (DBJavaField field : fields) {
			String value = executionInput.getInputValue(field);
			switch(field.getType()){
				case "int":
					customTypeAttributes[i] = Integer.parseInt(value);
					break;
				case "float":
					customTypeAttributes[i] = Float.parseFloat(value);
					break;
				case "double":
					customTypeAttributes[i] = Double.parseDouble(value);
					break;
				case "byte":
					customTypeAttributes[i] = Byte.parseByte(value);
					break;
				case "short":
					customTypeAttributes[i] = Short.parseShort(value);
					break;
				case "long":
					customTypeAttributes[i] = Long.parseLong(value);
					break;
				case "boolean":
					customTypeAttributes[i] = Boolean.parseBoolean(value);
					break;
				case "class":
					WrapperBuilder.ComplexTypeKey key = new WrapperBuilder.ComplexTypeKey(field.getFieldClass().getName(), field.getArrayDepth());
					int typeNumber = wrapper.getComplexTypeConversion().get(key);
					String innerObjectName = WrapperBuilder.newSqlTypePrepend + typeNumber;
					customTypeAttributes[i] = getStructObject(executionInput, field.getFieldClass().getFields(), wrapper, innerObjectName);
					break;
				default:
					customTypeAttributes[i] = value;
			}
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
	private Array getArrayObject(JavaExecutionInput executionInput, List<DBJavaField> fields, Wrapper wrapper, String objectName){
		ConnectionHandler connection = getMethod().getConnection();
		SessionId targetSessionId = executionInput.getTargetSessionId();
		SchemaId targetSchemaId = executionInput.getTargetSchemaId();
		DBNConnection conn = connection.getConnection(targetSessionId, targetSchemaId);

		fields.sort(Comparator.comparingInt(DBJavaField::getIndex));
		Object[] customTypeAttributes = new Object[fields.size()];
		int i = 0;
		for (DBJavaField field : fields) {
			String value = executionInput.getInputValue(field);
			switch(field.getType()){
				case "int":
					customTypeAttributes[i] = Integer.parseInt(value);
					break;
				case "float":
					customTypeAttributes[i] = Float.parseFloat(value);
					break;
				case "double":
					customTypeAttributes[i] = Double.parseDouble(value);
					break;
				case "byte":
					customTypeAttributes[i] = Byte.parseByte(value);
					break;
				case "short":
					customTypeAttributes[i] = Short.parseShort(value);
					break;
				case "long":
					customTypeAttributes[i] = Long.parseLong(value);
					break;
				case "boolean":
					customTypeAttributes[i] = Boolean.parseBoolean(value);
					break;
				case "class":
					WrapperBuilder.ComplexTypeKey key = new WrapperBuilder.ComplexTypeKey(field.getFieldClass().getName(), field.getArrayDepth());
					int typeNumber = wrapper.getComplexTypeConversion().get(key);
					String innerObjectName = WrapperBuilder.newSqlTypePrepend + typeNumber;
					customTypeAttributes[i] = getStructObject(executionInput, field.getFieldClass().getFields(), wrapper, innerObjectName);
					break;
				default:
					customTypeAttributes[i] = value;
			}
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

}
