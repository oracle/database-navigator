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

package com.dbn.sync.java.upload.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.sync.java.upload.JavaUploadContext;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

public class JavaUploadResultDialog extends DBNDialog<JavaUploadResultForm> {
	private final JavaUploadContext context;

	public JavaUploadResultDialog(Project project, JavaUploadContext context) {
		super(project, "Java Upload Result", false);
		//this.setDefaultSize(380, 420);
		this.setModal(true);
		this.setAutoSize(true);
		this.context = context;
		renameAction(getCancelAction(), "Close");
		init();
	}

	@Override
	protected Action @NotNull [] createActions() {
		return new Action[]{getCancelAction()};
	}

	@Override
	protected @NotNull JavaUploadResultForm createForm() {
		return new JavaUploadResultForm(this, context);
	}
}
