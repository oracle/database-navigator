/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.object.impl;

import com.dbn.browser.DatabaseBrowserUtils;
import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Java;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBJavaClassMetadata;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.editor.DBContentType;
import com.dbn.execution.java.wrapper.TypeMappings;
import com.dbn.execution.java.wrapper.WrapperModel;
import com.dbn.nls.NlsResources;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.object.filter.type.ObjectTypeFilterSettings;
import com.dbn.object.lookup.DBJavaNameCache;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBJavaAccessibility;
import com.dbn.object.type.DBJavaClassKind;
import com.dbn.object.type.DBJavaScalarType;
import com.dbn.object.type.DBObjectType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.common.util.Strings.capitalize;
import static com.dbn.object.common.property.DBObjectProperty.ABSTRACT;
import static com.dbn.object.common.property.DBObjectProperty.COMPILABLE;
import static com.dbn.object.common.property.DBObjectProperty.DEBUGABLE;
import static com.dbn.object.common.property.DBObjectProperty.EDITABLE;
import static com.dbn.object.common.property.DBObjectProperty.FINAL;
import static com.dbn.object.common.property.DBObjectProperty.INNER;
import static com.dbn.object.common.property.DBObjectProperty.INVALIDABLE;
import static com.dbn.object.common.property.DBObjectProperty.PRIMITIVE;
import static com.dbn.object.common.property.DBObjectProperty.SCALAR;
import static com.dbn.object.common.property.DBObjectProperty.SOURCE;
import static com.dbn.object.common.property.DBObjectProperty.STATIC;
import static com.dbn.object.type.DBJavaClassKind.ENUM;
import static com.dbn.object.type.DBJavaClassKind.INTERFACE;
import static com.dbn.object.type.DBObjectType.JAVA_CLASS;
import static com.dbn.object.type.DBObjectType.JAVA_FIELD;
import static com.dbn.object.type.DBObjectType.JAVA_INNER_CLASS;
import static com.dbn.object.type.DBObjectType.JAVA_METHOD;

@Getter
public class DBJavaClassImpl extends DBSchemaObjectImpl<DBJavaClassMetadata> implements DBJavaClass {
	private DBObjectRef<DBJavaClass> outerClass;

	private DBJavaClassKind kind;
	private DBJavaAccessibility accessibility;

	private static final Set<DBJavaClassImpl> INSTANCES_CHECKED_FOR_SUPPORT
			= Collections.newSetFromMap(new WeakHashMap<>());

	private boolean argumentSupportChecked;
	private boolean argumentSupported;
	private int argumentDisplayRowCount;
	private String argumentUnsupportedReason;

	private boolean returnSupportChecked;
	private boolean returnSupported;
	private int returnDisplayRowCount;
	private String returnUnsupportedReason;

	private static final String NULL_CLASS_ENCOUNTERED = "Null class encountered";

	public DBJavaClassImpl(DBSchema schema, DBJavaClassMetadata metadata) throws SQLException {
		super(schema, metadata);

		String outerClassName = metadata.getOuterClassName();
		if (Strings.isNotEmpty(outerClassName)) {
			outerClass = new DBObjectRef<>(schema.ref(), DBObjectType.JAVA_CLASS, outerClassName);
			ref.clearReference();
			ref.setParent(outerClass);
		}
	}

	@Override
	public @NotNull DBObjectType getObjectType() {
		return JAVA_CLASS;
	}

	@Override
	protected String initObject(ConnectionHandler connection, DBObject parentObject, DBJavaClassMetadata metadata) throws SQLException {
		this.kind = DBJavaClassKind.get(metadata.getObjectKind());
		this.accessibility = DBJavaAccessibility.get(metadata.getAccessibility());

		String className = metadata.getObjectName();

		set(FINAL, metadata.isFinal());
		set(ABSTRACT, metadata.isAbstract());
		set(STATIC, metadata.isStatic());
		set(INNER, metadata.isInner());
		set(PRIMITIVE, metadata.isPrimitive());
		set(SOURCE, metadata.isSource());
		set(SCALAR, isPrimitive() || DBJavaScalarType.isScalar(className));

        this.argumentSupportChecked = false;
		this.returnSupportChecked = false;
		return className;
	}


	@Override
	protected void initLists(ConnectionHandler connection) {
		super.initLists(connection);

		// TODO support inner classes as child objects
		DBSchema schema = getSchema();
		DBObjectListContainer childObjects = ensureChildObjects();
		childObjects.createSubcontentObjectList(JAVA_INNER_CLASS, this, schema);
		childObjects.createSubcontentObjectList(JAVA_FIELD, this, schema);
		childObjects.createSubcontentObjectList(JAVA_METHOD, this, schema);
	}

	public void initProperties() {
		super.initProperties();
		properties.set(COMPILABLE, true);
		properties.set(INVALIDABLE, true);
		properties.set(DEBUGABLE, true);
		properties.set(EDITABLE, !isInner());
	}

	public void initStatus(DBJavaClassMetadata metadata) throws SQLException {
		boolean isValid = metadata.isValid();
		boolean isDebug = metadata.isDebug();
		DBObjectStatusHolder objectStatus = getStatus();
		objectStatus.set(DBObjectStatus.VALID, isValid);
		objectStatus.set(DBObjectStatus.DEBUG, isDebug);
		objectStatus.set(DBContentType.CODE, DBObjectStatus.PRESENT, true);
	}

	@Override
	public String getPresentableName() {
		return isInner() ?
                getSimpleName() :
                getCanonicalName();

	}

	@Override
	public String getCanonicalName() {
		return DBJavaNameCache.getCanonicalName(ref());
	}

	@Override
	public String getSimpleName() {
		return DBJavaNameCache.getSimpleName(ref());
	}

	@Override
	public String getPackageName() {
		return Java.getPackageName(getCanonicalName());
	}

	@Override
	public String getQualifiedName() {
		return getSchemaName() + "." + getCanonicalName();
	}

	@Override
	public String getQualifiedNameWithType() {
		return NlsResources.txt("app.object.label.QualifiedNameWithType", JAVA_CLASS.getName(), getQualifiedName());
	}

	@Override
	@Nullable
	public Icon getIcon() {
		if (kind == ENUM) return withErrorMarker(Icons.DBO_JAVA_ENUMERATION);
		if (kind == INTERFACE) return withErrorMarker(Icons.DBO_JAVA_INTERFACE);
		if (isAbstract()) return withErrorMarker(Icons.DBO_JAVA_CLASS_ABSTRACT);
		return withErrorMarker(withFinalMarker(Icons.DBO_JAVA_CLASS));
	}

	private Icon withErrorMarker(Icon base) {
		return isInvalid() ? Icons.withErrorMarker(base) : base;
	}

	private Icon withFinalMarker(Icon base) {
		return isFinal() ? Icons.withPinMarker(base): base;
	}

	private boolean isInvalid() {
		return getObjectStatus().isNot(DBObjectStatus.VALID);
	}

	@Override
	public boolean isFinal() {
		return is(FINAL);
	}

	@Override
	public boolean isAbstract() {
		return is(ABSTRACT);
	}

	@Override
	public boolean isStatic() {
		return is(STATIC);
	}

	@Override
	public boolean isInner() {
		return is(INNER);
	}

	@Override
	public boolean isPrimitive() {
		return is(PRIMITIVE);
	}

	@Override
	public boolean isScalar() {
		return is(SCALAR);
	}

	@Override
	public boolean isSource() {
		return is(SOURCE);
	}

	@Override
	public boolean isArgumentSupported() {
		if(!argumentSupportChecked)
			checkForArgumentSupport();
		return argumentSupported;
	}

	@Override
	public boolean isArgumentSupported(short arrayDepth) {
		if(!isArraySupported(arrayDepth))
			return false;
		return this.isArgumentSupported();
	}

	@Override
	public boolean isReturnSupported() {
		if(!returnSupportChecked)
			checkForReturnSupport();
		return returnSupported;
	}

	@Override
	public boolean isReturnSupported(short arrayDepth) {
		if(!isArraySupported(arrayDepth))
			return false;
		return this.isReturnSupported();
	}

	@Override
	public String getArgumentUnsupportedReason() {
		if(!argumentSupportChecked)
			checkForArgumentSupport();
		return argumentUnsupportedReason;
	}

	@Override
	public String getArgumentUnsupportedReason(short arrayDepth) {
		if(!isArraySupported(arrayDepth))
			return "Array "+getCanonicalName() + "[]".repeat(arrayDepth) + " is not supported.";
		return getArgumentUnsupportedReason();
	}

	@Override
	public String getReturnUnsupportedReason() {
		if(!returnSupportChecked)
			checkForReturnSupport();
		return returnUnsupportedReason;
	}

	@Override
	public String getReturnUnsupportedReason(short arrayDepth) {
		if(!isArraySupported(arrayDepth))
			return "Array "+getCanonicalName() + "[]".repeat(arrayDepth) + " is not supported.";
		return getReturnUnsupportedReason();
	}

	@Override
	public int getArgumentDisplayRowCount() {
		if(!argumentSupportChecked)
			checkForArgumentSupport();
		return argumentDisplayRowCount;
	}

	@Override
	public int getArgumentDisplayRowCount(short arrayDepth) {
		if(!isArraySupported(arrayDepth))
			return -1;
		return getArgumentDisplayRowCount();
	}

	@Override
	public int getReturnDisplayRowCount() {
		if(!returnSupportChecked)
			checkForReturnSupport();
		return returnDisplayRowCount;
	}

	@Override
	public int getReturnDisplayRowCount(short arrayDepth) {
		if(!isArraySupported(arrayDepth))
			return -1;
		return getReturnDisplayRowCount();
	}

	/**
	 Computes and caches the number of nested fields.
	 Behavior:
	 If not yet computed, call countNestedFields() and cache the result.
	 On success (result != -1), clear unsupportedReason.
	 If countNestedFields() reported "Null Class", it means dependent classes
	 aren't loaded yet. Clear the cache so a later call will retry, and return -1
	 to signal the caller to try again once classes are available.
	 */
	private void checkForArgumentSupport() {
		argumentDisplayRowCount = countNestedFields(false);
		if(argumentDisplayRowCount != -1)
			argumentSupported = true;

		if(!NULL_CLASS_ENCOUNTERED.equals(argumentUnsupportedReason)) {
			argumentSupportChecked = true;
			INSTANCES_CHECKED_FOR_SUPPORT.add(this);
		}
	}

	private void checkForReturnSupport() {
		returnDisplayRowCount = countNestedFields(true);
		if(returnDisplayRowCount != -1)
			returnSupported = true;
		if(!NULL_CLASS_ENCOUNTERED.equals(argumentUnsupportedReason)) {
			returnSupportChecked = true;
			INSTANCES_CHECKED_FOR_SUPPORT.add(this);
		}
	}

	private boolean isArraySupported(short arrayDepth) {
		if(arrayDepth == 0)
			return true;
		if(isScalar() && arrayDepth < 2)
			return true;
		return false;
	}


	private int countNestedFields(boolean isReturnType) {
		Set<String> classesSeenTillNow = new HashSet<>();
		return countNestedFields(classesSeenTillNow, isReturnType);
	}

	private int countNestedFields(Set<String> classesSeenTillNow,
								  boolean isReturnType) {

		if (hasUnsupportedStructure(classesSeenTillNow, isReturnType)) {
			return -1;
		}

		if (isScalar()) {
			return 1;
		}

		List<DBJavaField> fields = getFields();
		if (fields == null || fields.isEmpty()) {
			//TODO
			/*
			class is not scalar, yet it has not fields, will think more about this case !!!!!!
			 */
			return 1;
		}

		classesSeenTillNow.add(getCanonicalName());
		/*
		We count visible rows, not just input leaves. Even if only leaves need input from user,
		their ancestors are shown in the UI and take space too. Example
		     A
		      |-- a1
		      |     |-- a1.1
		      |-- a2
		            |--a2.1
		            |--a2.2
		    Input leaves: a1.1, a2.1, a2.2 → 3
			Visible rows: A, a1, a1.1, a2, a2.1, a2.2 → 6
			Count = 1 to include the displayed ancestors.
		 */
		int count = 1;
		try {
			for (DBJavaField field : fields) {
				if(!(field.getAccessibility() == DBJavaAccessibility.PUBLIC)) {
					if (!isReturnType && field.findSetterMethod() == null) {
						argumentUnsupportedReason
								= "get" + capitalize(field.getName()) + " settor method not found for "
								+ getCanonicalName() + "." + Strings.capitalize(field.getName());
						return -1;
					}
					if (isReturnType && field.findGetterMethod() == null) {
						returnUnsupportedReason
								= "set" + capitalize(field.getName()) + " getter method not found for "
								+ getCanonicalName() + "." + Strings.capitalize(field.getName());
						return -1;
					}
				}
				DBJavaClass fieldClass = field.getJavaClass();
				short fieldDepth = field.getArrayDepth();
				if(fieldClass == null) {
					if(!isReturnType){
						argumentUnsupportedReason = NULL_CLASS_ENCOUNTERED;
					} else {
						returnUnsupportedReason = NULL_CLASS_ENCOUNTERED;
					}
					return -1;
				} else {
					if(!isReturnType) {
						if(fieldClass.isArgumentSupported(fieldDepth)) {
							count = count +fieldClass.getArgumentDisplayRowCount(fieldDepth);
						} else {
							argumentUnsupportedReason = fieldClass.getArgumentUnsupportedReason(fieldDepth);
							return -1;
						}
					} else {
						if(fieldClass.isReturnSupported(fieldDepth)) {
							count = count +fieldClass.getReturnDisplayRowCount(fieldDepth);
						} else {
							returnUnsupportedReason = fieldClass.getReturnUnsupportedReason(fieldDepth);
							return -1;
						}
					}
				}
			}
			return count;
		}
		finally {
			classesSeenTillNow.remove(getCanonicalName());
		}

	}

	private boolean hasUnsupportedStructure(Set<String> classesSeenTillNow, boolean isReturnType) {

		if(isScalar())
			return false;

		String unsupportedReason = null;
		if(classesSeenTillNow.contains(getCanonicalName())) {
			unsupportedReason = "Cyclic Class";
		}

		if (TypeMappings.getUNSUPPORTED_TYPES().contains(getCanonicalName())) {
			unsupportedReason = getCanonicalName() + " is not supported";
		}

		if(!isReturnType && !hasPublicDefaultConstructor()){
			unsupportedReason = getCanonicalName() + " does not have a public default constructor";
		}

		if(unsupportedReason != null) {
			if(isReturnType)
				returnUnsupportedReason = unsupportedReason;
			else
				argumentUnsupportedReason = unsupportedReason;
			return true;
		}

		return false;
	}

	private boolean hasPublicDefaultConstructor(){
		//TODO remove false positive
		List<DBJavaMethod> methods = getMethods();
		for (DBJavaMethod method : methods) {
			if(!(method.getAccessibility() == DBJavaAccessibility.PUBLIC))
				continue;
			String methodName = method.getName();
			methodName = methodName.split("#")[0];
			if(methodName.equals("<init>") && method.getParameters().isEmpty()){
				/*
		 		gives false positive because method prameters are yet not loaded
		 		*/
				if(method.getSignature().contains("()"))
					return true;
			}
		}
		return false;
	}

	@Override
	public List<DBJavaMethod> getMethods() {
		return getChildObjects(JAVA_METHOD);
	}

	@Override
	public List<DBJavaMethod> getStaticMethods() {
		return filter(getMethods(), m -> m.isStatic());
	}

	@Override
	public DBJavaMethod getMethod(String name) {
		return getChildObject(JAVA_METHOD, name);
	}

	@Override
	public List<DBJavaField> getFields() {
		return getChildObjects(JAVA_FIELD);
	}

	@Override
	public DBJavaField getField(String name) {
		return getChildObject(JAVA_FIELD, name);
	}

	@Override
	public List<DBJavaClass> getInnerClasses(){
		return getChildObjects(JAVA_INNER_CLASS);
	}

	@Override
	public DBJavaClass getInnerClass(String name){
		return getChildObject(JAVA_INNER_CLASS, name);
	}

	@Nullable
	public DBJavaClass getOuterClass() {
		return isInner() ? DBObjectRef.get(outerClass) : null;
	}

	/*********************************************************
	 *                  DBEditableCodeObject                 *
	 ********************************************************/

	@Override
	public void executeUpdateDDL(DBContentType contentType, String oldCode, String newCode) throws SQLException {

		DatabaseInterfaceInvoker.execute(HIGHEST,
				"Updating source code",
				"Updating sources of " + getQualifiedNameWithType(),
				getProject(),
				getConnectionId(),
				conn -> {
					ConnectionHandler connection = getConnection();
					DatabaseDataDefinitionInterface dataDefinitionInterface = connection.getDataDefinitionInterface();
					String schemaName = getSchemaName(true);
					String name = getName(true);

					dataDefinitionInterface.updateJavaSource(
							schemaName,
							name,
							newCode.getBytes(),
							conn);

					dataDefinitionInterface.compileJavaClass(
							schemaName,
							name,
							conn);
				});

		resetAllSupportChecks();
		WrapperModel.markAllAsStale();
	}

	private static void resetAllSupportChecks(){
		synchronized(INSTANCES_CHECKED_FOR_SUPPORT){
			for(DBJavaClassImpl dbJavaClassImpl : INSTANCES_CHECKED_FOR_SUPPORT){
				dbJavaClassImpl.argumentSupportChecked = false;
				dbJavaClassImpl.returnSupportChecked = false;
			}
		}
	}

	/*********************************************************
	 *                     TreeElement                       *
	 *********************************************************/
	@Override
	@NotNull
	public List<BrowserTreeNode> buildPossibleTreeChildren() {
		return DatabaseBrowserUtils.createList(
				getChildObjectList(JAVA_FIELD),
				getChildObjectList(JAVA_METHOD),
				getChildObjectList(JAVA_INNER_CLASS));
	}

	@Override
	public boolean hasVisibleTreeChildren() {
		ObjectTypeFilterSettings settings = getObjectTypeFilterSettings();
		return settings.isVisible(JAVA_FIELD) ||
				settings.isVisible(JAVA_METHOD) ||
				settings.isVisible(JAVA_INNER_CLASS);
	}
}
