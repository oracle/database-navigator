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

import com.dbn.batch.impl.BatchProcessorBase;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.execution.compiler.DatabaseCompilerManager;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaEntity;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import lombok.SneakyThrows;

import java.util.List;

import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.type.DBObjectType.JAVA_CLASS;
import static com.dbn.object.type.DBObjectType.JAVA_RESOURCE;
import static com.dbn.sync.java.upload.JavaUploader.uploadJavaArchive;
import static com.dbn.sync.java.upload.JavaUploader.uploadJavaClass;
import static com.dbn.sync.java.upload.JavaUploader.uploadJavaResource;
import static com.dbn.sync.java.upload.JavaUploader.uploadJavaSource;

public final class JavaUploadProcessor extends BatchProcessorBase<JavaUploadTask, JavaUploadInput, JavaUploadBatch> {
	public static final JavaUploadProcessor INSTANCE = new JavaUploadProcessor();

	private JavaUploadProcessor() {
		super("JAVA_UPLOADER");
	}

	@Override
	@SneakyThrows
	public void processTask(JavaUploadBatch batch, JavaUploadTask task) {
		if (task.isJavaLibrary()) {
			uploadJavaArchive(batch, task.getFile());
		} else {
			DBObjectRef<DBJavaEntity> entity = task.getDatabaseEntity();
			byte[] content = task.getFileContent();

			if (task.isJavaSource()) {
				uploadJavaSource(batch, entity, content);

			} else if (task.isJavaClass()){
				uploadJavaClass(batch, entity, content);

			} else {
				uploadJavaResource(batch, entity, content);
			}
		}
    }

	@Override
	protected void postProcessBatch(JavaUploadBatch batch) {
		// notify listeners
		refreshDatabaseBrowser(batch);
		compileUploadedClasses(batch);
	}

	private static void refreshDatabaseBrowser(JavaUploadBatch batch) {
		JavaUploadInput input = batch.getInput();
		ConnectionId connectionId = nn(input.getTargetConnectionId());
		SchemaId schemaId = nn(input.getTargetSchemaId());

		List<DBObjectRef<DBJavaEntity>> javaClasses = batch.getUploadedEntities(JAVA_CLASS);
		List<DBObjectRef<DBJavaEntity>> javaResources = batch.getUploadedEntities(JAVA_RESOURCE);

		if (!javaClasses.isEmpty()) {
            ObjectChangeEvent.notify(CREATE, JAVA_CLASS, connectionId, schemaId);
        }

		if (!javaResources.isEmpty()) {
            ObjectChangeEvent.notify(CREATE, JAVA_RESOURCE, connectionId, schemaId);
        }
	}

	private void compileUploadedClasses(JavaUploadBatch batch) {
		Project project = batch.getProject();
		DatabaseCompilerManager compilerManager = DatabaseCompilerManager.getInstance(project);

		List<DBObjectRef<DBJavaClass>> javaClasses = batch.getUploadedEntities(JAVA_CLASS);
		compilerManager.compileJavaClasses(batch.getConnection(), javaClasses);
	}
}
