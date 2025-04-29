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

import com.dbn.common.util.Lists;
import com.dbn.sync.common.impl.SyncContextBase;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class JavaUploadContext extends SyncContextBase<JavaUploadInput, JavaUploadTask> {

	public JavaUploadContext(JavaUploadInput input) {
		super(input);
	}

	private List<List<String>> errors = new ArrayList<>();

	public List<String> getUploadedFiles() {
		return Lists.convert(getTasks(), t -> {
			JavaUploadElement uploadElement = t.getElement();
			return uploadElement.getFile().getPath();
		});
	}

	public JavaUploadTask createUploadTask(JavaUploadElement uploadElement) {
		JavaUploadTask uploadTask = new JavaUploadTask(uploadElement);
		addTask(uploadTask);
		return uploadTask;
	}

	public void addError(List<String> error){
		errors.add(error);
	}
}