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
import com.dbn.common.event.ProjectEvents;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.object.event.ObjectChangeListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import lombok.SneakyThrows;

import java.util.List;

import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.type.DBObjectType.JAVA_CLASS;
import static com.dbn.object.type.DBObjectType.JAVA_RESOURCE;
import static com.dbn.sync.java.upload.impl.JavaArchiveUploader.uploadJavaArchive;
import static com.dbn.sync.java.upload.impl.JavaClassUploader.uploadJavaSource;
import static com.dbn.sync.java.upload.impl.JavaResourceUploader.uploadJavaResource;

public final class JavaUploaderProcessor extends BatchProcessorBase<JavaUploadTask, JavaUploadInput, JavaUploadBatch> {
	public static final JavaUploaderProcessor INSTANCE = new JavaUploaderProcessor();

	private JavaUploaderProcessor() {
		super("JAVA_UPLOADER");
	}

	@Override
	@SneakyThrows
	public void processTask(JavaUploadBatch batch, JavaUploadTask task) {
		if (task.isArchive()) {
			uploadJavaArchive(batch, task.getFile().getPath());
		} else {
			byte[] content = VfsUtilCore.loadBytes(task.getFile());
			if (task.isJavaClass()) {
				uploadJavaSource(batch, task.getJavaClassName(), content);
			} else {
				uploadJavaResource(batch, task.getTargetEntityName(), content);
			}
		}
	}

	@Override
	protected void postProcessBatch(JavaUploadBatch batch) {
		// notify listeners
		Project project = batch.getProject();
		JavaUploadInput input = batch.getInput();
		ConnectionId connectionId = nn(input.getTargetConnectionId());
		SchemaId schemaId = nn(input.getTargetSchemaId());

		List<JavaUploadTask> completedTasks = batch.getCompletedTasks();
		boolean refreshJavaClasses = completedTasks.stream().anyMatch(t -> t.getTargetEntityType() == JAVA_CLASS);
		boolean refreshJavaResources = completedTasks.stream().anyMatch(t -> t.getTargetEntityType() == JAVA_RESOURCE);

		if (refreshJavaClasses) ProjectEvents.notify(project, ObjectChangeListener.TOPIC, l -> l.objectsChanged(connectionId, schemaId, JAVA_CLASS, CREATE));
		if (refreshJavaResources) ProjectEvents.notify(project, ObjectChangeListener.TOPIC, l -> l.objectsChanged(connectionId, schemaId, JAVA_RESOURCE, CREATE));
	}
}
