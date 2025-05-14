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

import com.dbn.common.util.Messages;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaEntity;
import com.dbn.object.DBJavaResource;
import com.dbn.object.common.DBObject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import lombok.SneakyThrows;

import static com.dbn.common.load.ProgressMonitor.setProgressDetail;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.vfs.DBVirtualFile.EMPTY_CONTENT;
import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;


public final class JavaDownloadProcessor extends JavaDownloaderBase {
	public static final JavaDownloadProcessor INSTANCE = new JavaDownloadProcessor();

	private JavaDownloadProcessor() {}



	/**
	 * Downloader batch preparation method. Queues all tasks to be executed in this batch.
	 * @param batch the {@link JavaDownloadBatch} to be prepared
	 */
	@Override
	protected void prepareBatch(JavaDownloadBatch batch) {
        try {
			// prepare destination folders
            prepareDestinationFolders(batch);
        } catch (Exception e) {
			batch.cancel();
			Project project = batch.getProject();
			Messages.showErrorDialog(project,
					"Download Failed",
					"Failed to prepare destination folders", e);
        }
	}

	@Override
	@SneakyThrows
	public void processTask(JavaDownloadBatch batch, JavaDownloadTask task) {
		String className = task.getEntityName();
		setProgressDetail("Loading sources of \"" + className + "\"");


		// load source code content
		Project project = batch.getProject();
		DBJavaEntity javaEntity = task.getEntity();
		SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);
		SourceCodeContent content = sourceCodeManager.loadSourceFromDatabase(javaEntity, DBContentType.CODE);

		String sourceCode = content.getRawContent();
		task.setContent(sourceCode.getBytes());


		setProgressDetail("Writing project class \"" + className + "\"");
		writeJavaFile(batch, task);
	}

	@SneakyThrows
	private static void writeJavaFile(JavaDownloadBatch batch, JavaDownloadTask task) {
		DBObject object = task.getEntity();
		String packageName = "";
		if (object instanceof DBJavaClass) {
			DBJavaClass javaClass = (DBJavaClass) object;
			packageName = javaClass.getPackageName();
		} else if (object instanceof DBJavaResource) {
			String[] packageNameTokens = task.getEntityPathTokens();
			packageName = String.join(".", packageNameTokens);
		}

		JavaDownloadInput input = batch.getInput();
		PsiDirectory rootDirectory = input.findContentRootDirectory();
		PsiDirectory targetDirectory = input.findPackageDirectory(rootDirectory, packageName);
		task.setTargetFolder(targetDirectory.getVirtualFile());

		Project project = batch.getProject();
		runWriteCommandAction(project, () -> writeJavaFile(task));
	}

	@SneakyThrows
	private static void writeJavaFile(JavaDownloadTask downloadTask) {
		String fileName = downloadTask.getEntityFileName();

		VirtualFile targetFolder = downloadTask.getTargetFolder();
		VirtualFile targetFile = targetFolder.findChild(fileName);
		if (targetFile == null) {
			targetFile = targetFolder.createChildData(null, fileName);
		}
		downloadTask.setTargetFile(targetFile);

		String sourceCode = new String(nvl(downloadTask.getContent(), EMPTY_CONTENT));
		VfsUtil.saveText(targetFile, sourceCode);
	}
}
