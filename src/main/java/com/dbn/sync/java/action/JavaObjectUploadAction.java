/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.sync.java.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.common.icon.Icons;
import com.dbn.connection.context.action.AbstractFolderContextAction;
import com.dbn.sync.java.upload.JavaUploadManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.file.util.ProjectFiles.isProjectSourceFile;
import static com.dbn.common.file.util.VirtualFiles.isArchive;
import static com.dbn.common.util.Java.isIdeSupportAvailable;

@BackgroundUpdate
public class JavaObjectUploadAction extends AbstractFolderContextAction {
	@Override
	protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
		VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
		if(file == null) return;

		JavaUploadManager manager = JavaUploadManager.getInstance(project);
		manager.openCodeUploader(file);
	}

	private boolean isAvailableFor(Project project, VirtualFile file) {
		if (file == null) return false;
		if (file.isDirectory()) return true; // support action on any folder level
		if (isArchive(file)) return true;
		if (!isProjectSourceFile(project, file)) return false;
		if (!isIdeSupportAvailable()) return false;

		return true;
	}

	@Override
	protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
		VirtualFile file = Lookups.getVirtualFile(e);
		boolean visible = isAvailableFor(project, file);


		Presentation presentation = e.getPresentation();
		presentation.setVisible(visible);
		presentation.setText("Upload to Database");
		presentation.setIcon(Icons.ACTION_UPLOAD);
	}
}
