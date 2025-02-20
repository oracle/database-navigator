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

package com.dbn.object.impl;

import com.dbn.common.icon.Icons;
import com.dbn.common.util.Java;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBJavaFieldMetadata;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaField;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectImpl;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBJavaAccessibility;
import com.dbn.object.type.DBJavaValueType;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.util.Java.isVoid;
import static com.dbn.common.util.Strings.capitalize;
import static com.dbn.object.common.property.DBObjectProperty.FINAL;
import static com.dbn.object.common.property.DBObjectProperty.PRIMITIVE;
import static com.dbn.object.common.property.DBObjectProperty.STATIC;
import static com.dbn.object.type.DBObjectType.JAVA_CLASS;

@Getter
public class DBJavaFieldImpl extends DBObjectImpl<DBJavaFieldMetadata> implements DBJavaField {
	private short index;
	private short arrayDepth;
	private DBObjectRef<DBJavaClass> javaClass;
	private DBJavaAccessibility accessibility;

	public DBJavaFieldImpl(@NotNull DBJavaClass javaClass, DBJavaFieldMetadata metadata) throws SQLException {
		super(javaClass, metadata);
	}

	@Override
	public @NotNull DBObjectType getObjectType() {
		return DBObjectType.JAVA_FIELD;
	}

	@Override
	protected String initObject(ConnectionHandler connection, DBObject parentObject, DBJavaFieldMetadata metadata) throws SQLException {
		index = metadata.getFieldIndex();
		arrayDepth = metadata.getArrayDepth();

		String fieldClassName = metadata.getFieldClassName();
		set(PRIMITIVE, Java.isPrimitive(fieldClassName));

		DBSchema schema = nd(parentObject.getSchema());
		javaClass = new DBObjectRef<>(DBObjectRef.of(schema), JAVA_CLASS, fieldClassName);

		accessibility =  metadata.getAccessibility() == null ?
				DBJavaAccessibility.DEFAULT :
				DBJavaAccessibility.get(metadata.getAccessibility());

		set(STATIC, metadata.isStatic());
		set(FINAL, metadata.isFinal());
		return metadata.getFieldName();
	}

	@Override
	public short getPosition() {
		return index;
	}

	@Override
	@Nullable
	public Icon getIcon() {
		// TODO accessibility overlays
		return Icons.DBO_JAVA_FIELD;
	}


	@Override
	public DBJavaClass getOwnerClass() {
		return getParentObject();
	}

	@Override
	public String getOwnerClassName() {
		return getOwnerClass().getName();
	}

	@Override
	public boolean isStatic() {
		return is(STATIC);
	}

	@Override
	public boolean isFinal() {
		return is(FINAL);
	}

	@Override
	public boolean isArray() {
		return arrayDepth > 0;
	}

	@Override
	public boolean isClass() {
		return !isPrimitive();
	}

	@Override
	public boolean isPrimitive() {
		return is(PRIMITIVE);
	}

	@Override
	public boolean isPlainValue() {
		return isPrimitive() || getValueType() != null;
	}

	@Override
	public DBJavaValueType getValueType() {
		return DBJavaValueType.forObjectName(javaClass.getObjectName());
	}

	public DBJavaClass getJavaClass() {
		return javaClass.get();
	}

	@Override
	public DBObjectRef<DBJavaClass> getJavaClassRef() {
		return javaClass;
	}

	@Override
	public String getJavaClassName() {
		return javaClass.getObjectName();
	}

	@Override
	public @Nullable DBJavaMethod findGetterMethod() {
		DBJavaClass ownerClass = getOwnerClass();
		String getterName = "get" + capitalize(getName());
		List<DBJavaMethod> methods = ownerClass.getMethods();
		for (DBJavaMethod method : methods) {
			String methodName = method.getName();
			methodName = methodName.split("#")[0];

			if (!Objects.equals(methodName, getterName)) continue;
			if (!Objects.equals(method.getReturnClassRef(), getJavaClassRef())) continue;
			if (!Objects.equals(method.getReturnArrayDepth(), getArrayDepth())) continue;

			return method;
		}
		return null;
	}

	@Override
	public @Nullable DBJavaMethod findSetterMethod() {
		DBJavaClass ownerClass = getOwnerClass();
		String setterName = "set" + capitalize(getName());
		List<DBJavaMethod> methods = ownerClass.getMethods();
		for (DBJavaMethod method : methods) {
			String methodName = method.getName();
			methodName = methodName.split("#")[0];

			if (!Objects.equals(methodName, setterName)) continue;
			if (!isVoid(method.getReturnClassName())) continue;
			// TODO
			List<DBJavaParameter> parameters = method.getParameters();
			if (parameters.size() != 1) continue;

			DBJavaParameter parameter = parameters.get(0);
			if (!Objects.equals(parameter.getJavaClassRef(), getJavaClassRef())) continue;
			if (!Objects.equals(parameter.getArrayDepth(), getArrayDepth())) continue;

			return method;
		}
		return null;
	}
}