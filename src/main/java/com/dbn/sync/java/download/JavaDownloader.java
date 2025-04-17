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
		List<JavaDownloadElement> elements = input.getSelectedElements();
		for (JavaDownloadElement element : elements) {
			context.handled(() -> downloadJavaClass(context, element));
		}
	}

	@SneakyThrows
	private void downloadJavaClass(JavaDownloadContext context, JavaDownloadElement element) {
		String className = element.getJavaClassName();
		setProgressDetail("Loading sources of \"" + className + "\"");

		// create download task
		JavaDownloadTask downloadTask = context.createDownloadTask(element);

		// load source code content
		Project project = context.getProject();
		DBJavaClass javaClass = element.getJavaClass();
		SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);
		SourceCodeContent content = sourceCodeManager.loadSourceFromDatabase(javaClass, DBContentType.CODE);

		String sourceCode = content.exportContent();
		downloadTask.setContent(sourceCode);

		setProgressDetail("Writing project class \"" + className + "\"");
		context.handled(() -> writeJavaFile(context, downloadTask));
	}

	@SneakyThrows
	private static void writeJavaFile(JavaDownloadContext context, JavaDownloadTask task) {
		DBJavaClass javaClass = task.getJavaClass();
		String packageName = javaClass.getPackageName();

		JavaDownloadInput input = context.getInput();
		PsiDirectory rootDirectory = input.findContentRootDirectory();
		PsiDirectory targetDirectory = input.findPackageDirectory(rootDirectory, packageName);
		task.setTargetFolder(targetDirectory.getVirtualFile());

		Project project = context.getProject();
		runWriteCommandAction(project, () -> context.handled(() -> writeJavaFile(task)));
	}

	@SneakyThrows
	private static void writeJavaFile(JavaDownloadTask downloadTask) {
		String fileName = downloadTask.getJavaFileName();

		VirtualFile targetFolder = downloadTask.getTargetFolder();
		VirtualFile targetFile = targetFolder.findChild(fileName);
		if (targetFile == null) {
			targetFile = targetFolder.createChildData(null, fileName);
		}
		downloadTask.setTargetFile(targetFile);

		String sourceCode = downloadTask.getContent();
		VfsUtil.saveText(targetFile, sourceCode);
	}
}
