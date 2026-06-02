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

package com.dbn.sync.java.upload;

import com.dbn.common.Priority;
import com.dbn.common.file.util.VirtualFiles;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseJavaInterface;
import com.dbn.object.DBJavaEntity;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.sql.SQLException;

@UtilityClass
public class JavaUploader {

	@SneakyThrows
	public static void uploadJavaArchive(JavaUploadBatch batch, VirtualFile archiveFile) {
		VirtualFile fileRoot = VirtualFiles.getJarFileRoot(archiveFile);
		if (fileRoot == null) {
			throw new IllegalArgumentException("Invalid archive file: " + archiveFile.getPath());
		}
		for (VirtualFile child : fileRoot.getChildren()) {
			createUploadTasks(batch, child);
		}
	}

	private static void createUploadTasks(JavaUploadBatch batch, VirtualFile file) {
		if (file.isDirectory()) {
			VirtualFile[] children = file.getChildren();
			for (VirtualFile child : children) {
				createUploadTasks(batch, child);
			}
		} else {
			batch.createTask(file);
		}
	}

	public static void uploadJavaResource(JavaUploadBatch batch, DBObjectRef<DBJavaEntity> entity, byte[] resourceBytes) throws SQLException {
		String schemaName = entity.getSchemaName(true);
		String objectName = entity.getObjectName(true);
		DatabaseInterfaceInvoker.execute(
				Priority.HIGH,
				"Uploading Java Resource",
				"Uploading java resource " + objectName,
				batch.getProject(),
				batch.getConnectionId(),
				c -> {
					DatabaseJavaInterface javaInterface = batch.getConnection().getJavaInterface();
					javaInterface.updateJavaResource(schemaName, objectName, resourceBytes, c);
				});
	}

	public static void uploadJavaSource(JavaUploadBatch batch, DBObjectRef<DBJavaEntity> entity, byte[] sourceContent) throws SQLException {
		String schemaName = entity.getSchemaName(true);
		String objectName = entity.getObjectName(true);

		DatabaseInterfaceInvoker.execute(
				Priority.HIGH,
				"Uploading Java Source",
				"Uploading java source " + objectName,
				batch.getProject(),
				batch.getConnectionId(),
				c -> {
					DatabaseJavaInterface javaInterface = batch.getConnection().getJavaInterface();
					javaInterface.replaceJavaSource(schemaName, objectName, sourceContent, c);

				});
	}

	public static void uploadJavaClass(JavaUploadBatch batch, DBObjectRef<DBJavaEntity> entity, byte[] classBytes) throws SQLException {
		String schemaName = entity.getSchemaName(true);
		String objectName = entity.getObjectName(true);

		DatabaseInterfaceInvoker.execute(
				Priority.HIGH,
				"Uploading Java Class",
				"Uploading java class " + objectName,
				batch.getProject(),
				batch.getConnectionId(),
				c -> {
					DatabaseJavaInterface javaInterface = batch.getConnection().getJavaInterface();
					javaInterface.replaceJavaClass(schemaName, objectName, classBytes, c);
				});
	}
}
