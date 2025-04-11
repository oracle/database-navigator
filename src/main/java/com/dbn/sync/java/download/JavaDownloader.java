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

import com.dbn.editor.DBContentType;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.object.DBJavaClass;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import lombok.SneakyThrows;

import java.util.List;

import static com.dbn.common.load.ProgressMonitor.setProgressDetail;
import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;


public final class JavaDownloader extends JavaDownloaderBase {
	public static final JavaDownloader INSTANCE = new JavaDownloader();

	private JavaDownloader() {}

	public void downloadJavaClasses(JavaDownloadContext context) {
		prepareDestinationFolders(context);
		if (context.hasErrors()) return;


		JavaDownloadInput input = context.getInput();
		List<JavaDownloadElement> downloadElements = input.getSelectedDownloadElements();
		for (JavaDownloadElement downloadElement : downloadElements) {
			context.handled(() -> downloadJavaClass(context, downloadElement));
		}
	}

	@SneakyThrows
	private void downloadJavaClass(JavaDownloadContext context, JavaDownloadElement downloadElement) {
		String className = downloadElement.getJavaClassName();
		setProgressDetail("Loading sources of \"" + className + "\"");
		Project project = context.getProject();
		SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);

		DBJavaClass javaClass = downloadElement.getJavaClass();
		SourceCodeContent content = sourceCodeManager.loadSourceFromDatabase(javaClass, DBContentType.CODE);
		String sourceCode = content.exportContent();

		setProgressDetail("Writing project class \"" + className + "\"");
		context.handled(() -> writeJavaFile(context, downloadElement, sourceCode));
	}

	@SneakyThrows
	private static void writeJavaFile(JavaDownloadContext context, JavaDownloadElement downloadElement, String sourceCode) {
		JavaDownloadInput input = context.getInput();
		DBJavaClass javaClass = downloadElement.getJavaClass();

		String javaFileName = downloadElement.getJavaFileName();
		String packageName = javaClass.getPackageName();

		PsiDirectory rootDirectory = input.findContentRootDirectory();
		PsiDirectory packageDirectory = input.findPackageDirectory(rootDirectory, packageName);;

		VirtualFile targetFolder = packageDirectory.getVirtualFile();
		runWriteCommandAction(context.getProject(), () -> context.handled(() -> writeJavaFile(targetFolder, javaFileName, sourceCode)));
	}

	@SneakyThrows
	private static void writeJavaFile(VirtualFile folder, String fileName, String sourceCode) {
		VirtualFile javaFile = folder.findChild(fileName);
		if (javaFile == null) {
			javaFile = folder.createChildData(null, fileName);
		}
		VfsUtil.saveText(javaFile, sourceCode);
	}
}
