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

package com.dbn.sync.java.download;

import com.dbn.common.util.Lists;
import com.dbn.framework.batch.BatchProducer;
import com.dbn.framework.batch.impl.BatchContextBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class JavaDownloadContext extends BatchContextBase<JavaDownloadElement, JavaDownloadInput, JavaDownloadTask> {
	private PsiDirectory targetRootDirectory;

	public JavaDownloadContext(JavaDownloadInput input) {
		super(input);
	}

	@Override
	public Object getContextObject() {
		return getInput().getSourceObject();
	}

	public List<VirtualFile> getDownloadedFiles() {
		return Lists.convert(getTasks(), t -> t.getTargetFile());
	}

	@Override
	protected BatchProducer createMessageProducer() {
		return new JavaDownloadMessageProducer(this);
	}

	@Override
	public JavaDownloadTask createBatchTask(JavaDownloadElement downloadElement) {
		JavaDownloadTask downloadTask = new JavaDownloadTask(downloadElement);
		addTask(downloadTask);
		return downloadTask;
	}
}