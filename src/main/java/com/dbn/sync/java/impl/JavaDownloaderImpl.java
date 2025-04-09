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

package com.dbn.sync.java.impl;

import com.dbn.common.thread.Write;
import com.dbn.common.util.Messages;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.generator.code.java.JavaCodeGeneratorInput;
import com.dbn.object.DBJavaClass;
import com.dbn.sync.java.JavaDownloaderInput;
import com.dbn.sync.java.base.JavaDownloaderBase;
import com.dbn.sync.java.ui.JavaDownloaderInputDialog;
import com.dbn.sync.java.ui.JavaDownloaderInputForm;
import com.dbn.sync.java.ui.JavaObjectDownloadInputForm;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import lombok.SneakyThrows;

import static com.dbn.common.options.Configs.fail;
import static com.dbn.common.util.Strings.isEmpty;

public class JavaDownloaderImpl<I extends JavaDownloaderInput> extends JavaDownloaderBase<I> {
	@Override
	protected boolean prepareDestination(Project project, DBJavaClass targetClass, I input) {
		prepareDestinationFolder(input);
		return handleDestinationOverwrite(project, targetClass, input);
	}

	@Override
	public JavaDownloaderInput createInput(DatabaseContext databaseContext) {
		JavaDownloaderInput input = new JavaDownloaderInput(databaseContext);
		DBJavaClass targetClass = (DBJavaClass) databaseContext;

		int lastDotIndex = targetClass.getCanonicalName().lastIndexOf('.');
		String packageName = (lastDotIndex != -1) ? targetClass.getCanonicalName().substring(0, lastDotIndex) : "";

		String className = targetClass.getSimpleName();

		input.setPackageName(packageName);
		input.setClassName(className);
		return input;
	}

	@Override
	public JavaDownloaderInputForm<I> createInputForm(JavaDownloaderInputDialog dialog, I input) {
		return new JavaObjectDownloadInputForm<>(dialog, input) ;
	}

	/**
	 * Prepares the destination directory structure for a specified input, creating the necessary
	 * package directories if they do not already exist.
	 * (moved from {@link JavaCodeGeneratorInput})
	 *
	 * @param input the input object containing generator destination information, such as the module,
	 *              content root, and package name required to determine and create the target directories
	 */
	@SneakyThrows
	private void prepareDestinationFolder(I input) {
		Module module = input.findModule();
		VirtualFile file = input.findContentRoot(module);
		PsiDirectory directory = input.findContentRootDirectory(file);

		String packageName = input.getPackageName();
		if (isEmpty(packageName)) return;

		String[] packageTokens = packageName.trim().split("\\.");
		for (String packageToken : packageTokens) {
			PsiDirectory subdirectory = directory.findSubdirectory(packageToken);
			if (subdirectory == null)  {
				directory.createSubdirectory(packageToken);
				subdirectory = directory.findSubdirectory(packageToken);
				if (subdirectory == null) fail("Cannot create package directory " + packageToken);
			}
			directory = subdirectory;
		}
	}

	@SneakyThrows
	private boolean handleDestinationOverwrite(Project project, DBJavaClass target, I input) {
		String className = target.getSimpleName();
		String fileName = className + ".java";

		PsiDirectory directory = input.getTargetDirectory();
		PsiFile file = directory.findFile(fileName);
		if (file == null) return true;

		int overwrite = Messages.showConfirmationDialog(
				project,
				"Overwrite Class",
				"A class named \"" + className + "\" already exists in the target location. Do you want to overwrite it?",
				Messages.OPTIONS_YES_NO, 0);

		if (overwrite == 0) {
			Write.run(() -> file.delete());
			return true;
		}

		return false;
	}
}
