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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public final class WrapperStatementBuilder {
	private final ProjectRef project;

	public WrapperStatementBuilder(@NotNull Project project) {
		this.project = ProjectRef.of(project);
	}

	@NotNull
	public Project getProject() {
		return ProjectRef.ensure(project);
	}

	private List<String> createSQLTypes(Wrapper wrapper) {
		List<String> sqlTypes = new ArrayList<>();
		@NonNls Properties properties = new Properties();

		Set<String> sqlTypeNames = wrapper.getSqlTypeNames();
		sqlTypeNames.clear();

		for (ClassWrapper classWrapper : wrapper.getClasses()) {
			SqlComplexType sct = classWrapper.getCorrespondingSqlType();
			String sqlTypeName = sct.getName();

			if (sqlTypeNames.contains(sqlTypeName)) continue;
			sqlTypeNames.add(sqlTypeName);

			String fields = sct.getFields().stream()
					.sorted(Comparator.comparingInt(SqlComplexType.Field::getFieldIndex))
					.map(e -> e.getName() + " " + e.getType())
					.collect(Collectors.joining(",\n\t"));

			properties.setProperty("TYPENAME", sqlTypeName);
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
		List<String> javaConverterMethods = new ArrayList<>();

		String objArray ="objArray";
		String javaObj = "javaObj";
		for (ClassWrapper classWrapper : wrapper.getClasses()) {
			// Skip RETURN attributes and duplicates based on SQL type name.
			if (classWrapper.getAttributeDirection() == ClassWrapper.AttributeDirection.RETURN)
				continue;

			Map<String, Object> context = new HashMap<>();
			context.put("JAVA_COMPLEX_TYPE", classWrapper.getJavaClassName());
			context.put("CONVERTER_METHOD_NAME", classWrapper.getSqlToJavaConverterName());
			context.put("OBJ_ARRAY",objArray);
			context.put("JAVA_OBJECT",javaObj);

			String code;
			if (classWrapper.isArray()) {
				// For arrays, set typecasting properties
				String squareBrackets = String.join("", Collections.nCopies(classWrapper.getArrayDepth() - 1, "[]"));
				String iterator = "i";
				String iterationCode = buildSqlArrayToJavaAssignmentLine(classWrapper,javaObj,objArray,iterator,wrapper);

				context.put("SQUARE_BRACKETS", squareBrackets);
				context.put("ITERATOR",iterator);
				context.put("ITERATION_CODE",iterationCode);

				code = generateCode("DBN - OJVM SQLArrayToJava.java", context);
			}
			else {
				List<String> fieldAssignments = new ArrayList<>();
				if (classWrapper.getFields() != null && !classWrapper.getFields().isEmpty()) {
				  for (FieldWrapper fieldWrapper : classWrapper.getFields()) {
					  String assignmentLine = buildSqlToJavaAssignmentLine(fieldWrapper, javaObj, objArray, wrapper);
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


	private List<String> createJavaToSQL(Wrapper wrapper) {
		List<String> javaConverterMethods = new ArrayList<>();
		if (wrapper.getMethods().isEmpty()) return javaConverterMethods;

		String objArray ="objArray";
		String javaObj = "javaObj";
		for (ClassWrapper classWrapper : wrapper.getClasses()) {
			if (classWrapper.getAttributeDirection() == ClassWrapper.AttributeDirection.ARGUMENT) continue;

			Map<String, Object> context = new HashMap<>();

			context.put("JAVA_COMPLEX_TYPE", classWrapper.getJavaClassName());
			context.put("SQL_OBJECT_TYPE", classWrapper.getCorrespondingSqlType().getName());
			context.put("CONVERTER_METHOD_NAME", classWrapper.getJavaToSqlConverterName());
			context.put("OBJ_ARRAY",objArray);
			context.put("JAVA_OBJECT",javaObj);

			String code;
			if (classWrapper.isArray()) {
				String squareBrackets = String.join("", Collections.nCopies(classWrapper.getArrayDepth() - 1, "[]"));
				String iterator = "i";
				String iterationCode = buildJavaArrayToSqlAssignmentLine(classWrapper,objArray,javaObj,iterator,wrapper);

				context.put("SQUARE_BRACKETS", squareBrackets);
				context.put("ITERATOR",iterator);
				context.put("ITERATION_CODE",iterationCode);
				code = generateCode("DBN - OJVM JavaArrayToSQL.java", context);
			} else {
				context.put("TOTAL_FIELDS", classWrapper.getFields().size());
				List<String> fieldAssignments = new ArrayList<>();
				if (classWrapper.getFields() != null && !classWrapper.getFields().isEmpty()) {
					for (FieldWrapper fieldWrapper : classWrapper.getFields()) {
						String assignmentLine = buildJavaToSqlAssignmentLine(fieldWrapper, objArray, javaObj, wrapper);
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

	private List<String> createJavaWrapperMethods(Wrapper wrapper) {
		List<String> javaWrapperMethods = new ArrayList<>();
		if (wrapper.getMethods().isEmpty()) return javaWrapperMethods;

		for (MethodWrapper method : wrapper.getMethods()) {
			String code;
			String methodReturnType = resolveMethodReturnType(method);
			String wrapperMethodName = method.getJavaMethodName();
			String methodSignature = getJavaSignature(method, true);
			String argumentConversions = getArgumentConversionStatements(method);
			String returnStatement = getReturnStatement(method, wrapper.getClassName());

			Map<String, Object> context = new HashMap<>();
			context.put("METHOD_RETURN_TYPE", methodReturnType);
			context.put("WRAPPER_METHOD_NAME", wrapperMethodName);
			context.put("METHOD_SIGNATURE", methodSignature);
			context.put("ARGUMENT_CONVERSIONS", argumentConversions);
			context.put("RETURN_STATEMENT", returnStatement);

			code = generateCode("DBN - OJVM JavaWrapperMethod.java", context);

			javaWrapperMethods.add(code);
		}
		return javaWrapperMethods;
	}

	@NonNls
	@NotNull
	private String createJavaWrapper(Wrapper wrapper) {
		List<String> sqlMethods = createSQLToJava(wrapper);
		List<String> javaMethods = createJavaToSQL(wrapper);
		List<String> javaWrapperMethods = createJavaWrapperMethods(wrapper);

		Map<String, Object> context = new HashMap<>();

		context.put("JAVA_WRAPPER_NAME", wrapper.getJavaWrapperClassName());
		context.put("SQL_CONVERSION_METHODS", sqlMethods);
		context.put("JAVA_CONVERSION_METHODS", javaMethods);
		context.put("JAVA_WRAPPER_METHODS", javaWrapperMethods);
		context.put("FULLY_QUALIFIED_ORIGINAL_CLASSNAME",wrapper.getClassName());

		context.put("JAVA_CLASS", wrapper.getClassName());

		context.put("WRAPPER_METHODS", wrapper.getMethods());

		return generateCode("DBN - OJVM JavaWrapper.java", context);

	}

	private String createSQLWrapper(Wrapper wrapper) {
		Map<String, Object> context = new HashMap<>();
		context.put("SQL_WRAPPER_NAME", wrapper.getSQLWrapperName());
		context.put("JAVA_WRAPPER_NAME", wrapper.getJavaWrapperClassName());
		// Transform each WrapperJavaMethod into a map with precomputed values.
		List<Map<String, Object>> methodList = wrapper.getMethods().stream()
				.map(method -> {
					Map<String, Object> m = new HashMap<>();
					m.put("javaMethodName", method.getJavaMethodName());
					m.put("sqlMethodName", method.getSqlMethodName());
					// Precompute the SQL signature using your new getSqlSignature function.
					m.put("sqlSignature", getSqlSignature(method));
					// Precompute the Java signature (false indicates no argument names, per your original code).
					m.put("javaSignature", getJavaSignature(method, false));

					// Determine the resolved return type.
					String resolvedReturnType = resolveMethodReturnType(method);
					m.put("resolvedReturnType", resolvedReturnType);

					// Flag to simplify the template logic.
					boolean isVoid = "void".equals(resolvedReturnType);
					m.put("isVoid", isVoid);

					// For non-void methods, pass along the SQL return type from the original return type.
					if (!isVoid && method.getReturnParameter() != null) {
						m.put("sqlReturnType", method.getReturnParameter().getSqlTypeName());
					}
					return m;
				})
				.collect(Collectors.toList());

		context.put("JAVA_METHODS", methodList);
		context.put("IS_PACKAGE_FORMAT", wrapper.isUseFriendlyNames());

        return generateCode("DBN - OJVM SQLWrapper.sql", context);
	}

	public String buildWrapperCreationStatement(Wrapper wrapper) {
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

		return sqlCode;
	}

	public String buildWrapperRemovalStatement(Wrapper wrapper) {
		Properties properties = new Properties();

		boolean isFunction = wrapper.getMethods().get(0).getReturnParameter() != null
				&& wrapper.getMethods().get(0).getReturnParameter().getJavaTypeName() != null;
		properties.setProperty("TYPE", isFunction ? "FUNCTION" : "PROCEDURE");

		Set<String> sqlTypeNames = wrapper.getSqlTypeNames();
		String allTypes = String.join(",", sqlTypeNames);
		properties.setProperty("SQLTYPES", allTypes);
		properties.setProperty("SQL_WRAPPER_NAME", wrapper.getSQLWrapperName());
		properties.setProperty("JAVA_WRAPPER_NAME", wrapper.getJavaWrapperClassName());
		return generateCode("DBN - OJVM SQLCleanup.sql", properties);
	}

	public String buildSqlToJavaAssignmentLine(FieldWrapper fieldWrapper,
											   String targetName, String arrayName,
											   Wrapper wrapper) {
		StringBuilder line = new StringBuilder();

		// Determine assignment operator and line terminator based on access modifier.
		String assignmentOperatorStart = "."+ fieldWrapper.getName() +"=";
		String assignmentOperatorEnd = "";
		String lineTerminator = ";";

		if(fieldWrapper.getAccessModifier() != FieldWrapper.AccessModifier.PUBLIC) {
			if(fieldWrapper.getSetter() == null || fieldWrapper.getSetter().isEmpty())
				targetName = "//" + targetName;
			else
			{
				assignmentOperatorStart = "."+ fieldWrapper.getSetter()+"(";
				assignmentOperatorEnd = ")";
			}

		}

		// Build conversion expression.
		String conversionPrefix = "";
		String conversionSuffix = "";
		if (fieldWrapper.isComplexType()) {
			// For complex types, wrap the SQL element with the proper converter.
			String converterMethod = wrapper.getClasses()
					.get(fieldWrapper.getComplexTypeIndexInWrapper())
					.getSqlToJavaConverterName();
			conversionPrefix = converterMethod + "(" +
					(fieldWrapper.isArray() ? "(java.sql.Array)" : "(java.sql.Struct)") + "(";
			conversionSuffix = "))";
		}

		String typeCastStart = (fieldWrapper.getTypeCastStart() != null) ? fieldWrapper.getTypeCastStart() : "";
		String typeCastEnd = (fieldWrapper.getTypeCastEnd() != null) ? fieldWrapper.getTypeCastEnd() : "";

		// Build the value expression.
		String valueExpression = (typeCastStart)
				+ conversionPrefix
				+ arrayName + "[" + fieldWrapper.getFieldIndex() + "]"
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
											   Wrapper wrapper) {
		StringBuilder line = new StringBuilder();

		// Determine assignment operator and line terminator based on access modifier.
		String assignmentOperator = "["+ fieldWrapper.getFieldIndex() +"] =";
		String lineTerminator = ";";
		String fieldAccessor = "."+ fieldWrapper.getName();

		if(fieldWrapper.getAccessModifier() != FieldWrapper.AccessModifier.PUBLIC) {
			if(fieldWrapper.getGetter() == null || fieldWrapper.getGetter().isEmpty())
				targetArray = "//" + targetArray;
			else
				fieldAccessor = "."+ fieldWrapper.getGetter()+"()";
		}

		String conversionStart = "";
		String conversionEnd = "";

		if(fieldWrapper.isArray() || fieldWrapper.isComplexType()) {
			String converterName =
					wrapper.getClasses()
							.get(fieldWrapper.getComplexTypeIndexInWrapper())
							.getJavaToSqlConverterName();
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
													String iterator, Wrapper wrapper) {
		StringBuilder line = new StringBuilder();

		// Determine assignment operator and line terminator based on access modifier.
		String assignmentOperator = " = ";
		String lineTerminator = ";";

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
			SqlType sqlType = TypeMappings.getSqlType(classWrapper.getJavaClassName());

			if (sqlType != null) {
				getValueStart = sqlType.getTransformerPrefix();
				getValueEnd = sqlType.getTransformerSuffix();
			}
			else
			{
				conversionPrefix = wrapper.getClasses().get(classWrapper.getContainedJavaComplexTypeIndex())
						.getSqlToJavaConverterName()+"((java.sql.Struct)(";
				conversionSuffix = "))";
			}
		}
		else {
			// Multi-dimensional
			conversionPrefix = wrapper.getClasses().get(classWrapper.getContainedJavaComplexTypeIndex())
					.getSqlToJavaConverterName()+"((java.sql.Array)(";
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
													String iterator, Wrapper wrapper) {
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

		if (classWrapper.getContainedJavaComplexTypeIndex() != -1) {
			String converterMethodName = wrapper
					.getClasses()
					.get(classWrapper.getContainedJavaComplexTypeIndex())
					.getJavaToSqlConverterName();
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

	public String getSqlSignature(MethodWrapper method) {
		AtomicInteger idx = new AtomicInteger(0);
		return method.getParameters()
				.stream()
				.map(e -> "arg_" + idx.getAndIncrement() + " " + e.getSqlTypeName())
				.collect(Collectors.joining(", "));
	}

	public String getJavaSignature(MethodWrapper method, boolean includeArgumentNames){

		AtomicInteger idx = new AtomicInteger(0);
		return method.getParameters()
				.stream()
				.map(e -> (
						e.isArray() ? "java.sql.Array" : e.isComplexType() ? "java.sql.Struct" : e.getJavaTypeName())
						+ (includeArgumentNames ? " arg" + idx.getAndIncrement(): "")
				)
				.collect(Collectors.joining(", "));
	}

	public String getArgumentsInJavaCaller(MethodWrapper method) {
		AtomicInteger idx = new AtomicInteger(0);
		return method.getParameters()
				.stream()
				.map(e -> " arg" + idx.getAndIncrement() +
						((e.isArray() || e.isComplexType()) ? "Java" :""))
				.collect(Collectors.joining(", "));
	}


	public String getArgumentConversionStatements(MethodWrapper method) {
		StringBuilder statements = new StringBuilder();
		List<ParameterWrapper> methodAttributes = method.getParameters();
		for (int i = 0; i < methodAttributes.size(); i++) {
			ParameterWrapper methodAttribute = methodAttributes.get(i);
			if (methodAttribute.isArray() || methodAttribute.isComplexType()) {
				// Build the type string with array dimensions if applicable.
				StringBuilder typeBuilder = new StringBuilder(methodAttribute.getJavaTypeName());
				if (methodAttribute.isArray())
					typeBuilder.append("[]".repeat(Math.max(0, methodAttribute.getArrayDepth())));

				// Construct the conversion statement.
				statements.append(typeBuilder)
						.append(" arg").append(i).append("Java = ")
						.append(methodAttribute.getConverterName())
						.append("(arg").append(i).append(");")
						.append("\n");
			}
		}
		return statements.toString();
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
		return returnType.getJavaTypeName();
	}

	public String getReturnStatement(MethodWrapper method, String fullyQualifiedOriginalClassName) {
		ParameterWrapper returnType = method.getReturnParameter();
		StringBuilder statement = new StringBuilder();
		if(returnType != null){
			statement.append("return ");
		}
		String converterMethodStart = "";
		String converterMethodEnd = ";";
		if (returnType != null && (returnType.isArray() || returnType.isComplexType())) {
			converterMethodStart = returnType.getConverterName() + "(";
			converterMethodEnd = ");";
		}


		statement.append(converterMethodStart)
				.append(fullyQualifiedOriginalClassName).append(".")
				.append(method.getOriginalJavaMethodName())
				.append("(")
				.append(getArgumentsInJavaCaller(method))
				.append(")")
				.append(converterMethodEnd);
		return statement.toString();
	}

	private String generateCode(@NonNls String templateName, Properties properties) {
		return TemplateUtilities.generateCode(getProject(), templateName, properties);
	}

	private String generateCode(@NonNls String templateName, Map<String, Object> context) {
		return TemplateUtilities.generateCode(getProject(), templateName, context);
	}

}
