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

import com.dbn.common.message.MessageType;
import com.dbn.common.message.TaggedMessage;
import com.dbn.framework.batch.impl.BatchProcessorBase;
import com.intellij.openapi.vfs.VfsUtilCore;
import lombok.SneakyThrows;

import java.util.List;

import static com.dbn.common.load.ProgressMonitor.setProgressDetail;
import static com.dbn.sync.java.upload.impl.JavaArchiveUploader.uploadJavaArchive;
import static com.dbn.sync.java.upload.impl.JavaClassUploader.uploadJavaSource;

public final class JavaUploader extends BatchProcessorBase<JavaUploadContext> {
	public static final JavaUploader INSTANCE = new JavaUploader();

	private JavaUploader() {}

	/**
	 * Uploader tasks preparation method. Queues all tasks to be executed in context.
	 * @param context the {@link JavaUploadContext} to be prepared
	 */
	public void prepareBatch(JavaUploadContext context) {
		// schedule upload tasks in context
		JavaUploadInput input = context.getInput();
		List<JavaUploadElement> elements = input.getSelectedElements();
		for (JavaUploadElement element : elements) {
			Object subject = element.getSubject();
			context.queueTask(subject, () -> performElementUpload(context, element));
		}
	}

	@SneakyThrows
	private static void performElementUpload(JavaUploadContext context, JavaUploadElement element) {
		String elementName = element.getName();
		setProgressDetail("Uploading sources of \"" + elementName + "\"");

		// create upload task
		JavaUploadTask uploadTask = context.createBatchTask(element);

		if (element.isArchive()) {
			uploadJavaArchive(context, element.getFile().getPath());
		} else {
			byte[] content = VfsUtilCore.loadBytes(element.getFile());
			if (element.isJavaClass()) {
				uploadJavaSource(context, element.getJavaClassName(), content);
			} else {
				// TODO upload resources
				context.addMessage(new TaggedMessage<>(MessageType.WARNING, "Uploading resources is not supported yet", element.getSubject()));
			}
		}
	}
}
