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

package com.dbn.execution.java.wrapper;

import com.dbn.common.project.ProjectRef;
import com.dbn.common.template.TemplateUtilities;
import com.dbn.common.util.Java;
import com.dbn.execution.java.wrapper.model.ClassWrapper;
import com.dbn.execution.java.wrapper.model.FieldWrapper;
import com.dbn.execution.java.wrapper.model.MethodWrapper;
import com.dbn.execution.java.wrapper.model.ParameterWrapper;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@NonNls
public final class WrapperStatementBuilder {
    private final ProjectRef project;

    public WrapperStatementBuilder(@NotNull Project project) {
        this.project = ProjectRef.of(project);
    }

    @NotNull
    public Project getProject() {
        return ProjectRef.ensure(project);
    }

    private List<String> createSQLTypes(WrapperModel model) {
        List<String> sqlTypes = new ArrayList<>();
        @NonNls Properties properties = new Properties();

        Set<String> sqlTypeNames = new HashSet<>();
        for (ClassWrapper classWrapper : model.getClasses()) {
            String sqlTypeName = classWrapper.getSqlTypeName();

            if (sqlTypeNames.contains(sqlTypeName)) continue;
            sqlTypeNames.add(sqlTypeName);

            String fields = classWrapper
                    .getFields()
                    .stream()
                    .map(f -> f.getSqlAttributeDeclaration())
                    .collect(Collectors.joining(",\n\t"));

            properties.setProperty("TYPENAME", sqlTypeName);
            properties.setProperty("FIELDS", fields);
            properties.setProperty("IS_ARRAY", String.valueOf(classWrapper.isArray()));

            String containedSqlTypeName = classWrapper.getContainedSqlTypeName();
            if (containedSqlTypeName != null)
                properties.setProperty("ARRAY_TYPE", containedSqlTypeName);

            String code = generateCode("DBN - OJVM SQLType.sql", properties);
            sqlTypes.add(code);
        }
        return sqlTypes;
    }

    private List<String> createSQLToJava(WrapperModel model) {
        List<String> javaConverterMethods = new ArrayList<>();

        for (ClassWrapper classWrapper : model.getClasses()) {
            // Skip RETURN attributes and duplicates based on SQL type name.
            if (classWrapper.getArgumentDirection() == ClassWrapper.ArgumentDirection.OUT)
                continue;

            @NonNls
            Map<String, Object> context = new HashMap<>();
            context.put("JAVA_COMPLEX_TYPE", classWrapper.getClassName());
            context.put("CONVERTER_METHOD_NAME", classWrapper.getSqlToJavaConverterName());

            String code;
            if (classWrapper.isArray()) {
                // For arrays, set typecasting properties
                String squareBrackets = String.join("", Collections.nCopies(classWrapper.getArrayDepth() - 1, "[]"));
                String iterationCode = buildSqlArrayToJavaAssignmentLine(classWrapper);

                context.put("SQUARE_BRACKETS", squareBrackets);
                context.put("ITERATION_CODE", iterationCode);

                code = generateCode("DBN - OJVM SQLArrayToJava.java", context);
            } else {
                List<String> fieldAssignments = new ArrayList<>();
                if (classWrapper.getFields() != null && !classWrapper.getFields().isEmpty()) {
                    for (FieldWrapper fieldWrapper : classWrapper.getFields()) {
                        String assignmentLine = buildSqlToJavaAssignmentLine(fieldWrapper);
                        fieldAssignments.add(assignmentLine);
                    }
                }
                context.put("FIELD_ASSIGNMENTS", fieldAssignments);
                code = generateCode("DBN - OJVM SQLObjectToJava.java", context);
            }
            javaConverterMethods.add(code);
        }
        return javaConverterMethods;
    }


    private List<String> createJavaToSQL(WrapperModel model) {
        List<String> javaConverterMethods = new ArrayList<>();
        if (model.getMethods().isEmpty()) return javaConverterMethods;

        for (ClassWrapper classWrapper : model.getClasses()) {
            if (classWrapper.getArgumentDirection() == ClassWrapper.ArgumentDirection.IN) continue;

            @NonNls
            Map<String, Object> context = new HashMap<>();

            context.put("JAVA_COMPLEX_TYPE", classWrapper.getClassName());
            context.put("SQL_OBJECT_TYPE", classWrapper.getSqlType().getQualifiedName());
            context.put("CONVERTER_METHOD_NAME", classWrapper.getJavaToSqlConverterName());

            String code;
            if (classWrapper.isArray()) {
                String squareBrackets = String.join("", Collections.nCopies(classWrapper.getArrayDepth() - 1, "[]"));
                String iterationCode = buildJavaArrayToSqlAssignmentLine(classWrapper);

                context.put("SQUARE_BRACKETS", squareBrackets);
                context.put("ITERATION_CODE", iterationCode);
                code = generateCode("DBN - OJVM JavaArrayToSQL.java", context);
            } else {
                context.put("TOTAL_FIELDS", classWrapper.getFields().size());
                List<String> fieldAssignments = new ArrayList<>();
                if (classWrapper.getFields() != null && !classWrapper.getFields().isEmpty()) {
                    for (FieldWrapper fieldWrapper : classWrapper.getFields()) {
                        String assignmentLine = buildJavaToSqlAssignmentLine(fieldWrapper);
                        fieldAssignments.add(assignmentLine);
                    }
                }
                context.put("FIELD_ASSIGNMENTS", fieldAssignments);
                code = generateCode("DBN - OJVM JavaObjectToSQL.java", context);
            }

            javaConverterMethods.add(code);
        }
        return javaConverterMethods;
    }

    private List<String> createJavaWrapperMethods(WrapperModel model) {
        List<String> javaWrapperMethods = new ArrayList<>();
        if (model.getMethods().isEmpty()) return javaWrapperMethods;

        for (MethodWrapper method : model.getMethods()) {
            String methodReturnType = resolveMethodReturnType(method);
            String methodName = method.getSurrogateJavaMethodName();
            String methodParameters = buildMethodParameters(method,true);
            String methodInvocation = buildMethodInvocation(method, model.getClassName());
            List<String> argumentConversions = buildArgumentConversions(method);

            @NonNls
            Map<String, Object> context = new HashMap<>();
            context.put("METHOD_NAME", methodName);
            context.put("METHOD_PARAMETERS", methodParameters);
            context.put("METHOD_RETURN_TYPE", methodReturnType);
            context.put("ARGUMENT_CONVERSIONS", argumentConversions);
            context.put("METHOD_INVOCATION", methodInvocation);

            String code = generateCode("DBN - OJVM JavaWrapperMethod.java", context);

            javaWrapperMethods.add(code);
        }
        return javaWrapperMethods;
    }

    @NonNls
    @NotNull
    private String createJavaWrapper(WrapperModel model) {
        List<String> sqlMethods = createSQLToJava(model);
        List<String> javaMethods = createJavaToSQL(model);
        List<String> javaWrapperMethods = createJavaWrapperMethods(model);

        @NonNls
        Map<String, Object> context = new HashMap<>();

        context.put("JAVA_WRAPPER_NAME", model.getJavaWrapperName());
        context.put("SQL_CONVERSION_METHODS", sqlMethods);
        context.put("JAVA_CONVERSION_METHODS", javaMethods);
        context.put("JAVA_WRAPPER_METHODS", javaWrapperMethods);
        context.put("FULLY_QUALIFIED_ORIGINAL_CLASSNAME", model.getClassName());
        context.put("JAVA_CLASS", model.getClassName());
        context.put("WRAPPER_METHODS", model.getMethods());

        return generateCode("DBN - OJVM JavaWrapper.java", context);

    }

    private String createSQLWrapper(WrapperModel model) {
        @NonNls
        Map<String, Object> context = new HashMap<>();
        context.put("SQL_WRAPPER_NAME", model.getSqlWrapperName());
        context.put("JAVA_WRAPPER_NAME", model.getJavaWrapperName());
        // Transform each WrapperJavaMethod into a map with precomputed values.
        List<Map<String, Object>> methodList = model.getMethods().stream()
                .map(method -> {
                    @NonNls
                    Map<String, Object> m = new HashMap<>();
                    m.put("JAVA_METHOD_NAME", method.getSurrogateJavaMethodName());
                    m.put("SQL_METHOD_NAME", method.getSqlMethodName());
                    // Precompute the SQL signature using your new getSqlSignature function.
                    m.put("SQL_PARAMETERS", getSqlParameters(method));
                    // Precompute the Java signature (false indicates no argument names, per your original code).
                    m.put("JAVA_PARAMETERS", buildMethodParameters(method, false));

                    // Determine the resolved return type.
                    String javaReturnType = resolveMethodReturnType(method);
                    m.put("JAVA_RETURN_TYPE", javaReturnType);

                    // Flag to simplify the template logic.
                    boolean isProcedure = Java.isVoid(javaReturnType);
                    m.put("IS_PROCEDURE", isProcedure);

                    // For non-void methods, pass along the SQL return type from the original return type.
                    if (!isProcedure && method.getReturnParameter() != null) {
                        m.put("SQL_RETURN_TYPE", method.getReturnParameter().getSqlTypeName());
                    }
                    return m;
                })
                .collect(Collectors.toList());

        context.put("WRAPPER_METHODS", methodList);
        context.put("IS_PACKAGE_FORMAT", model.isClassWrapper());

        return generateCode("DBN - OJVM SQLWrapper.sql", context);
    }

    public String buildWrapperCreationStatement(WrapperModel model) {
        List<String> sqlTypes = createSQLTypes(model);
        String javaCode = createJavaWrapper(model);
        String sqlWrapper = createSQLWrapper(model);

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

        return sqlCode;
    }

    public String buildWrapperRemovalStatement(WrapperModel model) {
        @NonNls
        Map<String, Object> context = new HashMap<>();

        ParameterWrapper returnParameter = model.getMethods().get(0).getReturnParameter();
        boolean isFunction = returnParameter != null && returnParameter.getJavaTypeName() != null;
        context.put("SQL_WRAPPER_TYPE", isFunction ? "FUNCTION" : "PROCEDURE");

        Set<String> sqlTypeNames = model.getSqlTypeNames();
        context.put("SQL_TYPE_NAMES", sqlTypeNames);
        context.put("SQL_WRAPPER_NAME", model.getSqlWrapperName());
        context.put("JAVA_WRAPPER_NAME", model.getJavaWrapperName());
        return generateCode("DBN - OJVM SQLCleanup.sql", context);
    }

    public String buildSqlToJavaAssignmentLine(FieldWrapper fieldWrapper) {
        if (!fieldWrapper.isUpdatable()) return ""; // TODO will this ever happen?

        String fieldName = fieldWrapper.getName();
        int fieldIndex = fieldWrapper.getIndex();

        String valueExpression = "objArray[" + fieldIndex + "]";

        // Build conversion expression.
        if (fieldWrapper.isComplexType()) {
            // For complex types, wrap the SQL element with the proper converter.
            WrapperModel model = fieldWrapper.getModel();
            ClassWrapper classWrapper = model.getFieldClassWrapper(fieldWrapper);
            String converterName = classWrapper.getSqlToJavaConverterName();

            String castBlock = fieldWrapper.isArray() ? "(java.sql.Array)" : "(java.sql.Struct)";
            valueExpression = converterName + "(" + castBlock + " " + valueExpression + ")";
        }

        // Type cast
        String typeCastStart = fieldWrapper.getTypeCastStart();
        String typeCastEnd = fieldWrapper.getTypeCastEnd();
        typeCastStart = typeCastStart != null && !fieldWrapper.isComplexType() ? typeCastStart : "";
        typeCastEnd = typeCastEnd != null && !fieldWrapper.isComplexType() ? typeCastEnd : "";

        valueExpression = typeCastStart + valueExpression + typeCastEnd;

        // Update block
        boolean accessible = fieldWrapper.isAccessible();
        String setterName = fieldWrapper.getSetterName();
        String assignmentStart = accessible ?
                "." + fieldName + " = " :
                "." + setterName + "(";
        String assignmentEnd = accessible ? "" : ")";
        String assignmentExpr = assignmentStart + valueExpression + assignmentEnd;

        // Assignment line
        return "if (objArray[" + fieldIndex + "] != null) { javaObj" + assignmentExpr + "; }";
    }


    public String buildJavaToSqlAssignmentLine(FieldWrapper fieldWrapper) {
        if (!fieldWrapper.isReadable()) return ""; // TODO will this ever happen?

        String fieldName = fieldWrapper.getName();
        int fieldIndex = fieldWrapper.getIndex();

        boolean accessible = fieldWrapper.isAccessible();
        String getterName = fieldWrapper.getGetterName();
        String fieldAccessor = accessible ?
                "." + fieldName :
                "." + getterName + "()";

        String valueExpression = "javaObj" + fieldAccessor;

        if (fieldWrapper.isArray() || fieldWrapper.isComplexType()) {
            WrapperModel model = fieldWrapper.getModel();
            ClassWrapper classWrapper = model.getFieldClassWrapper(fieldWrapper);
            String converterName = classWrapper.getJavaToSqlConverterName();

            valueExpression = converterName + "(" +valueExpression + ")";
        }

        // Assignment line
        return "objArray[" + fieldIndex + "] = " + valueExpression + ";";
    }

    public String buildSqlArrayToJavaAssignmentLine(ClassWrapper classWrapper) {
        @NonNls
        String valueExpression = "objArray[i]";

        if (classWrapper.getArrayDepth() <= 1) {
            SqlType sqlType = TypeMappings.getSqlType(classWrapper.getClassName());

            if (sqlType != null) {
                String transformerPrefix = sqlType.getTransformerPrefix();
                String transformerSuffix = sqlType.getTransformerSuffix();
                valueExpression = transformerPrefix + valueExpression + transformerSuffix;
            } else {
                String converterName = classWrapper.getContainedClassWrapper().getSqlToJavaConverterName();
                valueExpression = converterName + "((java.sql.Struct) " + valueExpression + ")";
            }
        } else {
            // Multi-dimensional
            String converterName = classWrapper.getContainedClassWrapper().getSqlToJavaConverterName();
            valueExpression = converterName + "((java.sql.Array) " + valueExpression + ")";
        }

        return "if (objArray[i] != null) { javaObj[i] = " + valueExpression + "; }";
    }

    public String buildJavaArrayToSqlAssignmentLine(ClassWrapper classWrapper) {
        @NonNls
        String valueExpression = "javaObj[i]";

        ClassWrapper containedClassWrapper = classWrapper.getContainedClassWrapper();
        if (containedClassWrapper != null) {
            String converterMethodName = containedClassWrapper.getJavaToSqlConverterName();
            valueExpression = converterMethodName + "(" +  valueExpression + ")";
        }

        return "objArray[i] = " + valueExpression + ";";
    }

    //methods for supporting wrapper creation

    public String getSqlParameters(MethodWrapper method) {
        List<ParameterWrapper> parameters = method.getParameters();
        // Filter out injected parameters
        List<ParameterWrapper> filtered =
                parameters.stream()
                        .filter(e -> !e.isCodeInput())
                        .toList();
        if (filtered.isEmpty()) return "";
        AtomicInteger idx = new AtomicInteger(0);
        return "(" + filtered
                .stream()
                .map(e -> "arg_" + idx.getAndIncrement() + " " + e.getSqlTypeName())
                .collect(Collectors.joining(", ")) + ")";
    }

    public String buildMethodParameters(MethodWrapper method, boolean includeArgumentNames) {
        List<ParameterWrapper> params = method.getParameters();

        return "(" + IntStream.range(0, params.size())
                .filter(i -> !params.get(i).isCodeInput()) // Exclude injected
                .mapToObj(i -> {
                    String mappedType = getMappedType(params.get(i));
                    return mappedType + (includeArgumentNames ? " arg" + i : "");
                })
                .collect(Collectors.joining(", ")) + ")";
    }

    public String getMappedType(ParameterWrapper parameter) {
        String javaTypeName = parameter.getJavaTypeName();
        return ((("java.lang.Character".equals(javaTypeName)
                || "char".equals(javaTypeName)) && !parameter.isArray()) ? "java.lang.String" :
                parameter.isArray() ? "java.sql.Array" :
                parameter.isComplexType() ? "java.sql.Struct" :
                                javaTypeName);
    }

    public String getArgumentsInJavaCaller(MethodWrapper method) {
        List<ParameterWrapper> params = method.getParameters();

        return IntStream.range(0, params.size())
                .mapToObj(i -> "param" + i)
                .collect(Collectors.joining(", "));
    }

    public List<String> buildArgumentConversions(MethodWrapper method) {
        List<String> argumentConversions = new ArrayList<>();
        List<ParameterWrapper> parameters = method.getParameters();

        for (int i = 0; i < parameters.size(); i++) {
            @NonNls
            StringBuilder statement = new StringBuilder();

            ParameterWrapper parameter = parameters.get(i);
            String javaTypeName = parameter.getJavaTypeName();
            if (parameter.isCodeInput()) {
                statement.append(parameter.getCodeInput());
                statement.append(System.lineSeparator());
            }
            else if (parameter.isArray() || parameter.isComplexType()) {
                // Construct the conversion statement.
                statement.append(javaTypeName)
                        .append(arrayBrackets(parameter.getArrayDepth()))
                        .append(" param").append(i).append(" = ")
                        .append(parameter.getConverterName())
                        .append("(arg").append(i).append(");");
            } else if ("char".equals(javaTypeName) || "java.lang.Character".equals(javaTypeName)) {
                statement.append(getCharacterArgumentInitialization(javaTypeName, i));
            } else {
                statement.append(javaTypeName)
                        .append(arrayBrackets(parameter.getArrayDepth()))
                        .append(" param").append(i).append(" = ")
                        .append("arg").append(i).append(";");
            }

            if (!statement.isEmpty()) {
                argumentConversions.add(statement.toString());
            }
        }
        return argumentConversions;
    }

    private String getCharacterArgumentInitialization(
            String javaTypeName,
            int argumentIndex) {

        String srcName = "arg" + argumentIndex;
        String destName = "param" + argumentIndex;

        @NonNls
        StringBuilder sb = new StringBuilder();
        SqlType sqlType = TypeMappings.getSqlType(javaTypeName);

        if ("char".equals(javaTypeName)) {
            sb.append("char ").append(destName)
                    .append(" = ")
                    .append(sqlType.getTransformerPrefix())
                    .append(srcName)
                    .append(sqlType.getTransformerSuffix())
                    .append(";");
        } else { // java.lang.Character
            sb.append("java.lang.Character ").append(destName)
                    .append(" = null;\n")
                    .append("if (").append(srcName)
                    .append(" != null && ").append(srcName).append(".length() > 0) {\n")
                    .append("    ").append(destName)
                    .append(" = ")
                    .append(sqlType.getTransformerPrefix())
                    .append(srcName)
                    .append(sqlType.getTransformerSuffix())
                    .append(";\n")
                    .append("}");
        }
        return sb.toString();
    }


    public String resolveMethodReturnType(MethodWrapper method) {
        ParameterWrapper returnType = method.getReturnParameter();
        if (returnType == null) {
            return "void";
        }
        if (returnType.isArray()) {
            return "java.sql.Array";
        }
        if (returnType.isComplexType()) {
            return "java.sql.Struct";
        }
        String returnJavaType = returnType.getJavaTypeName();
        if ("char".equals(returnJavaType) || "java.lang.Character".equals(returnJavaType)) {
            return "java.lang.String";
        }

        return returnJavaType;
    }

    public String buildMethodInvocation(MethodWrapper method,
                                        String fullyQualifiedOriginalClassName) {
        ParameterWrapper returnType = method.getReturnParameter();
        StringBuilder statement = new StringBuilder();
        String converterMethodStart = "";
        String converterMethodEnd = "";

        if (returnType != null && (returnType.isArray() || returnType.isComplexType())) {
            converterMethodStart = returnType.getConverterName() + "(";
            converterMethodEnd = ")";
        }


        statement.append(converterMethodStart)
                .append(fullyQualifiedOriginalClassName).append(".")
                .append(method.getJavaMethodName())
                .append("(")
                .append(getArgumentsInJavaCaller(method))
                .append(")")
                .append(converterMethodEnd);

        if (returnType != null && !returnType.isArray()) {
            String javaTypeName = returnType.getJavaTypeName();
            if ("char".equals(javaTypeName) || "java.lang.Character".equals(javaTypeName)) {
                return getCharReturnStatement(statement.toString(), javaTypeName);
            }
        }

        if (returnType != null) {
            if(returnType.isCodeInput()) { // TODO when will this ever happen?
                String declaration = returnType.getJavaTypeName() +
                        arrayBrackets(returnType.getArrayDepth())
                        + " retStr = new " + statement + ";" + System.lineSeparator();
                return declaration +
                        " if(retStr! = null)" + System.lineSeparator()
                        + "   return retStr.toString();"
                        + "return null;";

            }
            return "return " + statement + ";";
        }
        return statement + ";";
    }

    @NonNls
    private String getCharReturnStatement(String returnCaller, String javaTypeName) {
        if ("char".equals(javaTypeName)) {
            return "return String.valueOf(" + returnCaller + ");";
        }
        return  "java.lang.Character retChar = " + returnCaller + ";\n"
                + "return (retChar == null) ? null : String.valueOf(retChar);";
    }

    private String generateCode(@NonNls String templateName, Properties properties) {
        return TemplateUtilities.generateCode(getProject(), templateName, properties);
    }

    private String generateCode(@NonNls String templateName, @NonNls Map<String, Object> context) {
        return TemplateUtilities.generateCode(getProject(), templateName, context);
    }


    public static String arrayBrackets(int arrayDepth) {
        return "[]".repeat(Math.max(0, arrayDepth));
    }
}
