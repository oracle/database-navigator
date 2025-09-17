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

import static com.dbn.common.util.Java.isPrimitive;

@Slf4j
public final class WrapperStatementBuilder {
    private final ProjectRef project;
    private final String javaInitializedArgPrefix = "custom";

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

        String objArray = "objArray";
        String javaObj = "javaObj";
        for (ClassWrapper classWrapper : model.getClasses()) {
            // Skip RETURN attributes and duplicates based on SQL type name.
            if (classWrapper.getArgumentDirection() == ClassWrapper.ArgumentDirection.OUT)
                continue;

            Map<String, Object> context = new HashMap<>();
            context.put("JAVA_COMPLEX_TYPE", classWrapper.getClassName());
            context.put("CONVERTER_METHOD_NAME", classWrapper.getSqlToJavaConverterName());
            context.put("OBJ_ARRAY", objArray);
            context.put("JAVA_OBJECT", javaObj);

            String code;
            if (classWrapper.isArray()) {
                // For arrays, set typecasting properties
                String squareBrackets = String.join("", Collections.nCopies(classWrapper.getArrayDepth() - 1, "[]"));
                String iterator = "i";
                String iterationCode = buildSqlArrayToJavaAssignmentLine(classWrapper, javaObj, objArray, iterator);

                context.put("SQUARE_BRACKETS", squareBrackets);
                context.put("ITERATOR", iterator);
                context.put("ITERATION_CODE", iterationCode);

                code = generateCode("DBN - OJVM SQLArrayToJava.java", context);
            } else {
                List<String> fieldAssignments = new ArrayList<>();
                if (classWrapper.getFields() != null && !classWrapper.getFields().isEmpty()) {
                    for (FieldWrapper fieldWrapper : classWrapper.getFields()) {
                        String assignmentLine = buildSqlToJavaAssignmentLine(fieldWrapper, javaObj, objArray, model);
                        fieldAssignments.add(assignmentLine);
                    }
                    context.put("FIELD_ASSIGNMENTS", fieldAssignments);
                }

                code = generateCode("DBN - OJVM SQLObjectToJava.java", context);
            }
            javaConverterMethods.add(code);
        }
        return javaConverterMethods;
    }


    private List<String> createJavaToSQL(WrapperModel model) {
        List<String> javaConverterMethods = new ArrayList<>();
        if (model.getMethods().isEmpty()) return javaConverterMethods;

        String objArray = "objArray";
        String javaObj = "javaObj";
        for (ClassWrapper classWrapper : model.getClasses()) {
            if (classWrapper.getArgumentDirection() == ClassWrapper.ArgumentDirection.IN) continue;

            Map<String, Object> context = new HashMap<>();

            context.put("JAVA_COMPLEX_TYPE", classWrapper.getClassName());
            context.put("SQL_OBJECT_TYPE", classWrapper.getSqlTypeName());
            context.put("CONVERTER_METHOD_NAME", classWrapper.getJavaToSqlConverterName());
            context.put("OBJ_ARRAY", objArray);
            context.put("JAVA_OBJECT", javaObj);

            String code;
            if (classWrapper.isArray()) {
                String squareBrackets = String.join("", Collections.nCopies(classWrapper.getArrayDepth() - 1, "[]"));
                String iterator = "i";
                String iterationCode = buildJavaArrayToSqlAssignmentLine(classWrapper, objArray, javaObj, iterator, model);

                context.put("SQUARE_BRACKETS", squareBrackets);
                context.put("ITERATOR", iterator);
                context.put("ITERATION_CODE", iterationCode);
                code = generateCode("DBN - OJVM JavaArrayToSQL.java", context);
            } else {
                context.put("TOTAL_FIELDS", classWrapper.getFields().size());
                List<String> fieldAssignments = new ArrayList<>();
                if (classWrapper.getFields() != null && !classWrapper.getFields().isEmpty()) {
                    for (FieldWrapper fieldWrapper : classWrapper.getFields()) {
                        String assignmentLine = buildJavaToSqlAssignmentLine(fieldWrapper, objArray, javaObj, model);
                        fieldAssignments.add(assignmentLine);
                    }
                    context.put("FIELD_ASSIGNMENTS", fieldAssignments);
                }
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
            String argumentConversions = buildArgumentConversions(method);
            String methodInvocation = buildMethodInvocation(method, model.getClassName());

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
        Map<String, Object> context = new HashMap<>();
        context.put("SQL_WRAPPER_NAME", model.getSqlWrapperName());
        context.put("JAVA_WRAPPER_NAME", model.getJavaWrapperName());
        // Transform each WrapperJavaMethod into a map with precomputed values.
        List<Map<String, Object>> methodList = model.getMethods().stream()
                .map(method -> {
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
        Properties properties = new Properties();

        boolean isFunction = model.getMethods().get(0).getReturnParameter() != null
                && model.getMethods().get(0).getReturnParameter().getJavaTypeName() != null;
        properties.setProperty("TYPE", isFunction ? "FUNCTION" : "PROCEDURE");

        Set<String> sqlTypeNames = model.getSqlTypeNames();
        String allTypes = String.join(",", sqlTypeNames);
        properties.setProperty("SQLTYPES", allTypes);
        properties.setProperty("SQL_WRAPPER_NAME", model.getSqlWrapperName());
        properties.setProperty("JAVA_WRAPPER_NAME", model.getJavaWrapperName());
        return generateCode("DBN - OJVM SQLCleanup.sql", properties);
    }

    public String buildSqlToJavaAssignmentLine(FieldWrapper fieldWrapper,
                                               String targetName, String arrayName,
                                               WrapperModel model) {
        StringBuilder line = new StringBuilder();

        // Determine assignment operator and line terminator based on access modifier.
        String assignmentOperatorStart = "." + fieldWrapper.getName() + "=";
        String assignmentOperatorEnd = "";
        String lineTerminator = ";";

        if (!isPrimitive(fieldWrapper.getTypeClassName())) {
            //check if value is not null before accessing it
            line.append("  if(")
                    .append(arrayName)
                    .append("[" + fieldWrapper.getIndex() + "]")
                    .append(" != null){");
            lineTerminator = ";}";
        }


        if (!fieldWrapper.isAccessible()) {
            if (fieldWrapper.getSetterName() == null)
                targetName = "//" + targetName;
            else {
                assignmentOperatorStart = "." + fieldWrapper.getSetterName() + "(";
                assignmentOperatorEnd = ")";
            }

        }

        // Build conversion expression.
        String conversionPrefix = "";
        String conversionSuffix = "";
        if (fieldWrapper.isComplexType()) {
            // For complex types, wrap the SQL element with the proper converter.
            ClassWrapper classWrapper = model.getFieldClassWrapper(fieldWrapper);
            String converterMethod = classWrapper.getSqlToJavaConverterName();

            conversionPrefix = converterMethod + "(" +
                    (fieldWrapper.isArray() ? "(java.sql.Array)" : "(java.sql.Struct)") + "(";
            conversionSuffix = "))";
        }

        String typeCastStart = ((fieldWrapper.getTypeCastStart() != null) && (!fieldWrapper.isComplexType())) ?
                fieldWrapper.getTypeCastStart() : "";
        String typeCastEnd = ((fieldWrapper.getTypeCastEnd() != null) && (!fieldWrapper.isComplexType())) ?
                fieldWrapper.getTypeCastEnd() : "";

        // Build the value expression.
        String valueExpression = (typeCastStart)
                + conversionPrefix
                + arrayName + "[" + fieldWrapper.getIndex() + "]"
                + conversionSuffix
                + typeCastEnd;

        // Complete the assignment line.
        line.append(targetName)
                .append(assignmentOperatorStart)
                .append(valueExpression)
                .append(assignmentOperatorEnd)
                .append(lineTerminator);

        return line.toString();
    }


    public String buildJavaToSqlAssignmentLine(FieldWrapper fieldWrapper,
                                               String targetArray, String javaObj,
                                               WrapperModel model) {
        StringBuilder line = new StringBuilder();

        // Determine assignment operator and line terminator based on access modifier.
        String assignmentOperator = "[" + fieldWrapper.getIndex() + "] =";
        String lineTerminator = ";";
        String fieldAccessor = "." + fieldWrapper.getName();

        if (!fieldWrapper.isAccessible()) {
            if (fieldWrapper.getGetterName() == null)
                targetArray = "//" + targetArray;
            else
                fieldAccessor = "." + fieldWrapper.getGetterName() + "()";
        }

        String conversionStart = "";
        String conversionEnd = "";

        if (fieldWrapper.isArray() || fieldWrapper.isComplexType()) {
            ClassWrapper classWrapper = model.getFieldClassWrapper(fieldWrapper);
            String converterName = classWrapper.getJavaToSqlConverterName();
            conversionStart = converterName + "(";
            conversionEnd = ")";
        }

        // Build the value expression.
        String valueExpression = conversionStart
                + javaObj
                + fieldAccessor
                + conversionEnd;

        // Complete the assignment line.
        line.append(targetArray)
                .append(assignmentOperator)
                .append(valueExpression)
                .append(lineTerminator);

        return line.toString();
    }

    public String buildSqlArrayToJavaAssignmentLine(ClassWrapper classWrapper,
                                                    String targetName, String arrayName,
                                                    String iterator) {
        StringBuilder line = new StringBuilder();

        // Determine assignment operator and line terminator based on access modifier.
        String assignmentOperator = " = ";
        String lineTerminator = ";";

        if (!isPrimitive(classWrapper.getClassName())) {
            //check if value is not null before accessing it
            line.append("  if(")
                    .append(arrayName)
                    .append("[").append(iterator).append("]")
                    .append(" != null){");
            lineTerminator = ";}";
        }

        // Begin the assignment statement.
        line.append(targetName)
                .append("[").append(iterator).append("]")
                .append(assignmentOperator);

        // Build conversion expression.
        String conversionPrefix = "";
        String conversionSuffix = "";

        String getValueStart = "";
        String getValueEnd = "";

        if (classWrapper.getArrayDepth() <= 1) {
            SqlType sqlType = TypeMappings.getSqlType(classWrapper.getClassName());

            if (sqlType != null) {
                getValueStart = sqlType.getTransformerPrefix();
                getValueEnd = sqlType.getTransformerSuffix();
            } else {
                conversionPrefix = classWrapper.getContainedClassWrapper().getSqlToJavaConverterName() + "((java.sql.Struct)(";
                conversionSuffix = "))";
            }
        } else {
            // Multi-dimensional
            conversionPrefix = classWrapper.getContainedClassWrapper().getSqlToJavaConverterName() + "((java.sql.Array)(";
            conversionSuffix = "))";
        }

        // Build the value expression.
        String valueExpression = getValueStart
                + conversionPrefix
                + arrayName + "[" + iterator + "]"
                + conversionSuffix
                + getValueEnd;

        // Complete the assignment line.
        line.append(valueExpression).append(lineTerminator);

        return line.toString();
    }

    public String buildJavaArrayToSqlAssignmentLine(ClassWrapper classWrapper,
                                                    String targetSqlArray, String javaArrayName,
                                                    String iterator, WrapperModel model) {
        StringBuilder line = new StringBuilder();

        // Determine assignment operator
        String assignmentOperator = " = ";
        String lineTerminator = ";";

        // Begin the assignment statement.
        line.append(targetSqlArray)
                .append("[").append(iterator).append("]")
                .append(assignmentOperator);

        // Build conversion expression.
        String conversionPrefix = "";
        String conversionSuffix = "";

        ClassWrapper containedClassWrapper = classWrapper.getContainedClassWrapper();
        if (containedClassWrapper != null) {
            String converterMethodName = containedClassWrapper.getJavaToSqlConverterName();
            conversionPrefix = converterMethodName + "(";
            conversionSuffix = ")";
        }

        // Build the value expression.
        String valueExpression = conversionPrefix
                + javaArrayName + "[" + iterator + "]"
                + conversionSuffix;

        // Complete the assignment line.
        line.append(valueExpression).append(lineTerminator);

        return line.toString();
    }

    //methods for supporting wrapper creation

    public String getSqlParameters(MethodWrapper method) {
        List<ParameterWrapper> parameters = method.getParameters();
        // Filter out injected parameters
        List<ParameterWrapper> filtered =
                parameters.stream()
                        .filter(e -> !e.isJavaInjection())
                        .collect(Collectors.toList());
        if (filtered.isEmpty()) return "";
        AtomicInteger idx = new AtomicInteger(0);
        return "(" + filtered
                .stream()
                .map(e -> "arg_" + idx.getAndIncrement() + " " + e.getSqlTypeName())
                .collect(Collectors.joining(", ")) + ")";
    }

    public String buildMethodParameters(MethodWrapper method,
                                        boolean includeArgumentNames) {
        List<ParameterWrapper> params = method.getParameters();

        AtomicInteger idx = new AtomicInteger(0);

        return "(" + params.stream()
                .filter(p->!p.isJavaInjection()) // exclude Java-initialized args
                .map(p -> {
                    String mappedType = getMappedType(p);
                    return mappedType + (includeArgumentNames ? " arg" + idx.getAndIncrement() : "");
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
                .mapToObj(i -> {
                    ParameterWrapper p = params.get(i);
                    return getArgumentNameInJavaCaller(p, i);
                })
                .collect(Collectors.joining(", "));
    }

    public String getArgumentNameInJavaCaller(ParameterWrapper p, int paramIndex) {
        if (p.isJavaInjection())
            return getJavaInitializedArgumentName(paramIndex) ;

        StringBuilder name = new StringBuilder("arg").append(paramIndex);
        if (p.isArray() || p.isComplexType()) {
            name.append("Java");
        } else {
            String t = p.getJavaTypeName();
            if ("char".equals(t) || "java.lang.Character".equals(t)) {
                name.append("Char");
            }
        }
        return name.toString();
    }


    public String buildArgumentConversions(MethodWrapper method) {
        StringBuilder statements = new StringBuilder();
        List<ParameterWrapper> methodAttributes = method.getParameters();

        for (int i = 0; i < methodAttributes.size(); i++) {
            ParameterWrapper methodAttribute = methodAttributes.get(i);
            String javaTypeName = methodAttribute.getJavaTypeName();
            if(methodAttribute.isJavaInjection()) {
                statements.append(methodAttribute.getJavaInjectedCode());
                statements.append(System.lineSeparator());
            }
            else if (methodAttribute.isArray() || methodAttribute.isComplexType()) {
                // Build the type string with array dimensions if applicable.
                StringBuilder typeBuilder = new StringBuilder(javaTypeName);
                if (methodAttribute.isArray())
                    typeBuilder.append("[]".repeat(Math.max(0, methodAttribute.getArrayDepth())));

                // Construct the conversion statement.
                statements.append(typeBuilder)
                        .append(" arg").append(i).append("Java = ")
                        .append(methodAttribute.getConverterName())
                        .append("(arg").append(i).append(");")
                        .append("\n");
            } else if ("char".equals(javaTypeName) || "java.lang.Character".equals(javaTypeName)) {
                statements.append(
                        getCharacterArgumentInitialization(javaTypeName, "arg", i)
                ).append("\n");
            }
        }
        return statements.toString();
    }

    private String getCharacterArgumentInitialization(String javaTypeName,
                                                      String originalArgName,
                                                      int argumentIndex) {

        String srcName = originalArgName + argumentIndex;
        String destName = "arg" + argumentIndex + "Char";
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
            if(returnType.isJavaInjection()) {
                String declaration = returnType.getJavaTypeName() +
                        "[]".repeat(returnType.getArrayDepth())
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

    private String getCharReturnStatement(String returnCaller, String javaTypeName) {
        if ("char".equals(javaTypeName)) {
            return "return String.valueOf(" + returnCaller + ");";
        }
        return  "java.lang.Character retChar = " + returnCaller + ";\n"
                + "return (retChar == null) ? null : String.valueOf(retChar);";
    }

    public String getJavaInitializedArgumentName(int index) {
        return javaInitializedArgPrefix + "arg" + index;
    }

    private String generateCode(@NonNls String templateName, Properties properties) {
        return TemplateUtilities.generateCode(getProject(), templateName, properties);
    }

    private String generateCode(@NonNls String templateName, Map<String, Object> context) {
        return TemplateUtilities.generateCode(getProject(), templateName, context);
    }

}
