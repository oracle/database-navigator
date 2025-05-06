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
import com.dbn.framework.batch.BatchMessageProducer;
import com.dbn.framework.batch.impl.BatchContextBase;
import com.intellij.openapi.ui.DialogWrapper;
import lombok.Getter;
import lombok.Setter;

import javax.swing.Action;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class JavaUploadContext extends BatchContextBase<JavaUploadElement, JavaUploadInput, JavaUploadTask> {
	private final List<String> classesToCompile = new ArrayList<>();

	public JavaUploadContext(JavaUploadInput input) {
		super(input);
	}

	@Override
	public Object getContextObject() {
		return getInput().getTargetConnection();
	}

	public List<String> getUploadedFiles() {
		return Lists.convert(getTasks(), t -> {
			JavaUploadElement uploadElement = t.getElement();
			return uploadElement.getFile().getPath();
		});
	}

	@Override
	protected BatchMessageProducer createMessageProducer() {
		return new JavaUploadMessageProducer(this);
	}

	@Override
	protected Action[] createErrorResolutionActions() {
		if (isComplete()) return null; // do not overwrite the default "Close" action
		return new Action[] {
				createCancelAction(),
				createContinueAction()};
	}

	private Action createCancelAction() {
		return createAction("Cancel", dialog -> {
			dialog.close(DialogWrapper.CANCEL_EXIT_CODE);
		});
	}

	private Action createContinueAction() {
		return createAction("Continue Upload", dialog -> {
			dialog.close(DialogWrapper.OK_EXIT_CODE);
		});
	}

	@Override
	public JavaUploadTask createBatchTask(JavaUploadElement element) {
		JavaUploadTask uploadTask = new JavaUploadTask(element);
		addTask(uploadTask);
		return uploadTask;
	}

}