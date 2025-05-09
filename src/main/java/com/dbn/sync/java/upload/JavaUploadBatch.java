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
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Lists.convert;

@Getter
@Setter
public class JavaUploadBatch extends BatchBase<JavaUploadTask, JavaUploadInput> {
	private final List<String> classesToCompile = new ArrayList<>();

	public JavaUploadBatch(JavaUploadInput input) {
		super(input, JavaUploader.INSTANCE);
	}

	@Override
	public String getProcessTitle() {
		return "Java Upload Process";
	}

	@Override
	public Object getContextObject() {
		return getInput().getTargetConnection();
	}

	public List<String> getUploadedFiles() {
		return convert(getTasks(), t -> t.getFile().getPath());
	}
}