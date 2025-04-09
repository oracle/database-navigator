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
import com.dbn.common.message.Message;
import com.dbn.common.message.MessageType;
import com.dbn.common.util.Messages;
import com.dbn.connection.context.action.AbstractFolderContextAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@BackgroundUpdate
public class JavaObjectUploadAction extends AbstractFolderContextAction {
	@Override
	protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
		VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);

		if(file == null) return;

		if (isPackage(file)) {
			List<String> javaFiles = new ArrayList<>();
			collectJavaFilesAndPackages(file, javaFiles);

			String message = "Java Files:\n" + String.join("\n", javaFiles);

			Messages.showMessageDialog(project, new Message(MessageType.WARNING, message));
		} else {
			Messages.showMessageDialog(project, new Message(MessageType.WARNING, "Right-click action triggered on: " + file.getName()));
		}
	}

	private void collectJavaFilesAndPackages(VirtualFile directory, List<String> javaFiles) {
		if (!directory.isDirectory()) return;

		for (VirtualFile file : directory.getChildren()) {
			if (file.isDirectory()) {
				collectJavaFilesAndPackages(file, javaFiles);
			} else {
				javaFiles.add(directory.getCanonicalPath() + "/" + file.getName());
			}
		}
	}

	private boolean isAvailableFor(VirtualFile virtualFile) {
		return virtualFile != null && virtualFile.getExtension() != null && virtualFile.getExtension().equals("java");
	}

	private boolean isPackage(VirtualFile virtualFile) {
		return virtualFile != null && virtualFile.isDirectory();
	}

	@Override
	protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
		Presentation presentation = e.getPresentation();
		VirtualFile file = Lookups.getVirtualFile(e);
		presentation.setVisible(isAvailableFor(file) || isPackage(file));
		String text = "Upload Class";
		if(isPackage(file)){
			text = "Upload Package";
		}

		presentation.setText(text);
	}
}
