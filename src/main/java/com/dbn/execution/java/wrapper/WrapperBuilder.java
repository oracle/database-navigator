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

import com.dbn.common.util.Naming;
import com.dbn.common.util.Strings;
import com.dbn.execution.java.wrapper.model.ClassWrapper;
import com.dbn.execution.java.wrapper.model.ClassWrapper.AttributeDirection;
import com.dbn.execution.java.wrapper.model.FieldWrapper;
import com.dbn.execution.java.wrapper.model.MethodWrapper;
import com.dbn.execution.java.wrapper.model.ParameterWrapper;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.lookup.DBObjectRef;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Lists.sortedCopy;
import static com.dbn.execution.java.wrapper.TypeMappings.getSqlType;
import static com.dbn.execution.java.wrapper.TypeMappings.isSupportedType;
import static com.dbn.execution.java.wrapper.TypeMappings.isUnsupportedType;
import static com.dbn.object.DBOrderedObject.POSITION_COMPARATOR;
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;
import static com.dbn.object.type.DBJavaScalarType.isScalar;

/**
 * Parses {@link DBJavaMethod} instances into {@link Wrapper} objects,
 * including generating the corresponding {@link ClassWrapper}
 * and associated {@link SqlComplexType}.
 *
 * <p>This implementation uses a Singleton pattern and keeps
 * all per-parse mutable state in local variables inside
 *  so that it is thread-safe
 * if multiple threads call it simultaneously.</p>
 */
@Slf4j
public final class WrapperBuilder {

	/**
	 * The single, static instance of WrapperBuilder.
	 */
	private static final WrapperBuilder INSTANCE = new WrapperBuilder();

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
	 * Entry point for parsing a {@link List<DBJavaMethod>} into a {@link Wrapper}.
	 *
	 * @param javaMethods The method definition to parse.
	 * @param useFriendlyNames Boolean
	 * @return The fully-populated {@link Wrapper}.
	 */
	public Wrapper build(List<DBJavaMethod> javaMethods,boolean useFriendlyNames) {
		// Create data structures that are unique to *this* parse call.
		WrapperBuilderContext context = new WrapperBuilderContext();

		// Delegate to the internal parsing method.
		return buildInternal(javaMethods, context, useFriendlyNames);
	}

	public Wrapper build(DBJavaMethod javaMethod,boolean useFriendlyNames) {
		ArrayList<DBJavaMethod> dbJavaMethodArrayList = new ArrayList<>();
		dbJavaMethodArrayList.add(javaMethod);

		return build(dbJavaMethodArrayList, useFriendlyNames);
	}

	// -------------------------------------------------
	// Internal Parsing Logic
	// -------------------------------------------------

	/**
	 * Internal method that actually performs the parsing to produce a {@link Wrapper}.
	 *
	 * @param javaMethods         The method definition to parse.
	 * @param context contains the following
	 *  complexTypeConversion Mapping from className -> unique integer (for type naming).
	 *  complexTypeMap        Cache of complex types created so far during this parse.
	 *  complexTypeSet        Used to detect cycles during recursive type creation.
	 * @return The generated {@link Wrapper}.
	 */
	private Wrapper buildInternal(
			List<DBJavaMethod> javaMethods,
			WrapperBuilderContext context, boolean useFriendlyNames) {
		if(javaMethods.isEmpty())
			return null;
		// Create a fresh Wrapper for this invocation
		Wrapper wrapper = new Wrapper();
		wrapper.setUseFriendlyNames(useFriendlyNames);
		DBObjectRef<DBJavaClass> ownerClass = javaMethods.get(0).getOwnerClassRef();
		wrapper.setClassName(getCanonicalName(ownerClass));


        for (DBJavaMethod javaMethod : javaMethods) {
			MethodWrapper methodWrapper = new MethodWrapper();
            setMethodMetadata(javaMethod, methodWrapper);
            parseParameters(javaMethod, methodWrapper, wrapper, context);
            initMethodReturnParameter(javaMethod, methodWrapper, wrapper, context);
			String javaMethodName = context.getAndAddWrapperMethodName(
					methodWrapper.getOriginalJavaMethodName(),
					methodWrapper.getJavaSignature(false));

			String sqlMethodName = Naming.toUpperSnakeCase(javaMethodName);

			methodWrapper.setJavaMethodName(javaMethodName);
			methodWrapper.setSqlMethodName(sqlMethodName);
			wrapper.addJavaMethod(methodWrapper);
        }

		return wrapper;
	}

	/**
	 * Sets up the basic method metadata on the {@link Wrapper} object.
	 */
	private void setMethodMetadata(DBJavaMethod javaMethod, MethodWrapper methodWrapper) {
		String methodName = javaMethod.getName().split("#")[0];
		methodWrapper.setOriginalJavaMethodName(methodName);

		// Replace "void" return in the signature with a more readable style, if present.
		String javaMethodSignature = javaMethod.getSignature().replace(": void", "").replace(":", " return");
		methodWrapper.setJavaMethodSignature(javaMethodSignature);
	}

	/**
	 * Parse all method parameters from the given {@link DBJavaMethod} and populate the wrapper.
	 */
	private void parseParameters(
			DBJavaMethod javaMethod,
			MethodWrapper methodWrapper,
			Wrapper wrapper,
			WrapperBuilderContext context) {
		List<DBJavaParameter> parameters = javaMethod.getParameters();
		if (parameters == null || parameters.isEmpty()) {
			return;
		}

		// Sort by position to ensure correct order
		parameters = sortedCopy(parameters, POSITION_COMPARATOR);

		// Create a Wrapper.MethodAttribute for each parameter
		for (DBJavaParameter parameter : parameters) {
			ParameterWrapper parameterWrapper = createParameterWrapper(
					parameter.getJavaClassRef(),
					parameter.getArrayDepth(),
					AttributeDirection.ARGUMENT,
					context,
					wrapper);
			methodWrapper.addParameter(parameterWrapper);
		}
	}

	/**
	 * Parse the return type (if not void) and populate the wrapper.
	 */
	private void initMethodReturnParameter(
			DBJavaMethod javaMethod,
			MethodWrapper methodWrapper,
			Wrapper wrapper,
			WrapperBuilderContext context) {
        if (javaMethod.isReturningVoid()) return;

		DBObjectRef<DBJavaClass> returnClass = javaMethod.getReturnClassRef();
		ParameterWrapper parameterWrapper = createParameterWrapper(
                returnClass,
                javaMethod.getReturnArrayDepth(),
                AttributeDirection.RETURN,
                context,
                wrapper);
		methodWrapper.setReturnParameter(parameterWrapper);
    }

	/**
	 * Creates a {@link ParameterWrapper} for the given DB elements, either
	 * a simple attribute if primitive/supported, or a complex type otherwise.
	 */
	private ParameterWrapper createParameterWrapper(
			DBObjectRef<DBJavaClass> javaClass,
			short arrayDepth,
			AttributeDirection attributeDirection,
			WrapperBuilderContext context,
			Wrapper wrapper) {

		// If non-array and we have a direct mapping -> simple attribute
		String className = getCanonicalName(javaClass);
		if (arrayDepth == 0 && isSupportedType(className)) {
			return createSimpleParameterWrapper(className);
		}

		// Otherwise, build or retrieve a JavaComplexType
		ClassWrapper classWrapper = (arrayDepth > 0) ?
				createArrayClassWrapper(javaClass, arrayDepth, attributeDirection, context, wrapper) :
				createClassWrapper(javaClass, attributeDirection, context, wrapper);

		if (classWrapper == null) {
			// If still null, it's unsupported or cyclical
			return null;
		}

		// Build a complex attribute
		return createComplexParameterWrapper(classWrapper, attributeDirection);
	}

	/**
	 * Builds a simple (non-complex) method attribute with a known SQL type mapping.
	 */
	private ParameterWrapper createSimpleParameterWrapper(String javaClassName) {
		ParameterWrapper methodAttribute = new ParameterWrapper();
		methodAttribute.setJavaTypeName(javaClassName);

		String sqlTypeName = TypeMappings.getSqlTypeName(javaClassName);
		methodAttribute.setSqlTypeName(sqlTypeName);
		methodAttribute.setComplexType(false);
		return methodAttribute;
	}

	/**
	 * Builds a method attribute that is backed by a {@link ClassWrapper}.
	 */
	private ParameterWrapper createComplexParameterWrapper(ClassWrapper classWrapper, AttributeDirection attributeDirection) {
		ParameterWrapper methodAttribute = new ParameterWrapper();
		methodAttribute.setArrayDepth(classWrapper.getArrayDepth());
		methodAttribute.setJavaTypeName(classWrapper.getJavaClassName());
		methodAttribute.setComplexType(true);

		SqlComplexType sqlType = classWrapper.getCorrespondingSqlType();
		methodAttribute.setSqlTypeName((sqlType == null) ? "" : sqlType.getName());
		if(attributeDirection.equals(AttributeDirection.ARGUMENT))
			methodAttribute.setConverterName(classWrapper.getSqlToJavaConverterName());
		else
			methodAttribute.setConverterName(classWrapper.getJavaToSqlConverterName());


		return methodAttribute;
	}

	// -------------------------------------------------
	// Complex Type Creation Logic
	// -------------------------------------------------

	/**
	 * Creates a {@link ClassWrapper} (non-array version) for the given class
	 * or parameter type, populating its fields recursively if needed.
	 */
	private ClassWrapper createClassWrapper(
			DBObjectRef<DBJavaClass> javaClass,
			AttributeDirection attributeDirection,
			WrapperBuilderContext context,
			Wrapper wrapper) {
		String javaClassName = getCanonicalName(javaClass);

		ComplexTypeKey key = new ComplexTypeKey(javaClassName, (short) 0);
		if (addToContextAndDetectCycle(key, context)) return null;

		ClassWrapper existing = getCachedClassWrapper(key, attributeDirection, context);
		if (existing != null) {
			context.removeFromSet(key);
			return existing;
		}

		// Create a new complex type shell
		ClassWrapper classWrapper = createClassWrapperShell(javaClassName, attributeDirection, (short) 0);
		SqlComplexType sqlComplexType = new SqlComplexType();
		sqlComplexType.setArray(false);
		sqlComplexType.setName(wrapper.getSqlTypeName(javaClassName, (short) 0));

		// Populate fields if we have a DBJavaClass
		boolean complexType = !isScalar(javaClassName);
		if (complexType) {
			populateClassWrapperFields(
					javaClass,
					attributeDirection,
					classWrapper,
					sqlComplexType,
					context,
					wrapper);
		}

		// Finalize and return
		finalizeComplexType(key, classWrapper, sqlComplexType, context, wrapper);
		return classWrapper;
	}

	/**
	 * Creates a {@link ClassWrapper} for array types (including nested arrays),
	 * populating its contained type recursively if necessary.
	 */
	private ClassWrapper createArrayClassWrapper(
			DBObjectRef<DBJavaClass> javaClass,
			short arrayDepth,
			AttributeDirection attributeDirection,
			WrapperBuilderContext context,
			Wrapper wrapper) {
		String javaClassName = getCanonicalName(javaClass);

		ComplexTypeKey key = new ComplexTypeKey(javaClassName, arrayDepth);
		if (addToContextAndDetectCycle(key, context)) return null;

		ClassWrapper existing = getCachedClassWrapper(key, attributeDirection, context);
		if (existing != null) {
			context.removeFromSet(key);
			return existing;
		}

		// Create new array-type shell
		ClassWrapper classWrapper = createClassWrapperShell(javaClassName, attributeDirection, arrayDepth);

		SqlComplexType sqlComplexType = new SqlComplexType();
		sqlComplexType.setArray(true);

		// If base type is unsupported, abort
		if (TypeMappings.isUnsupportedType(javaClassName)) {
			log.error("Encountered unsupported type for array: {}", javaClassName);
			context.removeFromSet(key);
			return null;
		}

		String sqlTypeName = null;
		ClassWrapper containedClassWrapper;

		// Single-dimension vs multi-dimension array
		if (arrayDepth <= 1) {
			sqlTypeName = TypeMappings.getSqlTypeName(javaClassName);
			if (sqlTypeName == null) {
				// Possibly a nested complex type
				containedClassWrapper = createClassWrapper(javaClass, attributeDirection, context, wrapper);
				if (containedClassWrapper != null) {
					sqlTypeName = containedClassWrapper.getCorrespondingSqlType().getName();
					int containedTypeIndex = context.getComplexTypeIndex(containedClassWrapper.getJavaClassName(),
							containedClassWrapper.getArrayDepth());
					classWrapper.setContainedJavaComplexTypeIndex(containedTypeIndex);
				}
			}
		} else {
			// Multi-dimensional
			containedClassWrapper = createArrayClassWrapper(javaClass,
					(short) (arrayDepth - 1), attributeDirection,
					context, wrapper);
			if (containedClassWrapper != null) {
				sqlTypeName = containedClassWrapper.getCorrespondingSqlType().getName();
				int containedTypeIndex = context.getComplexTypeIndex(containedClassWrapper.getJavaClassName(),
						containedClassWrapper.getArrayDepth());
				classWrapper.setContainedJavaComplexTypeIndex(containedTypeIndex);
			}
		}

		sqlComplexType.setContainedTypeName(sqlTypeName);
		sqlComplexType.setName(wrapper.getSqlTypeName(javaClassName, arrayDepth));
		classWrapper.setCorrespondingSqlType(sqlComplexType);


		wrapper.addJavaComplexType(classWrapper);

		context.addToIndex(key, wrapper.getNumberOfJavaComplexTypes()-1);
		context.addClassWrapper(key, classWrapper);
		context.removeFromSet(key);

		return classWrapper;
	}

	/**
	 * Builds a fresh {@link ClassWrapper} shell (for both array and non-array types).
	 */
	private ClassWrapper createClassWrapperShell(String javaClassName, AttributeDirection attributeDirection, short arrayDepth) {
		ClassWrapper classWrapper = new ClassWrapper();
		classWrapper.setAttributeDirection(attributeDirection);
		classWrapper.setArrayDepth(arrayDepth);
		classWrapper.setJavaClassName(javaClassName);
		return classWrapper;
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
	 * Returns a cached {@link ClassWrapper} if present, and upgrades its direction
	 * from ARGUMENT to BOTH if needed (when also used as a RETURN).
	 */
	@Nullable
	private ClassWrapper getCachedClassWrapper(
			ComplexTypeKey key,
			AttributeDirection direction,
			WrapperBuilderContext context) {
		ClassWrapper classWrapper = context.getClassWrapper(key);
        if (classWrapper == null) return null;

        // If it was ARGUMENT-only, and now we need a RETURN, upgrade to BOTH
        if (direction == AttributeDirection.RETURN
                && classWrapper.getAttributeDirection() == AttributeDirection.ARGUMENT) {
            changeAttributeDirection(classWrapper, context);
        }
        return classWrapper;
	}

	/**
	 * If a {@link ClassWrapper} was first encountered as an ARGUMENT but is also needed
	 * for RETURN, we mark it (and nested fields) as BOTH.
	 */
	private void changeAttributeDirection(
			ClassWrapper classWrapper,
			WrapperBuilderContext context) {
		classWrapper.setAttributeDirection(AttributeDirection.BOTH);

		if (classWrapper.isArray()) {
			// For arrays, mark all corresponding array dimension entries + the base
			for (short i = 1; i <= classWrapper.getArrayDepth(); i++) {
				ComplexTypeKey complexTypeKey = new ComplexTypeKey(
						classWrapper.getJavaClassName(), i);
				ClassWrapper mappedType = context.getClassWrapper(complexTypeKey);
				if (mappedType != null) {
					mappedType.setAttributeDirection(AttributeDirection.BOTH);
				}
			}
			// Also update the non-array variant if it exists
			ComplexTypeKey baseKey = new ComplexTypeKey(classWrapper.getJavaClassName(), (short) 0);
			ClassWrapper baseType = context.getClassWrapper(baseKey);
			if (baseType != null) {
				changeAttributeDirection(baseType, context);
			}
		} else {
			// For complex objects, recursively mark subfields
			for (FieldWrapper fieldWrapper : classWrapper.getFields()) {
				if (fieldWrapper.isComplexType()) {
					ComplexTypeKey complexTypeKey = new ComplexTypeKey(
							fieldWrapper.getType(),
							fieldWrapper.getArrayDepth());

					ClassWrapper nested = context.getClassWrapper(complexTypeKey);
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
	 * Finalizes a newly constructed {@link ClassWrapper}, storing it in the cache
	 * and linking its corresponding SQL type.
	 */
	private void finalizeComplexType(
			ComplexTypeKey key,
			ClassWrapper classWrapper,
			SqlComplexType sqlComplexType,
			WrapperBuilderContext context,
			Wrapper wrapper) {
		String sqlTypeName = wrapper.getSqlTypeName(
				classWrapper.getJavaClassName(),
				classWrapper.getArrayDepth());
		sqlComplexType.setName(sqlTypeName);
		classWrapper.setCorrespondingSqlType(sqlComplexType);
		wrapper.addJavaComplexType(classWrapper);

		context.addToIndex(key, wrapper.getNumberOfJavaComplexTypes()-1);
		context.addClassWrapper(key, classWrapper);
		context.removeFromSet(key);
	}

	// -------------------------------------------------
	// Field Population
	// -------------------------------------------------

	/**
	 * Populates the fields of a {@link ClassWrapper} given a {@link DBJavaClass},
	 * building nested types if necessary.
	 */
	private void populateClassWrapperFields(
			DBObjectRef<DBJavaClass> javaClass,
			AttributeDirection attributeDirection,
			ClassWrapper classWrapper,
			SqlComplexType sqlComplexType,
			WrapperBuilderContext context,
			Wrapper wrapper) {
		List<DBJavaField> javaFields = javaClass.get().getFields();
		for (DBJavaField javaField : javaFields) {

			FieldWrapper fieldWrapper = createFieldWrapper(javaField, javaClass, wrapper);

			// If it's a primitive or directly supported type, add to the SQL type
			SqlType sqlType = getSqlType(fieldWrapper.getType());
			if (sqlType != null && javaField.getArrayDepth() <= 0) {
				sqlComplexType.addField(fieldWrapper.getName(), sqlType.getSqlTypeName() + sqlType.getDeclarationSuffix(), fieldWrapper.getFieldIndex());
			} else {
				// It's a nested complex field
				handleNestedField(fieldWrapper, javaField, attributeDirection, sqlComplexType,
						context, wrapper);
				fieldWrapper.setComplexTypeIndexInWrapper(context.getComplexTypeIndex(fieldWrapper.getType(), fieldWrapper.getArrayDepth()));
			}
			classWrapper.addField(fieldWrapper);
		}
	}

	/**
	 * Builds a single {@link FieldWrapper} instance from a {@link DBJavaField}.
	 */
	private FieldWrapper createFieldWrapper(DBJavaField javaField, DBObjectRef<DBJavaClass> parentJavaClass, Wrapper wrapper) {
		FieldWrapper fieldWrapper = new FieldWrapper();

		// Get the raw field type in string form
		String fieldJavaClassName = getCanonicalName(javaField.getJavaClassRef());

		if (isUnsupportedType(fieldJavaClassName)) {
			log.error("Encountered unsupported type for field {}: {}", javaField, fieldJavaClassName);
		}

		// Basic field setup
		fieldWrapper.setFieldIndex(javaField.getPosition());
		fieldWrapper.setName(javaField.getName());
		if(javaField.getAccessibility() != null)
			fieldWrapper.setAccessModifier(javaField.getAccessibility().toString());
		fieldWrapper.setType(fieldJavaClassName, getSqlType(fieldJavaClassName));

		// If array
		short arrayDepth = javaField.getArrayDepth();
		if (arrayDepth > 0) {
			fieldWrapper.setArrayDepth(arrayDepth);
		}

		// If the field is non-public, set up the getter/setter if present
		if (fieldWrapper.getAccessModifier() != FieldWrapper.AccessModifier.PUBLIC) {
			DBJavaMethod getter = javaField.findGetterMethod();
			DBJavaMethod setter = javaField.findSetterMethod();
			fieldWrapper.setGetter(getter == null ? null : getter.getSimpleName());
			fieldWrapper.setSetter(setter == null ? null : setter.getSimpleName());
		}

		// If the underlying Java class is known
		if (Strings.isEmpty(fieldWrapper.getSqlType())) {
			// Re-use the same complexTypeConversion map.
			fieldWrapper.setSqlType(wrapper.getSqlTypeName(fieldJavaClassName, fieldWrapper.getArrayDepth()));
		}

		return fieldWrapper;
	}

	/**
	 * Handles a nested field that either references a nested array type or a nested complex type.
	 */
	private void handleNestedField(
			FieldWrapper fieldWrapper,
			DBJavaField javaField,
			AttributeDirection attributeDirection,
			SqlComplexType sqlComplexType,
			WrapperBuilderContext context,
			Wrapper wrapper) {
		fieldWrapper.setComplexType(true);
		ClassWrapper fieldClassWrapper;
		if (javaField.getArrayDepth() > 0) {
			// Nested array
			fieldClassWrapper = createArrayClassWrapper(
					javaField.getJavaClassRef(),
					javaField.getArrayDepth(),
					attributeDirection,
					context,
					wrapper);
		} else {
			// Nested object
			fieldClassWrapper = createClassWrapper(
					javaField.getJavaClassRef(),
					attributeDirection,
					context,
					wrapper);
		}
		if (fieldClassWrapper != null) {
			sqlComplexType.addField(
					fieldWrapper.getName(),
					fieldClassWrapper.getCorrespondingSqlType().getName(),
					fieldWrapper.getFieldIndex());
//			field.setSqlType(sqlComplexType.getName());
		}
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
		String className;
		short arrayLength;

		public ComplexTypeKey(String className, short arrayLength) {
			this.className = className;
			this.arrayLength = arrayLength;
		}
	}

	@Value
	public static class WrapperMethodKey {
		String originalMethodName;
		String methodSignature;

		public WrapperMethodKey(String wrapperMethodName, String methodSignature) {
			this.originalMethodName = wrapperMethodName;
			this.methodSignature = methodSignature;
		}

	}
}
