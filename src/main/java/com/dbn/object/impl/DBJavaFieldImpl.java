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
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBJavaFieldMetadata;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBSchema;
import com.dbn.object.DBJavaField;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectImpl;
import com.dbn.object.lookup.DBJavaClassRef;
import com.dbn.object.type.DBJavaAccessibility;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;

import static com.dbn.object.common.property.DBObjectProperty.STATIC;
import static com.dbn.object.common.property.DBObjectProperty.FINAL;

public class DBJavaFieldImpl extends DBObjectImpl<DBJavaFieldMetadata> implements DBJavaField {
	private short index;
	private short arrayDepth;
	private String baseType;
	private String className;
	private DBJavaClassRef fieldClass;
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
		baseType = metadata.getType();
		className = metadata.getClassName();

		if(metadata.getAccessibility() == null){
			accessibility = DBJavaAccessibility.PACKAGE_PRIVATE;
		} else {
			accessibility = DBJavaAccessibility.get(metadata.getAccessibility());
		}
		String fieldClassName = metadata.getFieldClassName();
		if (fieldClassName != null) {
			DBSchema schema = parentObject.getSchema();
			fieldClass = new DBJavaClassRef(schema, fieldClassName, "SYS");
		}

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
	public DBJavaClass getFieldClass() {
		return fieldClass == null ? null : fieldClass.get();
	}

	@Override
	public boolean isStatic() {
		return is(STATIC);
	}

	@Override
	public short getArrayDepth() {
		return arrayDepth;
	}

	@Override
	public String getType() {
		return baseType;
	}

	@Override
	public short getIndex() {
		return index;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public DBJavaAccessibility getAccessibility() {
		return accessibility;
	}

	@Override
	public boolean isFinal() {
		return is(FINAL);
	}
}