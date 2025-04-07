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
import com.dbn.execution.java.wrapper.model.FieldWrapper;
import com.dbn.execution.java.wrapper.model.MethodWrapper;
import com.dbn.execution.java.wrapper.naming.WrapperNamingProvider;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBMethod;
import com.dbn.object.DBPackage;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;


@Getter
@Setter
public class Wrapper {
	private final DBObjectRef<?> sourceObject;
	private final DBObjectRef<DBJavaClass> javaClass;
	private DBObjectRef<DBJavaClass> javaWrapperClass;
	private DBObjectRef<DBPackage> sqlWrapperPackage;
	private DBObjectRef<DBMethod> sqlWrapperMethod;

	private Set<String> sqlTypeNames = new HashSet<>();
	private List<MethodWrapper> methods = new ArrayList<>();
    private List<ClassWrapper> classes = new ArrayList<>();

	public Wrapper(@NotNull DBJavaClass javaClass) {
		this.sourceObject = DBObjectRef.of(javaClass);
		this.javaClass = DBObjectRef.of(javaClass);
		initWrapperNames(javaClass);
	}

	public Wrapper(@NotNull DBJavaMethod javaMethod) {
		this.sourceObject = DBObjectRef.of(javaMethod);
		this.javaClass = javaMethod.getOwnerClassRef();
		initWrapperNames(javaMethod);
	}

	private void initWrapperNames(DBObject sourceObject) {
		WrapperNamingProvider namingProvider = getNamingProvider();
		String javaWrapperName = namingProvider.getJavaWrapperName(sourceObject);
		DBObjectRef<?> schemaRef = javaClass.getParentRef();

		this.javaWrapperClass = new DBObjectRef<>(schemaRef, DBObjectType.JAVA_CLASS, javaWrapperName);

		String sqlWrapperName = namingProvider.getSqlWrapperName(sourceObject);
		if (sourceObject instanceof DBJavaClass) {
			sqlWrapperPackage = new DBObjectRef<>(schemaRef, DBObjectType.PACKAGE, sqlWrapperName);
		} else if (sourceObject instanceof DBJavaMethod) {
			DBJavaMethod javaMethod = (DBJavaMethod) sourceObject;
			DBObjectType methodType = javaMethod.isReturningVoid() ?
					DBObjectType.PROCEDURE :
					DBObjectType.FUNCTION;

			sqlWrapperMethod = new DBObjectRef<>(schemaRef, methodType, sqlWrapperName);
		}
	}

	public String getClassName() {
		return getCanonicalName(javaClass);
	}

	public String getSqlTypeName(DBJavaClass javaClass, int arrayDepth) {
		WrapperNamingProvider namingProvider = getNamingProvider();
		return namingProvider.getSqlTypeName(javaClass, arrayDepth);
	}

	public void addClassWrapper(ClassWrapper classWrapper) {
        classes.add(classWrapper);
    }

	public ClassWrapper getClassWrapper(String className, int arrayDepth) {
		return Lists.first(classes, c -> c.matches(className, arrayDepth));
	}

	public ClassWrapper getFieldClassWrapper(FieldWrapper fieldWrapper) {
		String className = fieldWrapper.getTypeClassName();
		int arrayDepth = fieldWrapper.getArrayDepth();
		return getClassWrapper(className, arrayDepth);
	}

	public void addJavaMethod(MethodWrapper javaMethod) {
		methods.add(javaMethod);
	}

	private static WrapperBuilderContext getContext() {
		return WrapperBuilderContext.get();
	}

	private static WrapperNamingProvider getNamingProvider() {
		return getContext().getNamingProvider();
	}

	public DBObject getSourceObject() {
		return this.sourceObject.ensure();
	}

	public DBObjectRef getSourceObjectRef() {
		return this.sourceObject;
	}

	public Object isClassWrapper() {
		return sourceObject.getObjectType().matches(DBObjectType.JAVA_CLASS);
	}

	public String getJavaWrapperName() {
		return javaWrapperClass.getObjectName();
	}

	public String getSqlWrapperName() {
		return sqlWrapperMethod != null ? sqlWrapperMethod.getObjectName() : sqlWrapperPackage.getObjectName();
	}

	public List<DBObjectRef> getWrapperObjects() {
		List<DBObjectRef> wrapperObjects = new ArrayList<>();
		wrapperObjects.add(javaWrapperClass);
		wrapperObjects.add(sqlWrapperMethod);
		wrapperObjects.add(sqlWrapperPackage);
		classes.forEach(c -> wrapperObjects.add(c.getSqlType()));
		return Lists.filter(wrapperObjects, o -> o != null);
	}
}