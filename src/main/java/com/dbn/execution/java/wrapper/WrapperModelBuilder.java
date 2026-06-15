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
import com.dbn.common.util.Strings;
import com.dbn.execution.java.wrapper.model.ClassWrapper;
import com.dbn.execution.java.wrapper.model.ClassWrapper.ArgumentDirection;
import com.dbn.execution.java.wrapper.model.FieldWrapper;
import com.dbn.execution.java.wrapper.model.MethodWrapper;
import com.dbn.execution.java.wrapper.model.ParameterWrapper;
import com.dbn.execution.java.wrapper.naming.WrapperNamingProvider;
import com.dbn.execution.java.wrapper.support.WrapperSupportData;
import com.dbn.execution.java.wrapper.support.WrapperSupportInfo;
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
import static com.dbn.execution.java.wrapper.support.WrapperSupportEvaluator.evaluateArgumentSupport;
import static com.dbn.execution.java.wrapper.support.WrapperSupportEvaluator.evaluateReturnArgumentSupport;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.DBOrderedObject.POSITION_COMPARATOR;
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;
import static com.dbn.object.type.DBJavaScalarType.isScalar;

/**
 * Parses {@link DBJavaMethod} instances into {@link WrapperModel} objects,
 * including generating the corresponding {@link ClassWrapper}
 *
 * <p>This implementation uses a Singleton pattern and keeps
 * all per-parse mutable state in local variables inside
 *  so that it is thread-safe
 * if multiple threads call it simultaneously.</p>
 */
@Slf4j
public final class WrapperModelBuilder {

	/**
	 * The single, static instance of WrapperBuilder.
	 */
	private static final WrapperModelBuilder INSTANCE = new WrapperModelBuilder();

	/**
	 * Private constructor to enforce Singleton usage.
	 */
	private WrapperModelBuilder() {
		// no-op
	}

	public static WrapperModelBuilder getInstance() {
		return INSTANCE;
	}

	public WrapperModel buildModel(WrapperModelInput input, WrapperNamingProvider namingProvider) {
		// Create data structures that are unique to *this* parse call.
		WrapperContext context = new WrapperContext(input, namingProvider);
		return buildModel(context);
	}

	public WrapperModel buildModel(WrapperModelInput input) {
        // Create data structures that are unique to *this* parse call.
        WrapperContext context = new WrapperContext(input);

        return buildModel(context);
    }
	// -------------------------------------------------
	// Internal Parsing Logic
	// -------------------------------------------------

	/**
	 * Internal method that actually performs the parsing to produce a {@link WrapperModel}.
	 * @return The generated {@link WrapperModel}.
	 */
	private WrapperModel buildModel(WrapperContext context) {
        WrapperModelInput input = context.getInput();

        List<DBJavaMethod> targetMethods = input.getJavaMethods();
        if (targetMethods.isEmpty()) return null;

		// Create a fresh Wrapper for this invocation
		WrapperModel model = new WrapperModel(context);
		context.setModel(model);

        for (DBJavaMethod javaMethod : targetMethods) {
            MethodWrapper methodWrapper = new MethodWrapper(model, javaMethod);
            initMethodParameters(context, methodWrapper);
            initMethodReturnParameter(context, methodWrapper);

			model.addJavaMethod(methodWrapper);
        }

		return model;
	}

	/**
	 * Parse all method parameters from the given {@link DBJavaMethod} and populate the wrapper.
	 */
	private void initMethodParameters(WrapperContext context, MethodWrapper methodWrapper) {
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

			String codeInput = getCodeInput(context, parameter);

			//TODO review this
			if(context.getInput().isTemporary()) {
				WrapperSupportData supportData = context.getInput().getSupportData();
				WrapperSupportInfo supportInfo = evaluateArgumentSupport(parameter, supportData);
				if (!supportInfo.isSupported()) {
					WrapperModel model = context.getModel();
					model.addError(supportInfo.getUnsupportedReason());
				}
			}

			ParameterWrapper parameterWrapper = createParameterWrapper(
                    	context,
                    	javaClass,
                    	arrayDepth,
						codeInput,
						IN);

			methodWrapper.addParameter(parameterWrapper);
		}
	}

	private String getCodeInput(WrapperContext context, DBJavaParameter parameter) {
		WrapperModelInput input = context.getInput();
		if (!input.isTemporary()) return null;
		return input.getCodeInputs().get(parameter.getName());
	}

	/**
	 * Parse the return type (if not void) and populate the wrapper.
	 */
	private void initMethodReturnParameter(WrapperContext context, MethodWrapper methodWrapper) {
		DBJavaMethod javaMethod = methodWrapper.getJavaMethod();
		if (javaMethod.isReturningVoid()) return;

		var javaClass = javaMethod.getReturnClassRef();
		short arrayDepth = javaMethod.getReturnArrayDepth();

		WrapperSupportData supportData = context.getInput().getSupportData();
		WrapperSupportInfo supportInfo = evaluateReturnArgumentSupport(javaClass, arrayDepth, supportData);

		boolean isIncompatible = !supportInfo.isSupported();
		if(isIncompatible) {
			context.getModel().
					addError(supportInfo.getUnsupportedReason());
		}

		ParameterWrapper parameterWrapper = createParameterWrapper(
                context,
                javaClass,
                arrayDepth,
				null,
                OUT);

		methodWrapper.setReturnParameter(parameterWrapper);
    }

	/**
	 * Creates a {@link ParameterWrapper} for the given DB elements, either
	 * a simple attribute if primitive/supported, or a complex type otherwise.
	 */
	private ParameterWrapper createParameterWrapper(
            WrapperContext context,
			DBObjectRef<DBJavaClass> javaClass,
			int arrayDepth,
			String codeInput,
			ArgumentDirection direction) {

		String className = getCanonicalName(javaClass);
		// If non-array and we have a direct mapping -> simple attribute

		if (arrayDepth == 0 && isSupportedType(className)) {
			return createSimpleParameterWrapper(context, className);
		}

		if(Strings.isNotEmpty(codeInput)) {
			return createCodeParameterWrapper(context, className, arrayDepth, codeInput, direction);
		}

		// Otherwise, build or retrieve a JavaComplexType
		ClassWrapper classWrapper = createClassWrapper(
                context,
                javaClass,
                arrayDepth,
                direction);

		if (classWrapper == null) {
			// If still null, it's unsupported or cyclical
			return null;
		}

		// Build a complex attribute
		return createComplexParameterWrapper(
                context,
                classWrapper,
                direction);
	}

	/**
	 * Builds a simple (non-complex) method attribute with a known SQL type mapping.
	 */
	private ParameterWrapper createSimpleParameterWrapper(
			WrapperContext context,
			String javaClassName) {
        WrapperModel model = context.getModel();
        ParameterWrapper methodAttribute = new ParameterWrapper(model);
		methodAttribute.setJavaTypeName(javaClassName);

		String sqlTypeName = TypeMappings.getSqlTypeName(javaClassName);
		methodAttribute.setSqlTypeName(sqlTypeName);
		methodAttribute.setComplexType(false);
		return methodAttribute;
	}

	/**
	 * Builds a simple (non-complex) method attribute with a known SQL type mapping.
	 */
	private ParameterWrapper createCodeParameterWrapper(
			WrapperContext context,
			String javaClassName,
			int arrayDepth,
			String codeInput,
			ArgumentDirection direction) {

		WrapperModel model = context.getModel();
		ParameterWrapper parameter = new ParameterWrapper(model);
		parameter.setJavaTypeName(javaClassName);
		if(direction == ArgumentDirection.OUT) {
			parameter.setSqlTypeName(TypeMappings.getSqlTypeName("java.lang.String"));
		}
		parameter.setArrayDepth(arrayDepth);
		parameter.setComplexType(false);
		parameter.setCodeInput(codeInput);
		return parameter;
	}

	/**
	 * Builds a method attribute that is backed by a {@link ClassWrapper}.
	 */
	private ParameterWrapper createComplexParameterWrapper(
			WrapperContext context,
			ClassWrapper classWrapper,
			ArgumentDirection argumentDirection) {
        WrapperModel model = context.getModel();
        ParameterWrapper methodAttribute = new ParameterWrapper(model);
		methodAttribute.setArrayDepth(classWrapper.getArrayDepth());
		methodAttribute.setJavaTypeName(classWrapper.getClassName());
		methodAttribute.setSqlTypeName(classWrapper.getSqlTypeName());
		methodAttribute.setSqlType(classWrapper.getSqlType());
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
	private ClassWrapper createClassWrapper(
            WrapperContext context,
            DBObjectRef<DBJavaClass> javaClass,
            int arrayDepth,
            ArgumentDirection direction) {
		String className = getCanonicalName(javaClass);
		ClassWrapper classWrapper = getClassWrapper(
                context,
                className,
                arrayDepth,
                direction);

		if (classWrapper != null) return classWrapper;

		if (arrayDepth > 0) {
			classWrapper = createArrayClassWrapper(
                    context,
					javaClass,
					arrayDepth,
					direction);
		} else {
			classWrapper = createClassWrapper(
                    context,
					javaClass,
					direction);
		}
		if (classWrapper != null) {
			WrapperModel model = context.getModel();
			model.addClassWrapper(classWrapper);
		}

		return classWrapper;
	}

	/**
	 * Creates a {@link ClassWrapper} (non-array version) for the given class
	 * or parameter type, populating its fields recursively if needed.
	 */
	private ClassWrapper createClassWrapper(
            WrapperContext context,
			DBObjectRef<DBJavaClass> javaClass,
			ArgumentDirection direction) {

		// do not create wrappers for non-array scalar classes
		if (isScalar(javaClass)) return null;

		// Create and cache a new plain class wrapper
		String className = getCanonicalName(javaClass);
		setProgressDetail(txt("prc.java.text.CreatingDatabaseWrapperForClass", className));

        WrapperModel model = context.getModel();
        ClassWrapper classWrapper = new ClassWrapper(model, javaClass, 0, direction);
        context.cacheClassWrapper(classWrapper);

		// Populate fields if we have a DBJavaClass
		createFieldWrappers(
                context,
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
            WrapperContext context,
			DBObjectRef<DBJavaClass> javaClass,
			int arrayDepth,
			ArgumentDirection direction) {

		// ensure dependency stack is created
		for (int i = 0; i < arrayDepth; i++) {
			createClassWrapper(context, javaClass, i, direction);
		}

		// If base type is unsupported, abort
		String className = getCanonicalName(javaClass);
		if (TypeMappings.isUnsupportedType(className)) {
			log.error("Encountered unsupported type for array: {}", className);
			return null;
		}

		// Create and cache a new array class wrapper
		setProgressDetail(txt("prc.java.text.CreatingDatabaseWrapperForClass", className));

        WrapperModel model = context.getModel();
        ClassWrapper classWrapper = new ClassWrapper(model, javaClass, arrayDepth, direction);
		context.cacheClassWrapper(classWrapper);

		String sqlTypeName = null;
		if (arrayDepth <= 1) {
			// Single-dimension vs multi-dimension array
			sqlTypeName = TypeMappings.getSqlTypeDeclaration(className);
			if (sqlTypeName == null) {
				// Possibly a nested complex type
				ClassWrapper containedClassWrapper = createClassWrapper(context, javaClass, 0, direction);
				if (containedClassWrapper != null) {
					sqlTypeName = containedClassWrapper.getSqlTypeName();
					classWrapper.setContainedClassWrapper(containedClassWrapper);
				}
			}
		} else {
			// Multi-dimensional
			ClassWrapper containedClassWrapper = createClassWrapper(context, javaClass, arrayDepth - 1, direction);
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
	private ClassWrapper getClassWrapper(WrapperContext context, String className, int arrayDepth, ArgumentDirection direction) {
		ClassWrapper classWrapper = context.getCachedClassWrapper(className, arrayDepth);
        if (classWrapper == null) return null;

        // If it was ARGUMENT-only, and now we need a RETURN, upgrade to BOTH
        ArgumentDirection wrapperDirection = classWrapper.getArgumentDirection();
        if (wrapperDirection != IN_OUT && wrapperDirection != direction) {
            changeAttributeDirection(context, classWrapper);
        }
        return classWrapper;
	}

	/**
	 * If a {@link ClassWrapper} was first encountered as an ARGUMENT but is also needed
	 * for RETURN, we mark it (and nested fields) as BOTH.
	 */
	private void changeAttributeDirection(WrapperContext context, ClassWrapper classWrapper) {
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
				changeAttributeDirection(context, baseType);
			}
		} else {
			// For complex objects, recursively mark subfields
			for (FieldWrapper fieldWrapper : classWrapper.getFields()) {
				if (fieldWrapper.isComplexType()) {
					String className = fieldWrapper.getTypeClassName();
					int arrayDepth = fieldWrapper.getArrayDepth();
					ClassWrapper nestedWrapper = context.getCachedClassWrapper(className, arrayDepth);
					if (nestedWrapper != null) {
						changeAttributeDirection(context, nestedWrapper);
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
            WrapperContext context,
			DBJavaClass javaClass,
			ArgumentDirection direction,
			ClassWrapper classWrapper) {

		List<DBJavaField> javaFields = javaClass.getFields();
		for (DBJavaField javaField : Lists.sortedCopy(javaFields, POSITION_COMPARATOR)) {

			FieldWrapper fieldWrapper = createFieldWrapper(context, classWrapper, javaField);

			String fieldClassName = fieldWrapper.getTypeClassName();
			SqlType sqlType = getSqlType(fieldClassName);

			// If it's a primitive or directly supported type, add to the SQL type
			if (sqlType != null && javaField.getArrayDepth() <= 0) {
				fieldWrapper.setSqlTypeName(sqlType.getSqlTypeDeclaration());
			} else {
				// It's a nested complex field
				handleNestedField(context, fieldWrapper, javaField, direction);
			}
			classWrapper.addField(fieldWrapper);
		}
	}

	/**
	 * Builds a single {@link FieldWrapper} instance from a {@link DBJavaField}.
	 */
	private FieldWrapper createFieldWrapper(WrapperContext context, ClassWrapper classWrapper, DBJavaField javaField) {
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

			String sqlTypeName = getSqlTypeName(context, javaClass, arrayDepth);

			fieldWrapper.setSqlTypeName(sqlTypeName);
		} else {
			fieldWrapper.setSqlTypeName(sqlType.getSqlTypeName());
			fieldWrapper.setTypeCastStart(sqlType.getTransformerPrefix());
			fieldWrapper.setTypeCastEnd(sqlType.getTransformerSuffix());

		}

		return fieldWrapper;
	}

    public String getSqlTypeName(WrapperContext context, DBJavaClass javaClass, int arrayDepth) {
        WrapperNamingProvider namingProvider = context.getNamingProvider();
        return namingProvider.getSqlTypeName(javaClass, arrayDepth);
    }

	/**
	 * Handles a nested field that either references a nested array type or a nested complex type.
	 */
	private void handleNestedField(
            WrapperContext context,
			FieldWrapper fieldWrapper,
			DBJavaField javaField,
			ArgumentDirection argumentDirection) {

		fieldWrapper.setComplexType(true);
		ClassWrapper fieldClassWrapper = createClassWrapper(
                context,
				javaField.getJavaClassRef(),
				javaField.getArrayDepth(),
				argumentDirection);

		if (fieldClassWrapper != null) {
			fieldWrapper.setSqlTypeName(fieldClassWrapper.getSqlTypeName());
		}
	}
}
