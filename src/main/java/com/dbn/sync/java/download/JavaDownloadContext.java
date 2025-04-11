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

import com.dbn.common.message.Message;
import com.dbn.common.message.MessageBundle;
import com.dbn.common.message.MessageCollector;
import com.dbn.common.message.MessageType;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.routine.ThrowableCallable;
import com.dbn.common.routine.ThrowableRunnable;
import com.dbn.common.util.Lists;
import com.dbn.connection.context.DatabaseContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
public class JavaDownloadContext {
	private final WeakRef<DatabaseContext> databaseContext;
	private final JavaDownloadInput input;
	private final MessageCollector messages = new MessageBundle();
	private final List<JavaDownloadTask> downloadTasks = new ArrayList<>();
	private VirtualFile targetRootDirectory;

	public JavaDownloadContext(JavaDownloadInput input) {
		this.input = input;
		this.databaseContext = WeakRef.of(input.getDatabaseContext());
	}

	@NotNull
	public <T extends DatabaseContext> T getDatabaseContext() {
		return cast(WeakRef.ensure(databaseContext));
	}

	@NotNull
	public Project getProject() {
		return nd(getDatabaseContext().getProject());
	}

	public void handled(ThrowableRunnable<Exception> runnable) {
		try {
			runnable.run();
		} catch (Throwable e) {
			handle(e);
		}
	}

	@Nullable
	public <T> T handled(ThrowableCallable<T, Exception> runnable) {
		try {
			return runnable.call();
		} catch (Throwable e) {
			handle(e);
			return null;
		}
	}

	private void handle(Throwable e) {
		messages.addMessage(new Message(MessageType.ERROR, e.getMessage()));
	}

	public boolean hasErrors() {
		return messages.hasErrors();
	}

	public List<VirtualFile> getDownloadedFiles() {
		return Lists.convert(downloadTasks, t -> t.getTargetFile());
	}

	public JavaDownloadTask createDownloadTask(JavaDownloadElement downloadElement) {
		JavaDownloadTask downloadTask = new JavaDownloadTask(downloadElement);
		downloadTasks.add(downloadTask);
		return downloadTask;
/*

		Project project = getProject();
		PsiFile psiFile = PsiUtil.getPsiFile(project, file);
		if (psiFile != null) {
			downloadedPsiFiles.add(psiFile);
		}
*/
	}
}