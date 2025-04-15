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

import com.dbn.common.message.Message;
import com.dbn.common.message.MessageBundle;
import com.dbn.common.message.MessageCollector;
import com.dbn.common.message.MessageType;
import com.dbn.common.routine.ThrowableRunnable;
import com.dbn.common.util.Lists;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class JavaUploadContext {
	private final MessageCollector messages = new MessageBundle();
	private JavaUploadInput input;
	private final List<JavaUploadTask> uploadTasks = new ArrayList<>();

	public JavaUploadContext(JavaUploadInput input) {
		this.input = input;
	}

	@NotNull
	public Project getProject() {
		return input.getProject();
	}

	public void handled(ThrowableRunnable<Exception> runnable) {
		try {
			runnable.run();
		} catch (Throwable e) {
			handle(e);
		}
	}

	private void handle(Throwable e) {
		messages.addMessage(new Message(MessageType.ERROR, e.getMessage()));
	}

	public boolean hasErrors() {
		return messages.hasErrors();
	}

	public List<String> getUploadedFiles() {
		return Lists.convert(uploadTasks, t -> {
			if(t.getInput().getJarPath() != null){
				return t.getInput().getJarPath();
			} else {
				return t.getInput().getJavaClassName();
			}
		});
	}

	public JavaUploadTask createUploadTask(JavaUploadElement uploadElement) {
		JavaUploadTask uploadTask = new JavaUploadTask(uploadElement);
		uploadTasks.add(uploadTask);
		return uploadTask;
	}
}