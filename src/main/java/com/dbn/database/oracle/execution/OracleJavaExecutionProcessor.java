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
import com.dbn.database.oracle.OracleTypes;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.result.JavaExecutionResult;
import com.dbn.execution.java.wrapper.Wrapper;
import com.dbn.execution.java.wrapper.model.ClassWrapper;
import com.dbn.execution.java.wrapper.model.MethodWrapper;
import com.dbn.execution.java.wrapper.model.ParameterWrapper;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.data.Data.asBigDecimal;
import static com.dbn.common.data.Data.asBigInteger;
import static com.dbn.common.data.Data.asBoolean;
import static com.dbn.common.data.Data.asBooleanPrimitive;
import static com.dbn.common.data.Data.asByte;
import static com.dbn.common.data.Data.asBytePrimitive;
import static com.dbn.common.data.Data.asCharacter;
import static com.dbn.common.data.Data.asCharacterPrimitive;
import static com.dbn.common.data.Data.asDouble;
import static com.dbn.common.data.Data.asDoublePrimitive;
import static com.dbn.common.data.Data.asFloat;
import static com.dbn.common.data.Data.asFloatPrimitive;
import static com.dbn.common.data.Data.asInteger;
import static com.dbn.common.data.Data.asIntegerPrimitive;
import static com.dbn.common.data.Data.asLong;
import static com.dbn.common.data.Data.asLongPrimitive;
import static com.dbn.common.data.Data.asShort;
import static com.dbn.common.data.Data.asShortPrimitive;
import static com.dbn.common.util.Lists.sortedCopy;
import static com.dbn.object.DBOrderedObject.POSITION_COMPARATOR;
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;

public class OracleJavaExecutionProcessor extends JavaExecutionProcessorImpl {

	private int sqlType = 0;
	public OracleJavaExecutionProcessor(DBJavaMethod method) {
		super(method);
	}

	@Override
	public String buildExecutionCommand(JavaExecutionInput executionInput, Wrapper wrapper) {
		boolean procedure = isProcedure();
		String wrapperName = wrapper.getSqlWrapperName();
		List<DBJavaParameter> arguments = getArguments();

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

		if (!procedure) {
			ParameterWrapper returnParameter = wrapper.getMethods().get(0).getReturnParameter();
			buffer.append("output_arg ")
					.append(returnParameter.getSqlTypeName())
					.append(returnParameter.getSqlDeclarationSuffix())
					.append(";")
					.append("\n");
		}
		buffer.append("begin \n");
		buffer.append("dbms_java.set_output(100000);\n");

		if(procedure){
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
		buffer.append("end;\n");
		return buffer.toString();
	}

	@SneakyThrows
	@Override
	protected void bindParameters(JavaExecutionInput executionInput, PreparedStatement statement, Wrapper wrapper) {
		// bind input variables
		int parameterIndex = 1;
		MethodWrapper methodWrapper = wrapper.getMethods().get(0);
		for (DBJavaParameter parameter : getArguments()) {

			String parameterName = parameter.getName();
			if (parameter.isArray()) {
				String objectName = methodWrapper.getParameters().get(parameterIndex - 1).getSqlTypeName();
				Array arrObj = getArrayObject(executionInput, parameter.getJavaClass().getFields(), wrapper, objectName, parameterName);
				statement.setArray(parameterIndex, arrObj);

			} else if (!parameter.isScalar()) { // TODO support pseudo-primitives com.dbn.object.type.DBJavaValueType
				String objectName = methodWrapper.getParameters().get(parameterIndex - 1).getSqlTypeName();
				Object structObj = getStructObject(executionInput, parameter.getJavaClass().getFields(), wrapper, objectName, parameterName);
				statement.setObject(parameterIndex, structObj);

			} else {
				String clazz = parameter.getJavaClassRef().getObjectName();
				String value = executionInput.getInputValue(parameterName);
				if (value == null) statement.setObject(parameterIndex, null);
				else if (clazz.equals("String")) statement.setString(parameterIndex, value);
				else if (clazz.equals("byte")) statement.setByte(parameterIndex, Byte.parseByte(value));
				else if (clazz.equals("short")) statement.setShort(parameterIndex, Short.parseShort(value));
				else if (clazz.equals("int")) statement.setInt(parameterIndex, Integer.parseInt(value));
				else if (clazz.equals("long")) statement.setLong(parameterIndex, Long.parseLong(value));
				else if (clazz.equals("float")) statement.setFloat(parameterIndex, Float.parseFloat(value));
				else if (clazz.equals("double")) statement.setDouble(parameterIndex, Double.parseDouble(value));
				else if (clazz.equals("boolean"))
					statement.setBoolean(parameterIndex, Boolean.getBoolean(value));
				else
					statement.setObject(parameterIndex, value);

			}
			parameterIndex++;
		}
		String returnArgument = getReturnArgument();
        if (!isProcedure()) {
            if (methodWrapper.getReturnParameter().isArray()) {
                sqlType = OracleTypes.ARRAY;
            } else {
                sqlType = getSQLTypes(returnArgument);
            }

            CallableStatement callableStatement = (CallableStatement) statement;
            if (sqlType == Types.STRUCT || sqlType == OracleTypes.ARRAY) {
                String returnTypeSQL = methodWrapper.getReturnParameter().getSqlTypeName();
                callableStatement.registerOutParameter(parameterIndex, sqlType, returnTypeSQL);
            } else
                callableStatement.registerOutParameter(parameterIndex, sqlType);
        }

    }

	@Override
	public void loadValues(JavaExecutionResult executionResult, DBNPreparedStatement<?> preparedStatement) throws SQLException {
		if (preparedStatement instanceof CallableStatement) {
			int outputIndex = getArgumentsCount() + 1;
			CallableStatement callableStatement = (CallableStatement) preparedStatement;
			Object result = getResult(callableStatement, outputIndex);
			executionResult.addArgumentValue("return", result);
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
		Object structDescriptor =  createDescriptorMethod.invoke(null, objectName.toUpperCase(), conn.getInner());

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

		@NonNls
		String className = getCanonicalName(field.getJavaClassRef());
		switch (className) {
			// primitives
			case "boolean": return asBooleanPrimitive(fieldValue);
			case "byte": return asBytePrimitive(fieldValue);
			case "char": return asCharacterPrimitive(fieldValue);
			case "double": return asDoublePrimitive(fieldValue);
			case "float": return asFloatPrimitive(fieldValue);
			case "int": return asIntegerPrimitive(fieldValue);
			case "long": return asLongPrimitive(fieldValue);
			case "short": return asShortPrimitive(fieldValue);

			// pseudo-primitives (prevent expensive class-details load from SYS schema)
			case "java.lang.Boolean": return asBoolean(fieldValue);
			case "java.lang.Byte": return asByte(fieldValue);
			case "java.lang.Character": asCharacter(fieldValue);
			case "java.lang.Double": return asDouble(fieldValue);
			case "java.lang.Float": return asFloat(fieldValue);
			case "java.lang.Integer": return asInteger(fieldValue);
			case "java.lang.Long": return asLong(fieldValue);
			case "java.lang.Short": return asShort(fieldValue);
			case "java.lang.String": return fieldValue;
			case "java.math.BigDecimal": return asBigDecimal(fieldValue);
			case "java.math.BigInteger": return asBigInteger(fieldValue);
			//...
			default:
				if (field.isClass()) {
					String objectName = getTypeName(field, wrapper);
					return getStructObject(executionInput, field.getJavaClass().getFields(), wrapper, objectName, fieldPath);
				}
				return fieldValue;
		}
	}

	private String getTypeName(DBJavaField field, Wrapper wrapper) {
		DBJavaClass javaClass = field.getJavaClass();

		for (ClassWrapper classWrapper : wrapper.getClasses()) {
			if (Objects.equals(classWrapper.getClassName(), javaClass.getCanonicalName())) {
				return classWrapper.getSqlTypeName();
			}
		}
		// should never reach here
		return null;
	}

	private int getSQLTypes(@NonNls String javaType) {
		switch (javaType) {
			case "int" : return Types.INTEGER;
			case "float" : return Types.FLOAT;
			case "double": return Types.DOUBLE;
			case "boolean": return Types.BIT;
			case "byte": return Types.TINYINT;
			case "short": return Types.SMALLINT;
			case "long": return Types.BIGINT;
			case "char": return Types.CHAR;
			case "java.lang.String": return Types.VARCHAR;
			case "java.lang.Character": return Types.CHAR;
			case "java.math.BigDecimal": return Types.DECIMAL;
			case "java.math.BigInteger": return Types.BIGINT;
			case "java.lang.Integer": return Types.INTEGER;
			case "java.lang.Long": return Types.BIGINT;
			case "java.lang.Double": return Types.DOUBLE;
			case "java.lang.Short": return Types.SMALLINT;
			case "java.lang.Float": return Types.FLOAT;
			case "java.lang.Byte": return Types.TINYINT;
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
