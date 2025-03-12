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
import com.dbn.execution.java.wrapper.Wrapper.MethodAttribute;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.dbn.execution.java.wrapper.TypeMappings.getSqlType;

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

		for (JavaComplexType jct : wrapper.getArgumentJavaComplexTypes()) {
			SqlComplexType sct = jct.getCorrespondingSqlType();
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
		List<String> javaMethods = new ArrayList<>();
		Set<String> addedJavaTypes = new HashSet<>();

		AtomicInteger idx = new AtomicInteger(0);
		for (JavaComplexType jct : wrapper.getArgumentJavaComplexTypes()) {
			if (jct.getAttributeDirection() == JavaComplexType.AttributeDirection.RETURN) continue;

			if (addedJavaTypes.contains(jct.getCorrespondingSqlType().getName()))
				continue;
			addedJavaTypes.add(jct.getCorrespondingSqlType().getName());

			@NonNls
			Properties properties = new Properties();

			properties.setProperty("JAVA_COMPLEX_TYPE", jct.getJavaClassName());
			String code;
			if (jct.isArray()) {
				properties.setProperty("SQL_OBJECT_TYPE", jct.getCorrespondingSqlType().getName());
				if(jct.getFields().isEmpty()){
					SqlType sqlType = getSqlType(jct.getJavaClassName());
					properties.setProperty("TYPECAST_START", sqlType.getTransformerPrefix());
					properties.setProperty("TYPECAST_END", sqlType.getTransformerSuffix());
				} else {
					JavaComplexType.Field field = jct.getFields().get(0);
					properties.setProperty("TYPECAST_START", field.getTypeCastStart());
					properties.setProperty("TYPECAST_END", field.getTypeCastEnd());
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
									return setterMethod + ";" + e.getSqlType() + "toJava( (java.sql.Struct) objArray[ " + e.getFieldIndex() + " ]" + ")" + ";" + end;
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

		@NonNls
		Properties properties = new Properties();

		for (JavaComplexType jct : wrapper.getArgumentJavaComplexTypes()) {
			if (jct.getAttributeDirection() == JavaComplexType.AttributeDirection.ARGUMENT) continue;
			properties.setProperty("JAVA_COMPLEX_TYPE", jct.getJavaClassName());
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
								return e.getFieldIndex() + ";" + getterMethod + ";" + e.getSqlType();
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

	@NonNls
	@NotNull
	private String createJavaWrapper(Wrapper wrapper) {
		List<String> sqlMethods = createSQLToJava(wrapper);
		List<String> javaMethods = createJavaToSQL(wrapper);

		@NonNls
		Properties properties = new Properties();

		properties.setProperty("JAVA_WRAPPER_NAME", wrapper.getJavaWrapperClassName());
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
						return e.getJavaTypeName() + "[]" + ";" + e.getSqlTypeName() + ";" + "arg" + idx.getAndIncrement();
					} else if (e.isComplexType()) {
						return e.getJavaTypeName() + ";" + e.getSqlTypeName() + ";" + "arg" + idx.getAndIncrement();
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
		String javaReturnType = "";
		String returnConversionMethod = "";
		String arrayConversionMethod = "";
		MethodAttribute returnType = wrapper.getReturnType();

		if (returnType != null) {
			isComplexReturnType = returnType.isComplexType();
			if (returnType.isArray()) {
				isArrayReturn = true;
				javaReturnType = "java.sql.Array";
				arrayConversionMethod = returnType.getSqlTypeName();
			} else if (isComplexReturnType) {
				javaReturnType = "java.sql.Struct";
				returnConversionMethod = returnType.getSqlTypeName();
			} else {
				javaReturnType = returnType.getJavaTypeName();
			}
		}

		properties.setProperty("METHOD_RETURN_TYPE", javaReturnType);
		properties.setProperty("IS_ARRAY_RETURN", String.valueOf(isArrayReturn));
		properties.setProperty("ARRAY_RETURN_JAVA_CONVERSION", arrayConversionMethod);
		properties.setProperty("IS_COMPLEX_RETURN", String.valueOf(isComplexReturnType));
		properties.setProperty("RETURN_JAVA_CONVERSION", returnConversionMethod);

		return generateCode("DBN - OJVM JavaWrapper.java", properties);
	}

	private String createSQLWrapper(Wrapper wrapper) {
		@NonNls Properties properties = new Properties();
		boolean isFunction = wrapper.getReturnType() != null && wrapper.getReturnType().getJavaTypeName() != null;
		properties.setProperty("SQL_WRAPPER_NAME", wrapper.getSQLWrapperName());
		properties.setProperty("JAVA_WRAPPER_NAME", wrapper.getJavaWrapperClassName());
		properties.setProperty("TYPE", isFunction ? "FUNCTION" : "PROCEDURE");
		properties.setProperty("METHOD", wrapper.getWrappedJavaMethodName());

		AtomicInteger idx = new AtomicInteger(0);
		String sqlSignature = wrapper.getMethodArguments()
				.stream()
				.map(e -> "arg_" + idx.getAndIncrement() + " " + e.getSqlTypeName())
				.collect(Collectors.joining(", "));

		String javaSignature = wrapper.getJavaSignature(false);

		properties.setProperty("SQL_SIGNATURE", sqlSignature);
		properties.setProperty("RETURN", isFunction ? wrapper.getReturnType().getSqlTypeName() : "");

		properties.setProperty("JAVA_METHOD_ARGS", javaSignature);

		String methodReturnType = "";
		if (wrapper.getReturnType() != null) {
			if (wrapper.getReturnType().isArray())
				methodReturnType = "java.sql.Array";
			else if (wrapper.getReturnType().isComplexType())
				methodReturnType = "java.sql.Struct";
			else
				methodReturnType = wrapper.getReturnType().getJavaTypeName();
		}
		properties.setProperty("JAVA_METHOD_RETURN", methodReturnType);

		return generateCode("DBN - OJVM SQLWrapper.sql", properties);
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

		boolean isFunction = wrapper.getReturnType() != null && wrapper.getReturnType().getJavaTypeName() != null;
		properties.setProperty("TYPE", isFunction ? "FUNCTION" : "PROCEDURE");

		Set<String> sqlTypeNames = wrapper.getSqlTypeNames();
		String allTypes = String.join(",", sqlTypeNames);
		properties.setProperty("SQLTYPES", allTypes);
		properties.setProperty("SQL_WRAPPER_NAME", wrapper.getSQLWrapperName());
		properties.setProperty("JAVA_WRAPPER_NAME", wrapper.getJavaWrapperClassName());

		return generateCode("DBN - OJVM SQLCleanup.sql", properties);
	}

	private String generateCode(@NonNls String templateName, Properties properties) {
		return TemplateUtilities.generateCode(getProject(), templateName, properties);
	}
}
