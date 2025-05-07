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

import com.dbn.common.file.FileTypes;
import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBJavaResourceMetadata;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBJavaResource;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.object.type.DBObjectType;
//import com.dbn.sync.java.upload.JavaResourceUploader;
import com.intellij.openapi.fileTypes.FileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.object.common.property.DBObjectProperty.EDITABLE;
import static com.dbn.object.common.property.DBObjectProperty.INVALIDABLE;
import static com.dbn.object.type.DBObjectType.JAVA_RESOURCE;

public class DBJavaResourceImpl extends DBSchemaObjectImpl<DBJavaResourceMetadata> implements DBJavaResource {

	public DBJavaResourceImpl(DBSchema schema, DBJavaResourceMetadata metadata) throws SQLException {
		super(schema, metadata);
	}

	@Override
	public @NotNull DBObjectType getObjectType() {
		return JAVA_RESOURCE;
	}

	@Override
	protected String initObject(ConnectionHandler connection, DBObject parentObject, DBJavaResourceMetadata metadata) throws SQLException {
		return metadata.getObjectName();
	}

	private String getExtension(String objectName){
		int dotIndex = objectName.lastIndexOf('.');
		if (dotIndex > 0 && dotIndex < objectName.length() - 1) {
			return objectName.substring(dotIndex + 1);
		}
		return "";
	}

	public void initProperties() {
		super.initProperties();
		properties.set(INVALIDABLE, true);
		properties.set(EDITABLE, true);
	}

	public void initStatus(DBJavaResourceMetadata metadata) throws SQLException {
		boolean isValid = metadata.isValid();
		DBObjectStatusHolder objectStatus = getStatus();
		objectStatus.set(DBObjectStatus.VALID, isValid);
		objectStatus.set(DBContentType.CODE, DBObjectStatus.PRESENT, true);
	}

	@Override
	@Nullable
	public Icon getIcon() {
		FileType fileType = FileTypes.resolveFileType(getExtension(this.getName()));
		if(fileType == null) return null;
		return withErrorMarker(fileType.getIcon());
	}

	private Icon withErrorMarker(Icon icon) {
		return isInvalid() ? Icons.withErrorMarker(icon) : icon;
	}

	private boolean isInvalid() {
		return getObjectStatus().isNot(DBObjectStatus.VALID);
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
				getSchemaId(),
				conn -> {
					byte[] resBytes = newCode.getBytes();
					// TODO : enable this when master branch gets updated
					// JavaResourceUploader.loadResource(getProject(), conn.getConnectionId(), getQualifiedNameWithType(), resBytes);
				});
	}
}
