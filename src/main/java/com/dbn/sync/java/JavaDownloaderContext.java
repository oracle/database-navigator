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

import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.common.outcome.OutcomeHandlers;
import com.dbn.common.outcome.OutcomeHandlersImpl;
import com.dbn.common.outcome.OutcomeType;
import com.dbn.common.ref.WeakRef;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.sync.java.impl.JavaDownloaderImpl;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
public class JavaDownloaderContext {
	private final JavaDownloader downloader;
	private final WeakRef<DatabaseContext> databaseContext;
	private final OutcomeHandlers outcomeHandlers = new OutcomeHandlersImpl();
	private JavaDownloaderInput input;

	public JavaDownloaderContext(DatabaseContext databaseContext) {
		this.downloader = new JavaDownloaderImpl();
		this.databaseContext = WeakRef.of(databaseContext);
	}

	public void addOutcomeHandler(OutcomeType outcomeType, OutcomeHandler handler) {
		if (handler == null) return;
		outcomeHandlers.addHandler(outcomeType, handler);
	}

	@NotNull
	public <T extends DatabaseContext> T getDatabaseContext() {
		return cast(WeakRef.ensure(databaseContext));
	}

	@NotNull
	public Project getProject() {
		return nd(getDatabaseContext().getProject());
	}

}