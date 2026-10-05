/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.database.oracle.execution;

import com.dbn.connection.jdbc.DBNCallableStatement;
import com.dbn.connection.jdbc.DBNPreparedStatement;
import com.dbn.data.type.DBDataType;
import com.dbn.data.type.DBNativeDataType;
import com.dbn.data.type.GenericDataType;
import com.dbn.database.common.execution.MethodExecutionProcessorImpl;
import com.dbn.database.oracle.OracleTypes;
import com.dbn.execution.method.MethodExecutionInput;
import com.dbn.execution.method.result.MethodExecutionResult;
import com.dbn.object.DBArgument;
import com.dbn.object.DBMethod;
import com.dbn.object.DBType;
import com.dbn.object.DBTypeAttribute;
import org.jetbrains.annotations.NonNls;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OracleMethodExecutionProcessor extends MethodExecutionProcessorImpl {
    public OracleMethodExecutionProcessor(DBMethod method) {
        super(method);
    }

    protected void preHookExecutionCommand(StringBuilder buffer) {}
    protected void postHookExecutionCommand(StringBuilder buffer) {}


    @Override
    public String buildExecutionCommand(MethodExecutionInput executionInput) throws SQLException {
        DBArgument returnArgument = getReturnArgument();

        @NonNls StringBuilder buffer = new StringBuilder();
        buffer.append("declare\n");
        buffer.append("     v_timeout BINARY_INTEGER;\n");

        // variable declarations
        List<DBArgument> arguments = getArguments();
        for (DBArgument argument : arguments) {
            DBDataType dataType = argument.getDataType();
            if (dataType.isPurelyDeclared()) {
                buffer.append("    ");
                appendVariableName(buffer, argument);
                buffer.append(" ").append(dataType.getQualifiedName(true)).append(";\n");
            } else if (isBoolean(dataType)) {
                appendVariableName(buffer, argument);
                buffer.append(" boolean;\n");
            }
        }

        buffer.append("begin\n");

        preHookExecutionCommand(buffer);

        // input variable initialization
        for (DBArgument argument : arguments) {
            if (!argument.isInput()) continue;

            DBDataType dataType = argument.getDataType();
            if (dataType.isPurelyDeclared()) {
                DBType declaredType = dataType.getDeclaredType();

                buffer.append("    ");
                appendVariableName(buffer, argument);
                buffer.append(" := ").append(dataType.getQualifiedName(true));
                if (declaredType.isCollection()) {
                    buffer.append("()");
                    buffer.append(";\n");
                } else {
                    appendNullConstructor(buffer, declaredType);
                    buffer.append(";\n");
                    appendRecordAttributeInitializations(buffer, appendVariableName(new StringBuilder(), argument).toString(), declaredType);
                }
            } else if(isBoolean(dataType)) {
                String stringValue = parseBoolean(argument.getName(true), executionInput.getInputValue(argument));

                buffer.append("    ");
                appendVariableName(buffer, argument);
                buffer.append(" := ").append(stringValue).append(";\n");
            }
        }

        // method call
        buffer.append("\n    ");
        if (returnArgument != null) {
            DBDataType dataType = returnArgument.getDataType();
            if (dataType.isPurelyDeclared() || isBoolean(dataType))
                appendVariableName(buffer, returnArgument); else
                buffer.append("?");

            buffer.append(" := ");
        }

        // method parameters
        buffer.append(getMethod().getQualifiedName(true)).append("(");
        for (DBArgument argument : arguments) {
            if (argument != returnArgument) {
                DBDataType dataType = argument.getDataType();
                if (dataType.isPurelyDeclared() || isBoolean(dataType))
                    appendVariableName(buffer, argument); else
                    buffer.append("?");
                boolean isLast = arguments.indexOf(argument) == arguments.size() - 1;
                if (!isLast) buffer.append(", ");
            }
        }
        buffer.append(");\n\n");


        // output variable initialization
        for (DBArgument argument : arguments) {
            if (!argument.isOutput()) continue;

            DBDataType dataType = argument.getDataType();
            if (dataType.isPurelyDeclared()) {
                DBType declaredType = dataType.getDeclaredType();
                if (declaredType.isCollection()) {
                    buffer.append("    open ? for select * from table (");
                    appendVariableName(buffer, argument);
                    buffer.append(");");
                } else {
                    appendRecordOutput(buffer, appendVariableName(new StringBuilder(), argument).toString(), declaredType);
                }
            } else if (isBoolean(dataType)) {
                buffer.append("    ? := case when ");
                appendVariableName(buffer, argument);
                buffer.append(" then 'true' else 'false' end;\n");
            }
        }


        postHookExecutionCommand(buffer);
        buffer.append("\nend;\n");
        return buffer.toString();
    }

    private static StringBuilder appendVariableName(@NonNls StringBuilder buffer, DBArgument argument) {
        return buffer.append("var_").append(argument.getPosition());
    }


    @Override
    protected void bindParameters(MethodExecutionInput executionInput, PreparedStatement preparedStatement) throws SQLException {
        CallableStatement callableStatement = (CallableStatement) preparedStatement;
        DBArgument returnArgument = getReturnArgument();

        // bind input variables
        int parameterIndex = 1;
        for (DBArgument argument : getArguments()) {
            if (!argument.isInput()) continue;

            DBDataType dataType = argument.getDataType();
            DBType type = dataType.getDeclaredType();
            if (dataType.isPurelyDeclared()) {
                if (!type.isCollection()) {
                    parameterIndex = bindRecordParameters(
                            executionInput,
                            callableStatement,
                            argument,
                            type,
                            Collections.emptyList(),
                            parameterIndex);
                }
            }
        }

        // bind return variable (functions only)
        if (returnArgument != null) {
            DBDataType dataType = returnArgument.getDataType();
            if(!dataType.isPurelyDeclared() && !isBoolean(dataType)) {
                callableStatement.registerOutParameter(parameterIndex, returnArgument.getDataType().getSqlType());
                parameterIndex++;
            }
        }

        // bind input/output parameters
        for (DBArgument argument : getArguments()) {
            DBDataType dataType = argument.getDataType();
            if (!argument.equals(returnArgument) && dataType.isNative() && !isBoolean(dataType)) {
                if (argument.isInput()) {
                    String stringValue = executionInput.getInputValue(argument);
                    setParameterValue(callableStatement, parameterIndex, dataType, stringValue);
                }
                if (argument.isOutput()){
                    callableStatement.registerOutParameter(parameterIndex, dataType.getSqlType());
                }
                parameterIndex++;
            }
        }

        // bind output variables
        for (DBArgument argument : getArguments()) {
            if (!argument.isOutput()) continue;

            DBDataType dataType = argument.getDataType();
            if (dataType.isPurelyDeclared()) {
                DBType declaredType = dataType.getDeclaredType();
                if (declaredType.isCollection()) {
                    callableStatement.registerOutParameter(parameterIndex, OracleTypes.CURSOR);
                    parameterIndex++;
                } else {
                    parameterIndex = registerRecordOutputParameters(callableStatement, declaredType, parameterIndex);
                }
            } else if (isBoolean(dataType)){
                callableStatement.registerOutParameter(parameterIndex, dataType.getSqlType());
                parameterIndex++;
            }
        }
    }

    @Override
    public void loadValues(MethodExecutionResult executionResult, DBNPreparedStatement preparedStatement) throws SQLException {
        DBNCallableStatement callableStatement = (DBNCallableStatement) preparedStatement;
        DBArgument returnArgument = getReturnArgument();

        // increment parameter index for input variables
        int parameterIndex = 1;
        for (DBArgument argument : getArguments()) {
            DBDataType dataType = argument.getDataType();
            if (dataType.isPurelyDeclared()) {
                if (argument.isInput()) {
                    DBType declaredType = dataType.getDeclaredType();
                    if (!declaredType.isCollection()) {
                        parameterIndex += countRecordValues(declaredType);
                    }
                }
            }
        }

        // get return value (functions only)
        if (returnArgument != null) {
            DBDataType dataType = returnArgument.getDataType();
            if (!dataType.isPurelyDeclared() && !isBoolean(dataType)) {
                DBNativeDataType nativeDataType = dataType.getNativeType();
                Object result = nativeDataType.getValueFromStatement(callableStatement, parameterIndex);
                executionResult.addArgumentValue(returnArgument, result);
                parameterIndex++;

            }
        }

        // get output parameter values
        for (DBArgument argument : getArguments()) {
            if (!argument.equals(returnArgument)) {
                DBDataType dataType = argument.getDataType();
                if (dataType.isNative() && !isBoolean(dataType)) {
                    if (argument.isOutput()){
                        DBNativeDataType nativeDataType = dataType.getNativeType();
                        Object result = nativeDataType.getValueFromStatement(callableStatement, parameterIndex);
                        executionResult.addArgumentValue(argument, result);
                    }
                    parameterIndex++;
                }
            }
        }

        // get output variable values
        for (DBArgument argument : getArguments()) {
            if (!argument.isOutput()) continue;

            DBDataType dataType = argument.getDataType();
            if (dataType.isPurelyDeclared()) {
                DBType declaredType = dataType.getDeclaredType();
                if (declaredType.isCollection()) {
                    DBNativeDataType nativeDataType = dataType.getNativeType();
                    Object result = nativeDataType == null ?
                            callableStatement.getObject(parameterIndex) :
                            nativeDataType.getValueFromStatement(callableStatement, parameterIndex);
                    executionResult.addArgumentValue(argument, result);
                    parameterIndex++;
                } else {
                    parameterIndex = loadRecordValues(
                            executionResult,
                            callableStatement,
                            argument,
                            declaredType,
                            Collections.emptyList(),
                            parameterIndex);
                }
            } else if (isBoolean(dataType)) {
                Object result = dataType.getNativeType().getValueFromStatement(callableStatement, parameterIndex);
                executionResult.addArgumentValue(argument, result);
                parameterIndex++;

            }
        }
    }

    private static boolean isBoolean(DBDataType dataType) {
        return dataType.getGenericDataType() == GenericDataType.BOOLEAN;
    }

    private static boolean isRecord(DBDataType dataType) {
        DBType declaredType = dataType.getDeclaredType();
        return dataType.isPurelyDeclared() && declaredType != null && !declaredType.isCollection();
    }

    private static boolean isScalar(DBDataType dataType) {
        return dataType.isNative() || dataType.getDeclaredType() == null;
    }

    private static void appendNullConstructor(StringBuilder buffer, DBType declaredType) {
        buffer.append("(");
        List<DBTypeAttribute> attributes = declaredType.getAttributes();
        for (int i = 0; i < attributes.size(); i++) {
            if (i > 0) buffer.append(", ");
            buffer.append("NULL");
        }
        buffer.append(")");
    }

    private static void appendRecordAttributeInitializations(StringBuilder buffer, String variableName, DBType declaredType) {
        for (DBTypeAttribute attribute : declaredType.getAttributes()) {
            DBDataType dataType = attribute.getDataType();
            String attributeName = variableName + "." + attribute.getName(true);
            if (isRecord(dataType)) {
                DBType nestedType = dataType.getDeclaredType();
                buffer.append("    ").append(attributeName).append(" := ")
                        .append(dataType.getQualifiedName(true));
                appendNullConstructor(buffer, nestedType);
                buffer.append(";\n");
                appendRecordAttributeInitializations(buffer, attributeName, nestedType);
            } else if (isScalar(dataType)) {
                buffer.append("    ").append(attributeName).append(" := ?;\n");
            }
        }
    }

    private static void appendRecordOutput(
            StringBuilder buffer,
            String variableName,
            DBType declaredType) {
        for (DBTypeAttribute attribute : declaredType.getAttributes()) {
            DBDataType dataType = attribute.getDataType();
            String attributeName = variableName + "." + attribute.getName(true);
            if (isRecord(dataType)) {
                appendRecordOutput(buffer, attributeName, dataType.getDeclaredType());
            } else if (isScalar(dataType)) {
                buffer.append("    ? := ").append(attributeName).append(";\n");
            }
        }
    }

    private int bindRecordParameters(
            MethodExecutionInput executionInput,
            CallableStatement callableStatement,
            DBArgument argument,
            DBType declaredType,
            List<DBTypeAttribute> attributePath,
            int parameterIndex) throws SQLException {
        for (DBTypeAttribute attribute : declaredType.getAttributes()) {
            DBDataType dataType = attribute.getDataType();
            List<DBTypeAttribute> nestedPath = new ArrayList<>(attributePath);
            nestedPath.add(attribute);
            if (isRecord(dataType)) {
                parameterIndex = bindRecordParameters(
                        executionInput,
                        callableStatement,
                        argument,
                        dataType.getDeclaredType(),
                        nestedPath,
                        parameterIndex);
            } else if (isScalar(dataType)) {
                String stringValue = executionInput.getInputValue(argument, nestedPath);
                setParameterValue(callableStatement, parameterIndex, dataType, stringValue);
                parameterIndex++;
            }
        }
        return parameterIndex;
    }

    private static int registerRecordOutputParameters(CallableStatement callableStatement, DBType declaredType, int parameterIndex) throws SQLException {
        for (DBTypeAttribute attribute : declaredType.getAttributes()) {
            DBDataType dataType = attribute.getDataType();
            if (isRecord(dataType)) {
                parameterIndex = registerRecordOutputParameters(callableStatement, dataType.getDeclaredType(), parameterIndex);
            } else if (isScalar(dataType)) {
                callableStatement.registerOutParameter(parameterIndex, dataType.getSqlType());
                parameterIndex++;
            }
        }
        return parameterIndex;
    }

    private static int countRecordValues(DBType declaredType) {
        int count = 0;
        for (DBTypeAttribute attribute : declaredType.getAttributes()) {
            DBDataType dataType = attribute.getDataType();
            count += isRecord(dataType) ? countRecordValues(dataType.getDeclaredType()) : isScalar(dataType) ? 1 : 0;
        }
        return count;
    }

    private static int loadRecordValues(
            MethodExecutionResult executionResult,
            DBNCallableStatement callableStatement,
            DBArgument argument,
            DBType declaredType,
            List<DBTypeAttribute> attributePath,
            int parameterIndex) throws SQLException {
        for (DBTypeAttribute attribute : declaredType.getAttributes()) {
            DBDataType dataType = attribute.getDataType();
            List<DBTypeAttribute> nestedPath = new ArrayList<>(attributePath);
            nestedPath.add(attribute);
            if (isRecord(dataType)) {
                parameterIndex = loadRecordValues(
                        executionResult,
                        callableStatement,
                        argument,
                        dataType.getDeclaredType(),
                        nestedPath,
                        parameterIndex);
            } else if (isScalar(dataType)) {
                DBNativeDataType nativeDataType = dataType.getNativeType();
                Object result = nativeDataType == null ?
                        callableStatement.getObject(parameterIndex) :
                        nativeDataType.getValueFromStatement(callableStatement, parameterIndex);
                executionResult.addArgumentValue(argument, nestedPath, result);
                parameterIndex++;
            }
        }
        return parameterIndex;
    }

    private static String parseBoolean(String argumentName, String booleanString) throws SQLException {
        if (booleanString != null && !booleanString.equalsIgnoreCase("true") && !booleanString.equalsIgnoreCase("false")) {
            throw new SQLException("Invalid boolean value for argument " + argumentName + ". true / false expected");
        }
        return Boolean.toString(Boolean.parseBoolean(booleanString));
    }

}
