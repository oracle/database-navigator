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

import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBJavaParameterMetadata;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectImpl;
import com.dbn.object.lookup.DBJavaClassRef;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

@Getter
public class DBJavaParameterImpl extends DBObjectImpl<DBJavaParameterMetadata> implements DBJavaParameter {
	private short methodIndex;
	private short position;
	private short arrayDepth;

	private String parameterType;
	private DBJavaClassRef parameterClass;

	public DBJavaParameterImpl(@NotNull DBJavaMethod javaMethod, DBJavaParameterMetadata metadata) throws SQLException {
		super(javaMethod, metadata);
	}

	@Override
	public @NotNull DBObjectType getObjectType() {
		return DBObjectType.JAVA_PARAMETER;
	}

	@Override
	protected void initLists(ConnectionHandler connection) {
		super.initLists(connection);
	}

	@Override
	protected String initObject(ConnectionHandler connection, DBObject parentObject, DBJavaParameterMetadata metadata) throws SQLException {
		methodIndex = metadata.getMethodIndex();
		position = metadata.getArgumentPosition();
		arrayDepth = metadata.getArrayDepth();

		parameterType = metadata.getBaseType();
		boolean isClass = parameterType.equals("class");
		String argumentClass = metadata.getArgumentClass();

		String arrMatrix = arrayDepth > 0 ? "[]".repeat(arrayDepth) : "";

		if (isClass) {
			parameterClass = new DBJavaClassRef(parentObject.getSchema(), argumentClass, "SYS");
			String className = argumentClass.substring(argumentClass.lastIndexOf("/") + 1);
			return className + arrMatrix + " p" + position;
		} else {
			return parameterType + arrMatrix + " p" + position;
		}
	}

	@Override
	public DBJavaClass getParameterClass() {
		return parameterClass == null ? null : parameterClass.get();
	}

	@Override
	public String getPresentableText() {

		return super.getPresentableText();
	}

	@Override
	public String getParameterTypeName() {
		return parameterClass == null ?
				parameterType :
				parameterClass.getClassSimpleName();
	}

	@Override
	public short getPosition() {
		return position;
	}
}
