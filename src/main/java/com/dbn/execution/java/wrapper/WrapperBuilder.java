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

import com.dbn.common.util.Lists;
import com.dbn.execution.java.wrapper.model.ClassWrapper;
import com.dbn.execution.java.wrapper.model.ClassWrapper.ArgumentDirection;
import com.dbn.execution.java.wrapper.model.FieldWrapper;
import com.dbn.execution.java.wrapper.model.MethodWrapper;
import com.dbn.execution.java.wrapper.model.ParameterWrapper;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.lookup.DBObjectRef;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.common.load.ProgressMonitor.setProgressDetail;
import static com.dbn.common.util.Lists.sortedCopy;
import static com.dbn.execution.java.wrapper.TypeMappings.getSqlType;
import static com.dbn.execution.java.wrapper.TypeMappings.isSupportedType;
import static com.dbn.execution.java.wrapper.TypeMappings.isUnsupportedType;
import static com.dbn.execution.java.wrapper.model.ClassWrapper.ArgumentDirection.IN;
import static com.dbn.execution.java.wrapper.model.ClassWrapper.ArgumentDirection.IN_OUT;
import static com.dbn.execution.java.wrapper.model.ClassWrapper.ArgumentDirection.OUT;
import static com.dbn.object.DBOrderedObject.POSITION_COMPARATOR;
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;
import static com.dbn.object.type.DBJavaScalarType.isScalar;

/**
 * Parses {@link DBJavaMethod} instances into {@link Wrapper} objects,
 * including generating the corresponding {@link ClassWrapper}
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
	public Wrapper build(DBJavaClass javaClass, List<DBJavaMethod> javaMethods, boolean useFriendlyNames) {
		// Create data structures that are unique to *this* parse call.
		WrapperBuilderContext context = new WrapperBuilderContext(useFriendlyNames);

		// Delegate to the internal parsing method.
		return context.surround(() -> buildInternal(javaClass, javaMethods));
	}

	public Wrapper build(DBJavaMethod javaMethod, boolean useFriendlyNames) {
		return build(null, List.of(javaMethod), useFriendlyNames);
	}

	// -------------------------------------------------
	// Internal Parsing Logic
	// -------------------------------------------------

	/**
	 * Internal method that actually performs the parsing to produce a {@link Wrapper}.
	 *
	 * @param javaMethods         The method definition to parse.
	 * @return The generated {@link Wrapper}.
	 */
	private Wrapper buildInternal(
			DBJavaClass javaClass,
			List<DBJavaMethod> javaMethods) {

		if (javaMethods.isEmpty()) return null;

		// Create a fresh Wrapper for this invocation
		Wrapper wrapper = javaClass == null ?
				new Wrapper(javaMethods.get(0)) :
				new Wrapper(javaClass);
		getContext().setWrapper(wrapper);

        for (DBJavaMethod javaMethod : javaMethods) {
			MethodWrapper methodWrapper = new MethodWrapper(javaMethod);
            initMethodParameters(methodWrapper);
            initMethodReturnParameter(methodWrapper);

			wrapper.addJavaMethod(methodWrapper);
        }

		return wrapper;
	}

	/**
	 * Parse all method parameters from the given {@link DBJavaMethod} and populate the wrapper.
	 */
	private void initMethodParameters(MethodWrapper methodWrapper) {
		DBJavaMethod javaMethod = methodWrapper.getJavaMethod();
		List<DBJavaParameter> parameters = javaMethod.getParameters();
		if (parameters == null) return;
		if (parameters.isEmpty()) return;

		// Sort by position to ensure correct order
		parameters = sortedCopy(parameters, POSITION_COMPARATOR);

		// Create a Wrapper.MethodAttribute for each parameter
		for (DBJavaParameter parameter : parameters) {
			var javaClass = parameter.getJavaClassRef();
			int arrayDepth = parameter.getArrayDepth();
			ParameterWrapper parameterWrapper = createParameterWrapper(javaClass, arrayDepth, IN);
			methodWrapper.addParameter(parameterWrapper);
		}
	}

	/**
	 * Parse the return type (if not void) and populate the wrapper.
	 */
	private void initMethodReturnParameter(MethodWrapper methodWrapper) {
		DBJavaMethod javaMethod = methodWrapper.getJavaMethod();
		if (javaMethod.isReturningVoid()) return;

		var javaClass = javaMethod.getReturnClassRef();
		int arrayDepth = javaMethod.getReturnArrayDepth();
		ParameterWrapper parameterWrapper = createParameterWrapper(javaClass, arrayDepth, OUT);

		methodWrapper.setReturnParameter(parameterWrapper);
    }

	/**
	 * Creates a {@link ParameterWrapper} for the given DB elements, either
	 * a simple attribute if primitive/supported, or a complex type otherwise.
	 */
	private ParameterWrapper createParameterWrapper(
			DBObjectRef<DBJavaClass> javaClass,
			int arrayDepth,
			ArgumentDirection direction) {

		String className = getCanonicalName(javaClass);
		// If non-array and we have a direct mapping -> simple attribute

		if (arrayDepth == 0 && isSupportedType(className)) {
			return createSimpleParameterWrapper(className);
		}

		// Otherwise, build or retrieve a JavaComplexType
		ClassWrapper classWrapper = createClassWrapper(javaClass, arrayDepth, direction);

		if (classWrapper == null) {
			// If still null, it's unsupported or cyclical
			return null;
		}

		// Build a complex attribute
		return createComplexParameterWrapper(classWrapper, direction);
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
	private ParameterWrapper createComplexParameterWrapper(ClassWrapper classWrapper, ArgumentDirection argumentDirection) {
		ParameterWrapper methodAttribute = new ParameterWrapper();
		methodAttribute.setArrayDepth(classWrapper.getArrayDepth());
		methodAttribute.setJavaTypeName(classWrapper.getClassName());
		methodAttribute.setSqlTypeName(classWrapper.getSqlTypeName());
		methodAttribute.setComplexType(true);

		String converterName = argumentDirection == IN ?
				classWrapper.getSqlToJavaConverterName() :
				classWrapper.getJavaToSqlConverterName();

		methodAttribute.setConverterName(converterName);

		return methodAttribute;
	}

	// -------------------------------------------------
	// Complex Type Creation Logic
	// -------------------------------------------------

	@Nullable
	private ClassWrapper createClassWrapper(DBObjectRef<DBJavaClass> javaClass, int arrayDepth, ArgumentDirection direction) {
		String className = getCanonicalName(javaClass);
		ClassWrapper classWrapper = getClassWrapper(className, arrayDepth, direction);
		if (classWrapper != null) return classWrapper;

		if (arrayDepth > 0) {
			classWrapper = createArrayClassWrapper(
					javaClass,
					arrayDepth,
					direction);
		} else {
			classWrapper = createClassWrapper(
					javaClass,
					direction);
		}
		if (classWrapper != null) {
			Wrapper wrapper = getWrapper();
			wrapper.addClassWrapper(classWrapper);
		}

		return classWrapper;
	}

	/**
	 * Creates a {@link ClassWrapper} (non-array version) for the given class
	 * or parameter type, populating its fields recursively if needed.
	 */
	private ClassWrapper createClassWrapper(
			DBObjectRef<DBJavaClass> javaClass,
			ArgumentDirection direction) {

		// do not create wrappers for non-array scalar classes
		if (isScalar(javaClass)) return null;

		// Create and cache a new plain class wrapper
		String className = getCanonicalName(javaClass);
		setProgressDetail("Creating database wrapper for class \"" + className + "\"");

		ClassWrapper classWrapper = new ClassWrapper(javaClass, 0, direction);
		getContext().cacheClassWrapper(classWrapper);

		// Populate fields if we have a DBJavaClass
		createFieldWrappers(
				javaClass.ensure(),
				direction,
				classWrapper);

		return classWrapper;
	}

	/**
	 * Creates a {@link ClassWrapper} for array types (including nested arrays),
	 * populating its contained type recursively if necessary.
	 */
	private ClassWrapper createArrayClassWrapper(
			DBObjectRef<DBJavaClass> javaClass,
			int arrayDepth,
			ArgumentDirection direction) {

		// ensure dependency stack is created
		for (int i = 0; i < arrayDepth; i++) {
			createClassWrapper(javaClass, i, direction);
		}

		// If base type is unsupported, abort
		String className = getCanonicalName(javaClass);
		if (TypeMappings.isUnsupportedType(className)) {
			log.error("Encountered unsupported type for array: {}", className);
			return null;
		}

		// Create and cache a new array class wrapper
		setProgressDetail("Creating database wrapper for class \"" + className + "\"");
		ClassWrapper classWrapper = new ClassWrapper(javaClass, arrayDepth, direction);
		getContext().cacheClassWrapper(classWrapper);

		String sqlTypeName = null;
		if (arrayDepth <= 1) {
			// Single-dimension vs multi-dimension array
			sqlTypeName = TypeMappings.getSqlTypeDeclaration(className);
			if (sqlTypeName == null) {
				// Possibly a nested complex type
				ClassWrapper containedClassWrapper = createClassWrapper(javaClass, 0, direction);
				if (containedClassWrapper != null) {
					sqlTypeName = containedClassWrapper.getSqlTypeName();
					classWrapper.setContainedClassWrapper(containedClassWrapper);
				}
			}
		} else {
			// Multi-dimensional
			ClassWrapper containedClassWrapper = createClassWrapper(javaClass, arrayDepth - 1, direction);
			if (containedClassWrapper != null) {
				sqlTypeName = containedClassWrapper.getSqlTypeName();
				classWrapper.setContainedClassWrapper(containedClassWrapper);
			}
		}

		classWrapper.setContainedSqlTypeName(sqlTypeName);

		return classWrapper;
	}

	// -------------------------------------------------
	// Caching and Direction Upgrades
	// -------------------------------------------------

	/**
	 * Returns a cached {@link ClassWrapper} if present, and upgrades its direction
	 * from ARGUMENT to BOTH if needed (when also used as a RETURN).
	 */
	@Nullable
	private ClassWrapper getClassWrapper(String className, int arrayDepth, ArgumentDirection direction) {
		WrapperBuilderContext context = getContext();
		ClassWrapper classWrapper = context.getCachedClassWrapper(className, arrayDepth);
        if (classWrapper == null) return null;

        // If it was ARGUMENT-only, and now we need a RETURN, upgrade to BOTH
        if (direction == OUT && classWrapper.getArgumentDirection() == IN) {
            changeAttributeDirection(classWrapper);
        }
        return classWrapper;
	}

	/**
	 * If a {@link ClassWrapper} was first encountered as an ARGUMENT but is also needed
	 * for RETURN, we mark it (and nested fields) as BOTH.
	 */
	private void changeAttributeDirection(ClassWrapper classWrapper) {
		WrapperBuilderContext context = getContext();
		Wrapper wrapper = getWrapper();
		classWrapper.setArgumentDirection(IN_OUT);

		if (classWrapper.isArray()) {
			// For arrays, mark all corresponding array dimension entries + the base
			String className = classWrapper.getClassName();
			for (int i = 1; i <= classWrapper.getArrayDepth(); i++) {
				ClassWrapper typeWrapper = context.getCachedClassWrapper(className, i);
				if (typeWrapper != null) {
					typeWrapper.setArgumentDirection(IN_OUT);
				}
			}
			// Also update the non-array variant if it exists
			ClassWrapper baseType = context.getCachedClassWrapper(className, 0);
			if (baseType != null) {
				changeAttributeDirection(baseType);
			}
		} else {
			// For complex objects, recursively mark subfields
			for (FieldWrapper fieldWrapper : classWrapper.getFields()) {
				if (fieldWrapper.isComplexType()) {
					String className = fieldWrapper.getTypeClassName();
					int arrayDepth = fieldWrapper.getArrayDepth();
					ClassWrapper nestedWrapper = context.getCachedClassWrapper(className, arrayDepth);
					if (nestedWrapper != null) {
						changeAttributeDirection(nestedWrapper);
					}
				}
			}
		}
	}

	// -------------------------------------------------
	// Field Population
	// -------------------------------------------------

	/**
	 * Populates the fields of a {@link ClassWrapper} given a {@link DBJavaClass},
	 * building nested types if necessary.
	 */
	private void createFieldWrappers(
			DBJavaClass javaClass,
			ArgumentDirection direction,
			ClassWrapper classWrapper) {

		List<DBJavaField> javaFields = javaClass.getFields();
		for (DBJavaField javaField : Lists.sortedCopy(javaFields, POSITION_COMPARATOR)) {

			FieldWrapper fieldWrapper = createFieldWrapper(classWrapper, javaField);

			String fieldClassName = fieldWrapper.getTypeClassName();
			SqlType sqlType = getSqlType(fieldClassName);

			// If it's a primitive or directly supported type, add to the SQL type
			if (sqlType != null && javaField.getArrayDepth() <= 0) {
				fieldWrapper.setSqlTypeName(sqlType.getSqlTypeDeclaration());
			} else {
				// It's a nested complex field
				handleNestedField(fieldWrapper, javaField, direction);
			}
			classWrapper.addField(fieldWrapper);
		}
	}

	/**
	 * Builds a single {@link FieldWrapper} instance from a {@link DBJavaField}.
	 */
	private FieldWrapper createFieldWrapper(ClassWrapper classWrapper, DBJavaField javaField) {
		Wrapper wrapper = getWrapper();

		FieldWrapper fieldWrapper = new FieldWrapper(classWrapper, javaField);

		// Get the raw field type in string form
		String fieldJavaClassName = getCanonicalName(javaField.getJavaClassRef());

		if (isUnsupportedType(fieldJavaClassName)) {
			log.error("Encountered unsupported type for field {}: {}", javaField, fieldJavaClassName);
		}

		SqlType sqlType = getSqlType(fieldJavaClassName);
		if (sqlType == null) {
			// Re-use the same complexTypeConversion map.
			DBJavaClass javaClass = javaField.getJavaClass();
			int arrayDepth = fieldWrapper.getArrayDepth();
			String sqlTypeName = wrapper.getSqlTypeName(javaClass, arrayDepth);

			fieldWrapper.setSqlTypeName(sqlTypeName);
		} else {
			fieldWrapper.setSqlTypeName(sqlType.getSqlTypeName());
			fieldWrapper.setTypeCastStart(sqlType.getTransformerPrefix());
			fieldWrapper.setTypeCastEnd(sqlType.getTransformerSuffix());

		}

		return fieldWrapper;
	}

	/**
	 * Handles a nested field that either references a nested array type or a nested complex type.
	 */
	private void handleNestedField(
			FieldWrapper fieldWrapper,
			DBJavaField javaField,
			ArgumentDirection argumentDirection) {

		fieldWrapper.setComplexType(true);
		ClassWrapper fieldClassWrapper = createClassWrapper(
				javaField.getJavaClassRef(),
				javaField.getArrayDepth(),
				argumentDirection);

		if (fieldClassWrapper != null) {
			fieldWrapper.setSqlTypeName(fieldClassWrapper.getSqlTypeName());
		}
	}

	private Wrapper getWrapper() {
		return getContext().getWrapper();
	}

	private WrapperBuilderContext getContext() {
		return WrapperBuilderContext.get();
	}
}
