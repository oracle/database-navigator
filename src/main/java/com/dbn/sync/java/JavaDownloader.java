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

import com.dbn.connection.context.DatabaseContext;
import com.dbn.object.DBJavaClass;

public interface JavaDownloader {
	/**
	 * Creates an input for the code generator for a given database context
	 *
	 * @param javaClass the {@link DatabaseContext} to create input for
	 * @return a {@link JavaDownloaderInput}
	 */
	JavaDownloaderInput createInput(DBJavaClass javaClass);

	/**
	 * The main utility of the code generator, accepting a {@link JavaDownloaderContext}.
	 * The context is expected to contain the {@link JavaDownloaderInput}
	 * The outcomes are reported back to the outcome handlers registered to the context (see {@link JavaDownloaderContext#getOutcomeHandlers()})
	 *
	 * @param context the {@link JavaDownloaderContext} that contains all the information necessary for code generation
	 */
	void downloadObject(JavaDownloaderContext context);
}
