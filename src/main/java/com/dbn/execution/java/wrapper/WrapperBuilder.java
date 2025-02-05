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

import com.dbn.common.util.Strings;
import com.dbn.execution.java.wrapper.JavaComplexType.AttributeDirection;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.DBOrderedObject;
import com.dbn.object.lookup.DBObjectRef;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

import static com.dbn.common.util.Java.isVoid;
import static com.dbn.execution.java.wrapper.TypeMappings.getSqlType;
import static com.dbn.execution.java.wrapper.TypeMappings.isSupportedType;
import static com.dbn.execution.java.wrapper.TypeMappings.isUnsupportedType;
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;
import static com.dbn.object.type.DBJavaValueType.isPseudoPrimitive;

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

	public static final String DBN_TYPE_SUFFIX = "DBN_OJVM_TYPE_";

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
		wrapper.setFullyQualifiedClassName(getCanonicalName(javaMethod.getOwnerClassName()));
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
			Wrapper.MethodAttribute attr = createMethodAttribute(
					parameter.getJavaClassRef(),
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
		DBObjectRef<DBJavaClass> returnClass = javaMethod.getReturnClassRef();
		String returnClassName = getCanonicalName(returnClass.getObjectName());
        if (isVoid(returnClassName)) return;

        Wrapper.MethodAttribute returnAttr = createMethodAttribute(
                returnClass,
                javaMethod.getReturnArrayDepth(),
                AttributeDirection.RETURN,
                context,
                wrapper);
        wrapper.setReturnType(returnAttr);
    }

	/**
	 * Creates a {@link Wrapper.MethodAttribute} for the given DB elements, either
	 * a simple attribute if primitive/supported, or a complex type otherwise.
	 */
	private Wrapper.MethodAttribute createMethodAttribute(
			DBObjectRef<DBJavaClass> javaClass,
			short arrayDepth,
			AttributeDirection attributeDirection,
			WrapperBuilderContext context,
			Wrapper wrapper) {

		// If non-array and we have a direct mapping -> simple attribute
		String className = getCanonicalName(javaClass.getObjectName());
		if (arrayDepth == 0 && isSupportedType(className)) {
			return buildSimpleMethodAttribute(className);
		}

		// Otherwise, build or retrieve a JavaComplexType
		JavaComplexType javaComplexType = (arrayDepth > 0) ?
				createJavaComplexArrayType(javaClass, arrayDepth, attributeDirection, context, wrapper) :
				createJavaComplexType(javaClass, attributeDirection, context, wrapper);

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
	private Wrapper.MethodAttribute buildSimpleMethodAttribute(String javaClassName) {
		Wrapper.MethodAttribute methodAttribute = new Wrapper.MethodAttribute();
		methodAttribute.setJavaTypeName(javaClassName);

		String sqlTypeName = TypeMappings.getSqlTypeName(javaClassName);
		methodAttribute.setSqlTypeName(sqlTypeName);
		methodAttribute.setComplexType(false);
		return methodAttribute;
	}

	/**
	 * Builds a method attribute that is backed by a {@link JavaComplexType}.
	 */
	private Wrapper.MethodAttribute buildComplexMethodAttribute(JavaComplexType javaComplexType) {
		Wrapper.MethodAttribute methodAttribute = new Wrapper.MethodAttribute();
		methodAttribute.setArrayDepth(javaComplexType.getArrayDepth());
		methodAttribute.setJavaTypeName(javaComplexType.getJavaClassName());
		methodAttribute.setComplexType(true);

		SqlComplexType sqlType = javaComplexType.getCorrespondingSqlType();
		methodAttribute.setSqlTypeName((sqlType == null) ? "" : sqlType.getName());

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
			DBObjectRef<DBJavaClass> javaClass,
			AttributeDirection attributeDirection,
			WrapperBuilderContext context,
			Wrapper wrapper) {
		String javaClassName = getCanonicalName(javaClass);

		ComplexTypeKey key = new ComplexTypeKey(javaClassName, (short) 0);
		if (addToContextAndDetectCycle(key, context)) return null;

		JavaComplexType existing = getComplexTypeFromCache(key, attributeDirection, context);
		if (existing != null) {
			context.removeFromSet(key);
			return existing;
		}

		// Create a new complex type shell
		JavaComplexType javaComplexType = buildComplexTypeShell(javaClassName, attributeDirection, (short) 0);
		SqlComplexType sqlComplexType = new SqlComplexType();
		sqlComplexType.setArray(false);


		// Populate fields if we have a DBJavaClass
		boolean complexType = !isPseudoPrimitive(javaClassName);
		if (complexType) {
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
			DBObjectRef<DBJavaClass> javaClass,
			short arrayDepth,
			AttributeDirection attributeDirection,
			WrapperBuilderContext context,
			Wrapper wrapper) {
		String javaClassName = getCanonicalName(javaClass);

		ComplexTypeKey key = new ComplexTypeKey(javaClassName, arrayDepth);
		if (addToContextAndDetectCycle(key, context)) return null;

		JavaComplexType existing = getComplexTypeFromCache(key, attributeDirection, context);
		if (existing != null) {
			context.removeFromSet(key);
			return existing;
		}

		// Create new array-type shell
		JavaComplexType javaComplexType = buildComplexTypeShell(javaClassName, attributeDirection, arrayDepth);

		SqlComplexType sqlComplexType = new SqlComplexType();
		sqlComplexType.setArray(true);

		// If base type is unsupported, abort
		if (TypeMappings.isUnsupportedType(javaClassName)) {
			log.error("Encountered unsupported type for array: {}", javaClassName);
			context.removeFromSet(key);
			return null;
		}

		String sqlTypeName = null;
		JavaComplexType containedJavaComplexType;

		// Single-dimension vs multi-dimension array
		if (arrayDepth <= 1) {
			sqlTypeName = TypeMappings.getSqlTypeName(javaClassName);
			if (sqlTypeName == null) {
				// Possibly a nested complex type
				containedJavaComplexType = createJavaComplexType(javaClass, attributeDirection, context, wrapper);
				if (containedJavaComplexType != null) {
					sqlTypeName = containedJavaComplexType.getCorrespondingSqlType().getName();
				}
			}
		} else {
			// Multi-dimensional
			containedJavaComplexType = createJavaComplexArrayType(javaClass,
					(short) (arrayDepth - 1), attributeDirection,
					context, wrapper);
			if (containedJavaComplexType != null) {
				sqlTypeName = containedJavaComplexType.getCorrespondingSqlType().getName();
			}
		}

		sqlComplexType.setContainedTypeName(sqlTypeName);
		sqlComplexType.setName(getSqlTypeName(javaClassName, arrayDepth, wrapper));
		javaComplexType.setCorrespondingSqlType(sqlComplexType);

		wrapper.addArgumentJavaComplexType(javaComplexType);
		context.addMapEntry(key, javaComplexType);
		context.removeFromSet(key);

		return javaComplexType;
	}

	/**
	 * Builds a fresh {@link JavaComplexType} shell (for both array and non-array types).
	 */
	private JavaComplexType buildComplexTypeShell(String javaClassName, AttributeDirection attributeDirection, short arrayDepth) {
		JavaComplexType complexType = new JavaComplexType();
		complexType.setAttributeDirection(attributeDirection);
		complexType.setArrayType(arrayDepth == 0 ? null : JavaComplexType.ArrayType.SQUARE_BRACKET);
		complexType.setArrayDepth(arrayDepth);
		complexType.setJavaClassName(javaClassName);
		return complexType;
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
						javaComplexType.getJavaClassName(),
						javaComplexType.getArrayDepth());
				JavaComplexType mappedType = context.getJavaComplexType(complexTypeKey);
				if (mappedType != null) {
					mappedType.setAttributeDirection(AttributeDirection.BOTH);
				}
			}
			// Also update the non-array variant if it exists
			ComplexTypeKey baseKey = new ComplexTypeKey(javaComplexType.getJavaClassName(), (short) 0);
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
		String sqlTypeName = getSqlTypeName(
				javaComplexType.getJavaClassName(),
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
			DBObjectRef<DBJavaClass> javaClass,
			AttributeDirection attributeDirection,
			JavaComplexType javaComplexType,
			SqlComplexType sqlComplexType,
			WrapperBuilderContext context,
			Wrapper wrapper) {
		List<DBJavaField> javaFields = javaClass.get().getFields();
		for (DBJavaField javaField : javaFields) {
			JavaComplexType.Field field = buildJavaComplexField(javaField, javaClass, wrapper);
			javaComplexType.addField(field);

			// If it's a primitive or directly supported type, add to the SQL type
			SqlType sqlType = getSqlType(field.getType());
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
	private JavaComplexType.Field buildJavaComplexField(DBJavaField javaField, DBObjectRef<DBJavaClass> parentJavaClass, Wrapper wrapper) {
		JavaComplexType.Field field = new JavaComplexType.Field();

		// Get the raw field type in string form
		String fieldJavaClassName = getCanonicalName(javaField.getJavaClassName());

		if (isUnsupportedType(fieldJavaClassName)) {
			log.error("Encountered unsupported type for field {}: {}", javaField, fieldJavaClassName);
		}

		// Basic field setup
		field.setFieldIndex(javaField.getIndex());
		field.setName(javaField.getName());
		if(javaField.getAccessibility() != null)
			field.setAccessModifier(javaField.getAccessibility().toString());
		field.setType(fieldJavaClassName, getSqlType(fieldJavaClassName));

		// If array
		short arrayDepth = javaField.getArrayDepth();
		if (arrayDepth > 0) {
			field.setArrayDepth(arrayDepth);
		}

		// If the field is non-public, set up the getter/setter if present
		if (field.getAccessModifier() != JavaComplexType.Field.AccessModifier.PUBLIC) {
			DBJavaMethod getter = javaField.findGetterMethod();
			DBJavaMethod setter = javaField.findSetterMethod();
			field.setGetter(getter == null ? null : getter.getSimpleName());
			field.setSetter(setter == null ? null : setter.getSimpleName());
		}

		// If the underlying Java class is known
		if (Strings.isEmpty(field.getSqlType())) {
			// Re-use the same complexTypeConversion map.
			field.setSqlType(getSqlTypeName(fieldJavaClassName, field.getArrayDepth(), wrapper));
		}

		return field;
	}

	/**
	 * Handles a nested field that either references a nested array type or a nested complex type.
	 */
	private void handleNestedField(
			JavaComplexType.Field field,
			DBJavaField javaField,
			AttributeDirection attributeDirection,
			SqlComplexType sqlComplexType,
			WrapperBuilderContext context,
			Wrapper wrapper) {
		field.setComplexType(true);
		JavaComplexType fieldJavaComplexType;
		if (javaField.getArrayDepth() > 0) {
			// Nested array
			fieldJavaComplexType = createJavaComplexArrayType(
					javaField.getJavaClassRef(),
					javaField.getArrayDepth(),
					attributeDirection,
					context,
					wrapper);
		} else {
			// Nested object
			fieldJavaComplexType = createJavaComplexType(
					javaField.getJavaClassRef(),
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
	// Type Utilities
	// -------------------------------------------------

	/**
	 * Builds a new SQL type name, prepending a constant prefix plus an incrementing integer.
	 */
	private String getSqlTypeName(String className, short arrayDepth, Wrapper wrapper) {
		return DBN_TYPE_SUFFIX + wrapper.getSqlTypeIndex(className, arrayDepth);
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

	public static String getCanonicalPath(DBJavaClass javaClass) {
		// avoid accessing java class-name cache with fully qualified java path

		// TODO verify if execution on foreign schema is allowed
		//return javaClass.getSchemaName() + "." + getCanonicalName(javaClass.getName());
		return getCanonicalName(javaClass.getName());
	}
}
