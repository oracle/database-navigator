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
import com.intellij.openapi.vfs.VfsUtilCore;
import lombok.SneakyThrows;

import static com.dbn.common.load.ProgressMonitor.setProgressDetail;
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
		String taskName = task.getName();
		setProgressDetail("Uploading sources of \"" + taskName + "\"");


		if (task.isArchive()) {
			uploadJavaArchive(batch, task.getFile().getPath());
		} else {
			byte[] content = VfsUtilCore.loadBytes(task.getFile());
			if (task.isJavaClass()) {
				uploadJavaSource(batch, task.getJavaClassName(), content);
			} else {
				uploadJavaResource(batch, taskName, content);
			}
		}
	}
}
