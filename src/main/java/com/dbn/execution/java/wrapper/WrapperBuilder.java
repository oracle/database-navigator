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
 */

package com.dbn.execution.java.wrapper;

import com.dbn.execution.java.wrapper.JavaComplexType.AttributeDirection;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.DBOrderedObject;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

/**
 * Parses {@link DBJavaMethod} instances into {@link Wrapper} objects,
 * including generating the corresponding {@link JavaComplexType}
 * and associated {@link SqlComplexType}.
 *
 * <p>This implementation uses a Singleton pattern and keeps
 * all per-parse mutable state in local variables inside
 * {@link #build(DBJavaMethod)} so that it is thread-safe
 * if multiple threads call it simultaneously.</p>
 */
@Slf4j
public final class WrapperBuilder {

	/**
	 * The single, static instance of WrapperBuilder.
	 */
	private static final WrapperBuilder INSTANCE = new WrapperBuilder();

	public static final String newSqlTypePrepend = "DBN_OJVM_TYPE_";

	/**
	 * Private constructor to enforce Singleton usage.
	 */
	private WrapperBuilder() {
		// no-op
	}

	public static WrapperBuilder getInstance() {
		return INSTANCE;
	}


	/**
	 * Entry point for parsing a {@link DBJavaMethod} into a {@link Wrapper}.
	 *
	 * @param javaMethod The method definition to parse.
	 * @return The fully-populated {@link Wrapper}.
	 * @throws Exception if parsing fails or an unsupported cycle is detected.
	 */
	public Wrapper build(DBJavaMethod javaMethod) throws Exception {
		// Create data structures that are unique to *this* parse call.
		WrapperBuilderContext context = new WrapperBuilderContext();

		// Delegate to the internal parsing method.
		return buildInternal(javaMethod, context);
	}

	// -------------------------------------------------
	// Internal Parsing Logic
	// -------------------------------------------------

	/**
	 * Internal method that actually performs the parsing to produce a {@link Wrapper}.
	 *
	 * @param javaMethod         The method definition to parse.
	 * @param context contains the following
	 *  complexTypeConversion Mapping from className -> unique integer (for type naming).
	 *  complexTypeMap        Cache of complex types created so far during this parse.
	 *  complexTypeSet        Used to detect cycles during recursive type creation.
	 * @return The generated {@link Wrapper}.
	 */
	private Wrapper buildInternal(
			DBJavaMethod javaMethod,
			WrapperBuilderContext context) {
		// Create a fresh Wrapper for this invocation
		Wrapper wrapper = new Wrapper();

		setMethodMetadata(javaMethod, wrapper);
		parseParameters(javaMethod, wrapper, context);
		parseReturnType(javaMethod, wrapper, context);

		return wrapper;
	}

	/**
	 * Sets up the basic method metadata on the {@link Wrapper} object.
	 */
	private void setMethodMetadata(DBJavaMethod javaMethod, Wrapper wrapper) {
		String methodName = javaMethod.getName().split("#")[0];
		wrapper.setWrappedJavaMethodName(methodName);
		wrapper.setFullyQualifiedClassName(convertClassNameToDotNotation(javaMethod.getClassName()));
		// Replace "void" return in the signature with a more readable style, if present.

		String javaMethodSignature = javaMethod.getSignature().replace(": void", "").replace(":", " return");
		wrapper.setJavaMethodSignature(javaMethodSignature);
	}

	/**
	 * Parse all method parameters from the given {@link DBJavaMethod} and populate the wrapper.
	 */
	private void parseParameters(
			DBJavaMethod javaMethod,
			Wrapper wrapper,
			WrapperBuilderContext context) {
		List<DBJavaParameter> parameters = javaMethod.getParameters();
		if (parameters == null || parameters.isEmpty()) {
			return;
		}

		// Sort by position to ensure correct order
		parameters.sort(Comparator.comparingInt(DBOrderedObject::getPosition));

		// Create a Wrapper.MethodAttribute for each parameter
		for (DBJavaParameter parameter : parameters) {
			DBJavaClass parameterClass = parameter.getParameterClass();
			String className = (parameterClass == null) ? "" : parameterClass.getQualifiedName();

			Wrapper.MethodAttribute attr = createMethodAttribute(
					parameterClass,
					parameter.getParameterType(),
					className,
					parameter.getArrayDepth(),
					AttributeDirection.ARGUMENT,
					context,
					wrapper);
			wrapper.addMethodArgument(attr);
		}
	}

	/**
	 * Parse the return type (if not void) and populate the wrapper.
	 */
	private void parseReturnType(
			DBJavaMethod javaMethod,
			Wrapper wrapper,
			WrapperBuilderContext context) {
		if (!"void".equals(javaMethod.getReturnType())) {
			Wrapper.MethodAttribute returnAttr = createMethodAttribute(
					javaMethod.getReturnClass(),
					javaMethod.getReturnType(),
					javaMethod.getClassName(),
					javaMethod.getArrayDepth(),
					AttributeDirection.RETURN,
					context,
					wrapper
			);
			wrapper.setReturnType(returnAttr);
		}
	}

	/**
	 * Creates a {@link Wrapper.MethodAttribute} for the given DB elements, either
	 * a simple attribute if primitive/supported, or a complex type otherwise.
	 */
	private Wrapper.MethodAttribute createMethodAttribute(
			DBJavaClass javaClass,
			String type,
			String className,
			short arrayDepth,
			AttributeDirection attributeDirection,
			WrapperBuilderContext context,
			Wrapper wrapper) {
		String effectiveParameterType = getParameterType(type, className);

		// If non-array and we have a direct mapping -> simple attribute
		if (arrayDepth == 0 && TypeMappingsManager.isSupportedType(effectiveParameterType)) {
			return buildSimpleMethodAttribute(effectiveParameterType);
		}

		// Otherwise, build or retrieve a JavaComplexType
		JavaComplexType javaComplexType = (arrayDepth > 0) ?
				createJavaComplexArrayType(javaClass, effectiveParameterType, arrayDepth, attributeDirection, context, wrapper) :
				createJavaComplexType(javaClass, effectiveParameterType, attributeDirection, context, wrapper);

		if (javaComplexType == null) {
			// If still null, it's unsupported or cyclical
			return null;
		}

		// Build a complex attribute
		return buildComplexMethodAttribute(javaComplexType);
	}

	/**
	 * Builds a simple (non-complex) method attribute with a known SQL type mapping.
	 */
	private Wrapper.MethodAttribute buildSimpleMethodAttribute(String effectiveParameterType) {
		Wrapper.MethodAttribute methodAttribute = new Wrapper.MethodAttribute();
		methodAttribute.setArray(false);
		methodAttribute.setTypeName(convertClassNameToDotNotation(effectiveParameterType));

		SqlType sqlType = TypeMappingsManager.getCorrespondingSqlType(effectiveParameterType);
		methodAttribute.setCorrespondingSqlTypeName(sqlType.getSqlTypeName());
		methodAttribute.setComplexType(false);
		return methodAttribute;
	}

	/**
	 * Builds a method attribute that is backed by a {@link JavaComplexType}.
	 */
	private Wrapper.MethodAttribute buildComplexMethodAttribute(JavaComplexType javaComplexType) {
		Wrapper.MethodAttribute methodAttribute = new Wrapper.MethodAttribute();
		methodAttribute.setArray(javaComplexType.isArray());
		methodAttribute.setArrayDepth(javaComplexType.getArrayDepth());
		methodAttribute.setTypeName(convertClassNameToDotNotation(javaComplexType.getTypeName()));
		methodAttribute.setComplexType(true);

		SqlComplexType sqlType = javaComplexType.getCorrespondingSqlType();
		methodAttribute.setCorrespondingSqlTypeName((sqlType == null) ? "" : sqlType.getName());

		return methodAttribute;
	}

	// -------------------------------------------------
	// Complex Type Creation Logic
	// -------------------------------------------------

	/**
	 * Creates a {@link JavaComplexType} (non-array version) for the given class
	 * or parameter type, populating its fields recursively if needed.
	 */
	private JavaComplexType createJavaComplexType(
			DBJavaClass javaClass,
			String parameterType,
			AttributeDirection attributeDirection,
			WrapperBuilderContext context,
			Wrapper wrapper) {
		ComplexTypeKey key = buildComplexTypeKey(javaClass, parameterType, (short) 0);
		if (addToContextAndDetectCycle(key, context)) return null;

		JavaComplexType existing = getComplexTypeFromCache(key, attributeDirection, context);
		if (existing != null) {
			context.removeFromSet(key);
			return existing;
		}

		// Create a new complex type shell
		JavaComplexType javaComplexType = buildComplexTypeShell(javaClass, parameterType, attributeDirection, false);
		SqlComplexType sqlComplexType = new SqlComplexType();
		sqlComplexType.setArray(false);

		// Populate fields if we have a DBJavaClass
		if (javaClass != null) {
			populateComplexTypeFields(
					javaClass,
					attributeDirection,
					javaComplexType,
					sqlComplexType,
					context,
					wrapper);
		}

		// Finalize and return
		finalizeComplexType(key, javaComplexType, sqlComplexType, context, wrapper);
		return javaComplexType;
	}

	/**
	 * Creates a {@link JavaComplexType} for array types (including nested arrays),
	 * populating its contained type recursively if necessary.
	 */
	private JavaComplexType createJavaComplexArrayType(
			DBJavaClass dbJavaClass,
			String parameterType,
			short arrayDepth,
			AttributeDirection attributeDirection,
			WrapperBuilderContext context,
			Wrapper wrapper) {
		ComplexTypeKey key = buildComplexTypeKey(dbJavaClass, parameterType, arrayDepth);
		if (addToContextAndDetectCycle(key, context)) return null;

		JavaComplexType existing = getComplexTypeFromCache(key, attributeDirection, context);
		if (existing != null) {
			context.removeFromSet(key);
			return existing;
		}

		// Create new array-type shell
		JavaComplexType javaComplexType = buildComplexTypeShell(dbJavaClass, parameterType, attributeDirection, true);
		javaComplexType.setArrayDepth(arrayDepth);

		SqlComplexType sqlComplexType = new SqlComplexType();
		sqlComplexType.setArray(true);

		// If base type is unsupported, abort
		if (TypeMappingsManager.isUnSupportedType(parameterType)) {
			log.error("Encountered unsupported type for array: {}", parameterType);
			context.removeFromSet(key);
			return null;
		}

		// Identify the contained type name
		String sqlBaseName = (dbJavaClass != null) ? dbJavaClass.getName() : parameterType;

		String containedTypeName = null;
		JavaComplexType containedJavaComplexType;

		// Single-dimension vs multi-dimension array
		if (arrayDepth <= 1) {
			containedTypeName = getContainedTypeName(parameterType);
			if (containedTypeName == null) {
				// Possibly a nested complex type
				containedJavaComplexType = createJavaComplexType(dbJavaClass, parameterType, attributeDirection, context, wrapper);
				if (containedJavaComplexType != null) {
					containedTypeName = containedJavaComplexType.getCorrespondingSqlType().getName();
				}
			}
		} else {
			// Multi-dimensional
			containedJavaComplexType = createJavaComplexArrayType(dbJavaClass, parameterType,
					(short) (arrayDepth - 1), attributeDirection,
					context, wrapper);
			if (containedJavaComplexType != null) {
				containedTypeName = containedJavaComplexType.getCorrespondingSqlType().getName();
			}
		}

		sqlComplexType.setContainedTypeName(containedTypeName);
		sqlComplexType.setName(getOrCreateNewSqlTypeName(sqlBaseName, arrayDepth, wrapper));
		javaComplexType.setCorrespondingSqlType(sqlComplexType);

		wrapper.addArgumentJavaComplexType(javaComplexType);
		context.addMapEntry(key, javaComplexType);
		context.removeFromSet(key);

		return javaComplexType;
	}

	/**
	 * Builds a fresh {@link JavaComplexType} shell (for both array and non-array types).
	 */
	private JavaComplexType buildComplexTypeShell(DBJavaClass dbJavaClass, String parameterType, AttributeDirection attributeDirection, boolean isArray) {
		JavaComplexType javaComplexType = new JavaComplexType();
		javaComplexType.setAttributeDirection(attributeDirection);
		javaComplexType.setArray(isArray);
		javaComplexType.setArrayType(isArray ? JavaComplexType.ArrayType.SQUARE_BRACKET : null);
		javaComplexType.setArrayDepth((short) 0);
		javaComplexType.setTypeName(parameterType);

		if (dbJavaClass != null) {
			javaComplexType.setTypeName(dbJavaClass.getName());
		}
		return javaComplexType;
	}

	/**
	 * Attempts to return a contained (base) type name if it is a direct mapping in TypeMappingsManager.
	 * Returns {@code null} if no direct mapping is found (indicating a complex type).
	 */
	private String getContainedTypeName(String parameterType) {
		if (TypeMappingsManager.isSupportedType(parameterType)) {
			return TypeMappingsManager.getCorrespondingSqlType(parameterType).getSqlTypeName();
		}
		return null;
	}

	// -------------------------------------------------
	// ComplexTypeKey and Cycle Detection
	// -------------------------------------------------

	/**
	 * Builds a new {@link ComplexTypeKey} from the given parameters.
	 */
	private ComplexTypeKey buildComplexTypeKey(DBJavaClass dbJavaClass, String parameterType, short arrayDepth) {
		String keyName = (dbJavaClass == null)
				? convertClassNameToDotNotation(parameterType)
				: convertClassNameToDotNotation(dbJavaClass.getQualifiedName());
		return new ComplexTypeKey(keyName, arrayDepth);
	}

	/**
	 * Checks if we have already encountered this key, indicating a cycle.
	 */
	private boolean addToContextAndDetectCycle(ComplexTypeKey key, WrapperBuilderContext context) {
		if (context.detectRepetition(key)) {
			log.error("Encountered cycle for key: {}", key);
			return true;
		}
		context.addToSet(key);
		return false;
	}

	// -------------------------------------------------
	// Caching and Direction Upgrades
	// -------------------------------------------------

	/**
	 * Returns a cached {@link JavaComplexType} if present, and upgrades its direction
	 * from ARGUMENT to BOTH if needed (when also used as a RETURN).
	 */
	@Nullable
	private JavaComplexType getComplexTypeFromCache(
			ComplexTypeKey key,
			AttributeDirection direction,
			WrapperBuilderContext context) {
		JavaComplexType javaComplexType = context.getJavaComplexType(key);
        if (javaComplexType == null) return null;

        // If it was ARGUMENT-only, and now we need a RETURN, upgrade to BOTH
        if (direction == AttributeDirection.RETURN
                && javaComplexType.getAttributeDirection() == AttributeDirection.ARGUMENT) {
            changeAttributeDirection(javaComplexType, context);
        }
        return javaComplexType;
	}

	/**
	 * If a {@link JavaComplexType} was first encountered as an ARGUMENT but is also needed
	 * for RETURN, we mark it (and nested fields) as BOTH.
	 */
	private void changeAttributeDirection(
			JavaComplexType javaComplexType,
			WrapperBuilderContext context) {
		javaComplexType.setAttributeDirection(AttributeDirection.BOTH);

		if (javaComplexType.isArray()) {
			// For arrays, mark all corresponding array dimension entries + the base
			for (short i = 1; i <= javaComplexType.getArrayDepth(); i++) {
				ComplexTypeKey complexTypeKey = new ComplexTypeKey(
						javaComplexType.getTypeName(),
						javaComplexType.getArrayDepth());
				JavaComplexType mappedType = context.getJavaComplexType(complexTypeKey);
				if (mappedType != null) {
					mappedType.setAttributeDirection(AttributeDirection.BOTH);
				}
			}
			// Also update the non-array variant if it exists
			ComplexTypeKey baseKey = new ComplexTypeKey(javaComplexType.getTypeName(), (short) 0);
			JavaComplexType baseType = context.getJavaComplexType(baseKey);
			if (baseType != null) {
				changeAttributeDirection(baseType, context);
			}
		} else {
			// For complex objects, recursively mark subfields
			for (JavaComplexType.Field field : javaComplexType.getFields()) {
				if (field.isComplexType()) {
					ComplexTypeKey complexTypeKey = new ComplexTypeKey(
							field.getType(),
							field.getArrayDepth());

					JavaComplexType nested = context.getJavaComplexType(complexTypeKey);
					if (nested != null) {
						changeAttributeDirection(nested, context);
					}
				}
			}
		}
	}

	// -------------------------------------------------
	// Finalizing Complex Types
	// -------------------------------------------------

	/**
	 * Finalizes a newly constructed {@link JavaComplexType}, storing it in the cache
	 * and linking its corresponding SQL type.
	 */
	private void finalizeComplexType(
			ComplexTypeKey key,
			JavaComplexType javaComplexType,
			SqlComplexType sqlComplexType,
			WrapperBuilderContext context,
			Wrapper wrapper) {
		String sqlTypeName = getOrCreateNewSqlTypeName(
				javaComplexType.getTypeName(),
				javaComplexType.getArrayDepth(), wrapper);
		sqlComplexType.setName(sqlTypeName);
		javaComplexType.setCorrespondingSqlType(sqlComplexType);
		wrapper.addArgumentJavaComplexType(javaComplexType);

		context.addMapEntry(key, javaComplexType);
		context.removeFromSet(key);
	}

	// -------------------------------------------------
	// Field Population
	// -------------------------------------------------

	/**
	 * Populates the fields of a {@link JavaComplexType} given a {@link DBJavaClass},
	 * building nested types if necessary.
	 */
	private void populateComplexTypeFields(
			DBJavaClass dbJavaClass,
			AttributeDirection attributeDirection,
			JavaComplexType javaComplexType,
			SqlComplexType sqlComplexType,
			WrapperBuilderContext context,
			Wrapper wrapper) {
		List<DBJavaField> javaFields = dbJavaClass.getFields();
		for (DBJavaField javaField : javaFields) {
			JavaComplexType.Field field = buildJavaComplexField(javaField, dbJavaClass, wrapper);
			javaComplexType.addField(field);

			// If it's a primitive or directly supported type, add to the SQL type
			SqlType sqlType = TypeMappingsManager.getCorrespondingSqlType(field.getType());
			if (sqlType != null && javaField.getArrayDepth() <= 0) {
				sqlComplexType.addField(field.getName(), sqlType.getSqlTypeName(), field.getFieldIndex());
			} else {
				// It's a nested complex field
				handleNestedField(field, javaField, attributeDirection, sqlComplexType,
						context, wrapper);
			}
		}
	}

	/**
	 * Builds a single {@link JavaComplexType.Field} instance from a {@link DBJavaField}.
	 */
	private JavaComplexType.Field buildJavaComplexField(DBJavaField dbJavaField, DBJavaClass dbJavaClass, Wrapper wrapper) {
		JavaComplexType.Field field = new JavaComplexType.Field();

		// Get the raw field type in string form
		String fieldParameter;
		try {
			fieldParameter = getParameterType(dbJavaField.getType(), dbJavaField.getClassName());
		} catch (Exception e) {
			log.error("Could not create JavaComplexType for field: {}", dbJavaField, e);
			conditionallyLog(e);
			fieldParameter = dbJavaField.getType(); // fallback
		}

		if (TypeMappingsManager.isUnSupportedType(fieldParameter)) {
			log.error("Encountered unsupported type for field {}: {}", dbJavaField, fieldParameter);
		}

		// Basic field setup
		field.setFieldIndex(dbJavaField.getIndex());
		field.setName(dbJavaField.getName());
		field.setAccessModifier(dbJavaField.getAccessibility().toString());
		field.setType(fieldParameter, TypeMappingsManager.getCorrespondingSqlType(fieldParameter));

		// If array
		if (dbJavaField.getArrayDepth() > 0) {
			field.setArray(true);
			field.setArrayDepth(dbJavaField.getArrayDepth());
		}

		// If the field is non-public, set up the getter/setter if present
		if (!"PUBLIC".equals(field.getAccessModifier().toString())) {
			field.setGetter(getGetter(field.getName(), fieldParameter, field.getArrayDepth(), dbJavaClass));
			field.setSetter(getSetter(field.getName(), fieldParameter, field.getArrayDepth(), dbJavaClass));
		}

		// If the underlying Java class is known
		if (field.getSqlType().isEmpty()) {
			// Re-use the same complexTypeConversion map.
			field.setSqlType(getOrCreateNewSqlTypeName(fieldParameter, field.getArrayDepth(), wrapper));
		}

		return field;
	}

	/**
	 * Handles a nested field that either references a nested array type or a nested complex type.
	 */
	private void handleNestedField(
			JavaComplexType.Field field,
			DBJavaField dbJavaField,
			AttributeDirection attributeDirection,
			SqlComplexType sqlComplexType,
			WrapperBuilderContext context,
			Wrapper wrapper) {
		field.setComplexType(true);
		JavaComplexType fieldJavaComplexType;
		if (dbJavaField.getArrayDepth() > 0) {
			// Nested array
			fieldJavaComplexType = createJavaComplexArrayType(
					dbJavaField.getFieldClass(),
					field.getType(),
					dbJavaField.getArrayDepth(),
					attributeDirection,
					context,
					wrapper);
		} else {
			// Nested object
			fieldJavaComplexType = createJavaComplexType(
					dbJavaField.getFieldClass(),
					field.getType(),
					attributeDirection,
					context,
					wrapper);
		}
		if (fieldJavaComplexType != null) {
			sqlComplexType.addField(
					field.getName(),
					fieldJavaComplexType.getCorrespondingSqlType().getName(),
					field.getFieldIndex());
			field.setSqlType(sqlComplexType.getName());
		}
	}

	// -------------------------------------------------
	// Getters / Setters
	// -------------------------------------------------

	/**
	 * Finds a setter method matching the given field signature, if one exists.
	 * TODO move logic to DBJavaClass (getFieldSetter)
	 */
	@Nullable
	private String getSetter(
			String fieldName,
			String fieldParameter,
			short arrayDepth,
			DBJavaClass javaClass) {
		if (javaClass == null) return null;

		String setterName = "set" + capitalize(fieldName);
		List<DBJavaMethod> methods = javaClass.getMethods();
		DBJavaMethod setterMethod = null;
		for (DBJavaMethod method : methods) {
			if (setterName.equals(method.getName().split("#")[0])) {
				setterMethod = method;
				break;
			}
		}
        if (setterMethod == null) return null;

        List<DBJavaParameter> methodParameters = setterMethod.getParameters();
        if (methodParameters.size() != 1) return null;

		DBJavaParameter param = methodParameters.get(0);
		String targetFieldClass;
		DBJavaField javaField = javaClass.getField(fieldName);
		if (javaField != null && javaField.getFieldClass() != null) {
			String qualifiedName = javaField.getFieldClass().getQualifiedName();
			targetFieldClass = convertClassNameToDotNotation(qualifiedName);
		} else {
			targetFieldClass = fieldParameter;
		}

		if (getParameterType(param).equals(targetFieldClass)
				&& param.getArrayDepth() == arrayDepth) {
			return setterName;
		}

        return null;
	}

	/**
	 * Finds a getter method matching the given field signature, if one exists.
	 * // TODO move logic to DBJavaClass (getFieldGetter)
	 */
	@Nullable
	private String getGetter(
			String fieldName,
			String fieldParameter,
			short arrayDepth,
			DBJavaClass javaClass) {
		if (javaClass == null) return null;

		String getterName = "get" + capitalize(fieldName);
		List<DBJavaMethod> methods = javaClass.getMethods();
		DBJavaMethod getterMethod = null;
		for (DBJavaMethod method : methods) {
			if (getterName.equals(method.getName().split("#")[0])) {
				getterMethod = method;
				break;
			}
		}

        if (getterMethod == null) return null;

		String methodReturn = getParameterType(getterMethod.getReturnType(), getterMethod.getClassName());
		if (methodReturn.equals(fieldParameter)
				&& getterMethod.getArrayDepth() == arrayDepth
				&& getterMethod.getParameters().isEmpty()) {
			return getterName;
		}
        return null;
	}

	// -------------------------------------------------
	// Type Utilities
	// -------------------------------------------------

	/**
	 * Retrieves the parameter type from a {@link DBJavaParameter}.
	 */
	private String getParameterType(DBJavaParameter javaParameter) {
		String parameterType = convertClassNameToDotNotation(javaParameter.getParameterType());

		// If parameter type is empty, try to get it from the parameter class
		if (parameterType.isEmpty() || parameterType.equals("-") || parameterType.equals("class")) {
			DBJavaClass parameterClass = javaParameter.getParameterClass();
			if (parameterClass != null) {
				parameterType = convertClassNameToDotNotation(parameterClass.getQualifiedName());
			}
		}
		validateParameterType(parameterType);
		return parameterType;
	}

	/**
	 * Retrieves the parameter type by analyzing a raw type and class name, if needed.
	 */
	private String getParameterType(String type, String className) {
		String parameterType = convertClassNameToDotNotation(type);

		if (parameterType.isEmpty() || parameterType.equals("-") || parameterType.equals("class")) {
			if (className != null) {
				parameterType = convertClassNameToDotNotation(className);
			}
		}
		validateParameterType(parameterType);
		return parameterType;
	}

	/**
	 * Builds a new SQL type name, prepending a constant prefix plus an incrementing integer.
	 */
	private String getOrCreateNewSqlTypeName(String className, short arrayDepth, Wrapper wrapper) {
		// The SQL type prefix used to create new type names
		ComplexTypeKey key = new ComplexTypeKey(className, arrayDepth);
		if (!wrapper.getComplexTypeConversion().containsKey(key)) {
			// Assign a new index based on current map size (or use another scheme if needed)
			wrapper.addEntryToComplexTypeConversion(key,
					wrapper.getComplexTypeConversion().size() + 1);
		}
		return newSqlTypePrepend + wrapper.getComplexTypeConversion().get(key);
	}

	/**
	 * Ensures the parameter type is neither {@code null} nor empty. Logs an error if it is.
	 */
	private void validateParameterType(String parameterType) {
		if (parameterType == null || parameterType.isEmpty()) {
			log.error("Parameter type is empty or null.");
		}
	}

	/**
	 * Converts slash notation to dot notation (e.g., "java/lang/String" -> "java.lang.String").
	 */
	public static String convertClassNameToDotNotation(String className) {
		return className.replace('/', '.');
	}

	/**
	 * Capitalizes the first character of a string (e.g. "field" -> "Field").
	 */
	private String capitalize(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	// -------------------------------------------------
	// ComplexTypeKey
	// -------------------------------------------------

	/**
	 * A composite key of {@code className + arrayLength} used to ensure uniqueness
	 * when creating/looking up complex types. Prevents collisions for arrays
	 * of the same class at different depths.
	 */
	@Value // Required for proper usage in Maps/Sets
	public static class ComplexTypeKey {
		private final String className;
		private final short arrayLength;

		public ComplexTypeKey(String className, short arrayLength) {
			this.className = className;
			this.arrayLength = arrayLength;
		}
	}
}
