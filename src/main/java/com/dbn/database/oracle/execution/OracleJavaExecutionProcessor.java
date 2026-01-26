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

import com.dbn.common.data.Data;
import com.dbn.common.util.Java;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.connection.SessionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNPreparedStatement;
import com.dbn.database.common.execution.JavaExecutionProcessorImpl;
import com.dbn.database.oracle.OracleTypes;
import com.dbn.execution.java.JavaExecutionContext;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.result.JavaExecutionResult;
import com.dbn.execution.java.wrapper.WrapperModel;
import com.dbn.execution.java.wrapper.model.ClassWrapper;
import com.dbn.execution.java.wrapper.model.MethodWrapper;
import com.dbn.execution.java.wrapper.model.ParameterWrapper;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.lookup.DBObjectRef;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
	public String buildExecutionCommand(JavaExecutionInput executionInput, WrapperModel wrapperModel) {
		Map<String, String> codeParameters = wrapperModel.getInput().getCodeInputs();
		boolean procedure = isProcedure();
		String wrapperName = wrapperModel.getSqlWrapperName();
		List<DBJavaParameter> parameters = getParameters();

		@NonNls
		StringBuilder buffer = new StringBuilder();
		StringBuilder methodCallPrepare = new StringBuilder();

		List<String> inputPlaceHolders = new ArrayList<>();
		for (DBJavaParameter parameter : parameters) {
			if (!codeParameters.containsKey(parameter.getName())) {
				inputPlaceHolders.add("?");
			}
		}
		methodCallPrepare.append(String.join(", ", inputPlaceHolders));

		buffer.append("declare\n");

		if (!procedure) {
			ParameterWrapper returnParameter = wrapperModel.getMethods().get(0).getReturnParameter();
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
					.append(")");
			}
			buffer.append(";\n");
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
	protected void bindParameters(JavaExecutionInput executionInput, PreparedStatement statement, WrapperModel wrapperModel) {
		// bind input variables
		Map<String, String> javaInjectedParams = wrapperModel.getInput().getCodeInputs();
		int parameterIndex = 1;
		MethodWrapper methodWrapper = wrapperModel.getMethods().get(0);
		List<DBJavaParameter> parameters = getParameters();
		for (int i=0; i<parameters.size(); i++) {
			DBJavaParameter parameter = parameters.get(i);
			if(javaInjectedParams.containsKey(parameter.getName()))
				continue;

			String parameterName = parameter.getName();
			if (parameter.isArray()) {
				String objectName = methodWrapper.getParameters().get(i).getSqlTypeName();
				Array arrObj = getArrayObject(executionInput, parameter.getJavaClassRef(), wrapperModel, objectName, parameterName);
				statement.setArray(parameterIndex, arrObj);

			} else if (!parameter.isScalar()) { // TODO support pseudo-primitives com.dbn.object.type.DBJavaValueType
				String objectName = methodWrapper.getParameters().get(i).getSqlTypeName();
				Object structObj = getStructObject(executionInput, parameter.getJavaClass().getFields(), wrapperModel, objectName, parameterName);
				statement.setObject(parameterIndex, structObj);

			} else {
				@NonNls
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
				else if (clazz.equals("boolean")) statement.setBoolean(parameterIndex, Boolean.parseBoolean(value));
				else statement.setObject(parameterIndex, value);

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
	public void loadValues(JavaExecutionContext context, JavaExecutionResult executionResult, DBNPreparedStatement<?> preparedStatement) throws SQLException {
		if (preparedStatement instanceof CallableStatement callableStatement) {
			int outputIndex = getArgumentsCount(context) + 1;
            Object result = getResult(callableStatement, outputIndex);
			executionResult.addArgumentValue("return", result);
		}
	}

	@SneakyThrows
	private Object getStructObject(JavaExecutionInput executionInput, List<DBJavaField> fields, WrapperModel wrapperModel, String objectName, String fieldPath){
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
			customTypeAttributes[i] = parseValue(executionInput, wrapperModel, field, newFieldPath, value);
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
	private Array getArrayObject(JavaExecutionInput executionInput,DBObjectRef dbJavaClass, WrapperModel wrapperModel, String objectName, String fieldPath){
		ConnectionHandler connection = getMethod().getConnection();
		SessionId targetSessionId = executionInput.getTargetSessionId();
		SchemaId targetSchemaId = executionInput.getTargetSchemaId();
		DBNConnection conn = connection.getConnection(targetSessionId, targetSchemaId);

		String fieldValue = executionInput.getInputValue(fieldPath);
		String className = getCanonicalName(dbJavaClass);
		Class<?> clazz = Data.asPrimitiveClass(className);
		if(clazz == null){
			clazz = Class.forName(className);
		}
		List<?> values = Data.arrayStringToList(fieldValue, clazz);
		Object[] customTypeAttributes = values.toArray();

		ClassLoader cl =  conn.getInner().getClass().getClassLoader();
		Class<?> structDescriptorClass = Class.forName("oracle.sql.ArrayDescriptor",true, cl);
		Method createDescriptorMethod = structDescriptorClass.getMethod("createDescriptor", String.class, Connection.class);
		Object structDescriptor =  createDescriptorMethod.invoke(null, objectName, conn.getInner());

		Class<?> structClass = Class.forName("oracle.sql.ARRAY", true, cl);
		Constructor<?> structCtr = structClass.getConstructor(structDescriptorClass, Connection.class, Object.class);

		return (Array) structCtr.newInstance(structDescriptor, conn.getInner(), customTypeAttributes);
	}

	@Nullable
	private Object parseValue(JavaExecutionInput executionInput,
							  WrapperModel       wrapperModel,
							  DBJavaField        field,
							  String             fieldPath,
							  String             fieldValue) {

		if (field == null) return null;

		// 1) See if field is an array
		if(field.isArray()) {
			String objectName = getTypeName(field, wrapperModel);
			return getArrayObject(executionInput, field.getJavaClassRef(), wrapperModel, objectName, fieldPath);
		}

		// 2) Try the primitive / wrapper / well-known types first
        if (field.isScalar()) {
            return parseValue(field.getJavaClassRef(), fieldValue);
        }

		// 3) Complex struct handling (only in this overload)
		if (field.isClass()) {
            DBJavaClass javaClass = field.getJavaClass();
            String objectName = getTypeName(field, wrapperModel);
            return getStructObject(executionInput,
					javaClass.getFields(),
					wrapperModel,
					objectName,
					fieldPath);
		}

		// 3) Fallback
		return fieldValue;
	}

	@Nullable
	private Object parseValue(DBObjectRef javaClass, String fieldValue) {
		if (javaClass == null) return null;
		if (fieldValue == null) {
            // primitives cannot be null, let the Data.asAbcPrimitive default the values
            String className = javaClass.getObjectName();
            if (!Java.isPrimitive(className)) return null;
        }

		@NonNls
		String className = getCanonicalName(javaClass);
        return switch (className) {
            // primitives
            case "boolean" -> asBooleanPrimitive(fieldValue);
            case "byte" -> asBytePrimitive(fieldValue);
            case "char" -> asCharacterPrimitive(fieldValue);
            case "double" -> asDoublePrimitive(fieldValue);
            case "float" -> asFloatPrimitive(fieldValue);
            case "int" -> asIntegerPrimitive(fieldValue);
            case "long" -> asLongPrimitive(fieldValue);
            case "short" -> asShortPrimitive(fieldValue);

            // pseudo-primitives (prevent expensive class-details load from SYS schema)
            case "java.lang.Boolean" -> asBoolean(fieldValue);
            case "java.lang.Byte" -> asByte(fieldValue);
            case "java.lang.Character" -> asCharacter(fieldValue);
            case "java.lang.Double" -> asDouble(fieldValue);
            case "java.lang.Float" -> asFloat(fieldValue);
            case "java.lang.Integer" -> asInteger(fieldValue);
            case "java.lang.Long" -> asLong(fieldValue);
            case "java.lang.Short" -> asShort(fieldValue);
            case "java.lang.String" -> fieldValue;
            case "java.math.BigDecimal" -> asBigDecimal(fieldValue);
            case "java.math.BigInteger" -> asBigInteger(fieldValue);
            //...
            default -> null;
        };
	}


	private String getTypeName(DBJavaField field, WrapperModel wrapperModel) {

		for (ClassWrapper classWrapper : wrapperModel.getClasses()) {
			if (Objects.equals(classWrapper.getClassName(), getCanonicalName(field.getJavaClassRef()))) {
				return classWrapper.getSqlTypeName();
			}
		}
		// should never reach here
		return null;
	}

	private int getSQLTypes(@NonNls String javaType) {
        return switch (javaType) {
            case "int" -> Types.INTEGER;
            case "float" -> Types.FLOAT;
            case "double" -> Types.DOUBLE;
            case "boolean" -> Types.BIT;
            case "byte" -> Types.TINYINT;
            case "short" -> Types.SMALLINT;
            case "long" -> Types.BIGINT;
            case "char" -> Types.CHAR;
            case "java.lang.String" -> Types.VARCHAR;
            case "java.lang.Character" -> Types.CHAR;
            case "java.math.BigDecimal" -> Types.NUMERIC;
            case "java.math.BigInteger" -> Types.NUMERIC;
            case "java.lang.Integer" -> Types.NUMERIC;
            case "java.lang.Long" -> Types.NUMERIC;
            case "java.lang.Double" -> Types.NUMERIC;
            case "java.lang.Short" -> Types.NUMERIC;
            case "java.lang.Float" -> Types.NUMERIC;
            case "java.lang.Byte" -> Types.NUMERIC;
            default -> Types.STRUCT;
        };
	}

	private Object getResult(CallableStatement callableStatement, int outputIndex) throws SQLException {
        return switch (sqlType) {
            case Types.INTEGER -> callableStatement.getInt(outputIndex);
            case Types.FLOAT -> callableStatement.getFloat(outputIndex);
            case Types.DOUBLE -> callableStatement.getDouble(outputIndex);
            case Types.BIT -> callableStatement.getBoolean(outputIndex);
            case Types.TINYINT -> callableStatement.getByte(outputIndex);
            case Types.SMALLINT -> callableStatement.getShort(outputIndex);
            case Types.BIGINT -> callableStatement.getLong(outputIndex);
            case Types.CHAR -> callableStatement.getString(outputIndex);
            case Types.VARCHAR -> callableStatement.getString(outputIndex);
            case Types.DECIMAL -> callableStatement.getBigDecimal(outputIndex);
            default -> callableStatement.getObject(outputIndex);
        };
	}
}
