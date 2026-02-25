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

package com.dbn.execution.java.wrapper.model;

import com.dbn.execution.java.wrapper.WrapperModel;
import com.dbn.execution.java.wrapper.naming.WrapperNamingProvider;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBSchema;
import com.dbn.object.DBType;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;

@Getter
@Setter
public class ClassWrapper extends EntityWrapper {
	public enum ArgumentDirection {IN, OUT, IN_OUT}

	private ClassWrapper containedClassWrapper;
	private String containedSqlTypeName;

	private final DBObjectRef<DBJavaClass> javaClass;
	private final DBObjectRef<DBType> sqlType;
	private final int arrayDepth;

	private ArgumentDirection argumentDirection;
	private List<FieldWrapper> fields = new ArrayList<>();

	public ClassWrapper(WrapperModel model, DBObjectRef<DBJavaClass> javaClass, int arrayDepth, ArgumentDirection argumentDirection) {
        super(model);
		this.javaClass = javaClass;
		this.arrayDepth = arrayDepth;
		this.argumentDirection = argumentDirection;

		this.sqlType = initSqlType();
	}

	private DBObjectRef<DBType> initSqlType() {
		DBObjectRef<DBJavaClass> javaClass = getJavaClass();

		WrapperNamingProvider namingProvider = getNamingProvider();
		String typeName = namingProvider.getSqlTypeName(getClassName(), arrayDepth);

		DBObjectRef<DBSchema> schema = javaClass.getParentRef(DBObjectType.SCHEMA);
		return new DBObjectRef<>(schema, DBObjectType.TYPE, typeName);
	}

	public boolean matches(String className, int arrayDepth) {
		return Objects.equals(getClassName(), className) && this.arrayDepth == arrayDepth;
	}

	public String getClassName() {
		return getCanonicalName(javaClass);
	}

	public String getClassPackage() {
		DBJavaClass javaClass = DBObjectRef.get(this.javaClass);
		if (javaClass == null) return null;
		return javaClass.getPackageName();
	}


	public String getSqlTypeName() {
		return sqlType.getObjectName();
	}

	public boolean isArray() {
		return arrayDepth > 0;
	}

	public String getSqlToJavaConverterName() {
		return getSqlTypeName() + "_TO_JAVA";
	}

	public String getJavaToSqlConverterName() {
		return getSqlTypeName() + "_TO_SQL";
	}

	// Method to add field (only if not an array)
	public void addField(FieldWrapper fieldWrapper) {
		fields.add(fieldWrapper);
	}
}