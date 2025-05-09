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

import com.dbn.batch.impl.BatchProcessorBase;
import com.dbn.common.thread.Read;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import lombok.SneakyThrows;

import java.io.File;
import java.util.Set;

import static com.dbn.common.options.Configs.fail;
import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;

abstract class JavaDownloaderBase extends BatchProcessorBase<JavaDownloadTask, JavaDownloadInput, JavaDownloadBatch> {

	public JavaDownloaderBase() {
		super("JAVA_DOWNLOADER");
	}

	@SneakyThrows
	protected static void prepareDestinationFolders(JavaDownloadBatch batch) {
		prepareRootDirectory(batch);

		JavaDownloadInput input = batch.getInput();
		Set<JavaPackageNode> packageNodes = input.getTargetPackages();
		for (JavaPackageNode packageNode : packageNodes) {
			PsiDirectory rootDirectory = batch.getTargetRootDirectory();
			prepareChildDirectory(packageNode, rootDirectory);
		}
	}

	@SneakyThrows
	private static void prepareRootDirectory(JavaDownloadBatch batch) {
		JavaDownloadInput input = batch.getInput();
		Module module = input.findModule();
		VirtualFile file = input.findContentRoot(module);
		PsiDirectory rootDirectory = input.findContentRootDirectory(file);

		batch.setTargetRootDirectory(rootDirectory);
	}

	@SneakyThrows
	private static void prepareChildDirectory(JavaPackageNode packageNode, PsiDirectory parentDirectory) {
		String directoryName = packageNode.getName();
		PsiDirectory subdirectory = ensureChildDirectory(parentDirectory, directoryName);

		for (JavaPackageNode child : packageNode.getChildren()) {
			prepareChildDirectory(child, subdirectory);
		}
    }

	@SneakyThrows
	private static PsiDirectory ensureChildDirectory(PsiDirectory parentDirectory, String directoryName) {
		PsiDirectory childDirectory = Read.call(() -> parentDirectory.findSubdirectory(directoryName));
		if (childDirectory != null) return childDirectory;

		Project project = parentDirectory.getProject();
		childDirectory = runWriteCommandAction(project, (Computable<PsiDirectory>) () -> parentDirectory.createSubdirectory(directoryName));

		if (childDirectory == null) {
			String directoryPath = parentDirectory.getName() + File.pathSeparator + directoryName;
			fail("Could not create directory " + directoryPath);
		}
		return childDirectory;
	}
}
