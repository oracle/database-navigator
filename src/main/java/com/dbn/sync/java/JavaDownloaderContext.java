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

package com.dbn.sync.java;

import com.dbn.common.message.Message;
import com.dbn.common.message.MessageBundle;
import com.dbn.common.message.MessageCollector;
import com.dbn.common.message.MessageType;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.routine.ThrowableCallable;
import com.dbn.common.routine.ThrowableRunnable;
import com.dbn.connection.context.DatabaseContext;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
public class JavaDownloaderContext {
	private final WeakRef<DatabaseContext> databaseContext;
	private final JavaDownloaderInput input;
	private MessageCollector messages = new MessageBundle();

	public JavaDownloaderContext(JavaDownloaderInput input) {
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
}