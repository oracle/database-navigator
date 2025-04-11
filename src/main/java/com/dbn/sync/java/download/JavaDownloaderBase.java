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

import com.dbn.common.thread.Read;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;

import java.io.File;
import java.util.Set;

import static com.dbn.common.options.Configs.fail;
import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;

abstract class JavaDownloaderBase {
	protected void prepareDestinationFolders(JavaDownloadContext context) {
		PsiDirectory rootDirectory = context.handled(() -> prepareRootDirectory(context));
		if (rootDirectory == null) return;

		context.setTargetRootDirectory(rootDirectory.getVirtualFile());

		JavaDownloadInput input = context.getInput();
		Set<JavaPackageNode> packageNodes = input.getTargetPackages();
		for (JavaPackageNode packageNode : packageNodes) {
			context.handled(() -> prepareChildDirectory(context, packageNode, rootDirectory));
		}
	}

	private PsiDirectory prepareRootDirectory(JavaDownloadContext context) throws Exception{
		JavaDownloadInput input = context.getInput();
		Module module = input.findModule();
		VirtualFile file = input.findContentRoot(module);
		return input.findContentRootDirectory(file);
	}

	private void prepareChildDirectory(JavaDownloadContext context, JavaPackageNode packageNode, PsiDirectory parentDirectory) {
		String directoryName = packageNode.getName();
		PsiDirectory subdirectory = context.handled(() -> ensureChildDirectory(parentDirectory, directoryName));
		if (subdirectory == null) return;

		for (JavaPackageNode child : packageNode.getChildren()) {
            context.handled(() -> prepareChildDirectory(context, child, subdirectory));
        }
    }

	private static PsiDirectory ensureChildDirectory(PsiDirectory parentDirectory, String directoryName) throws Exception{
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
