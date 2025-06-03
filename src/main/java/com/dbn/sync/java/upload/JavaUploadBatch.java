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

import com.dbn.batch.impl.BatchBase;
import com.dbn.object.DBJavaEntity;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
public class JavaUploadBatch extends BatchBase<JavaUploadTask, JavaUploadInput> {
	private final List<String> classesToCompile = new ArrayList<>();

	public JavaUploadBatch(JavaUploadInput input) {
		super(input);
	}

	@Override
	protected JavaUploadMessenger createMessenger() {
		return JavaUploadMessenger.INSTANCE;
	}

	@Override
	protected JavaUploadProcessor createProcessor() {
		return JavaUploadProcessor.INSTANCE;
	}

	@Override
	public Object getContextObject() {
		return getInput().getTargetConnection();
	}

	public <T extends DBJavaEntity> List<DBObjectRef<T>> getUploadedEntities(@Nullable DBObjectType objectType) {
		return cast(getCompletedTasks().
				stream().
				map(e -> e.getDatabaseEntity()).
				filter(e -> e != null).
				filter(e -> objectType == null || e.getObjectType() == objectType).
				collect(Collectors.toList()));
	}

	@Override
	public void showResults() {
		JavaUploadManager uploadManager = JavaUploadManager.getInstance(getProject());
		uploadManager.openBatchResult(this);
	}

	public void createTask(VirtualFile file) {
		JavaUploadTask task = new JavaUploadTask(this, file);
		if (task.isJavaLibrary()) {
			// do not select a library-task by default unless the upload was invocated directly on the library file
			task.setSelected(Objects.equals(file, getInput().getRootFile()));
		}

		queueTask(task);
	}
}