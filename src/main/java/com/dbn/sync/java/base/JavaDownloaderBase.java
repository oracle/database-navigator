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

package com.dbn.sync.java.base;

import com.dbn.common.thread.Progress;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBSchema;
import com.dbn.sync.java.JavaDownloader;
import com.dbn.sync.java.JavaDownloaderContext;
import com.dbn.sync.java.JavaDownloaderInput;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public abstract class JavaDownloaderBase implements JavaDownloader {

	@Override
	public final void downloadObject(JavaDownloaderContext context) {
		JavaDownloaderInput input = context.getInput();

		Project project = input.getProject();
		DBJavaClass target = input.getJavaClass();
		List<String> dependentObjects = input.getDependentObjects();

		boolean destinationPrepared = prepareDestination(project, target, input);

		if (destinationPrepared) {
			downloadFileInTarget(input, project, target);
		}

		for (String dependentObject : dependentObjects) {
			dependentObject = dependentObject.replace("/", ".");
			ConnectionHandler connection = context.getDatabaseContext().getConnection();
			if(connection == null) continue;

			String schemaName = dependentObject.split(" \\(")[1].replace(")","");
			String classNameWithPackage =  dependentObject.split(" \\(")[0];

			int lastDotIndex = classNameWithPackage.lastIndexOf('.');
			String packageName = (lastDotIndex != -1) ? classNameWithPackage.substring(0, lastDotIndex) : "";
			String className = (lastDotIndex == -1) ? classNameWithPackage : classNameWithPackage.substring(lastDotIndex + 1);

			input.setPackageName(packageName);
			input.setClassName(className);
			DBSchema schema = connection.getSchema(connection.getSchemaId(schemaName));
			if(schema == null) continue;
			DBJavaClass targetClass = schema.getJavaClass(classNameWithPackage.replace(".", "/"));

			destinationPrepared = prepareDestination(project, targetClass, input);
			if (!destinationPrepared) continue; // do not continue with code download

			downloadFileInTarget(input, project, targetClass);
		}
	}

	private void downloadFileInTarget(JavaDownloaderInput input, Project project, DBJavaClass target){
		Progress.prompt(project, null, true,
				"Downloading file",
				"Downloading java file " + target.getName() + ".java",
				progress -> {

					VirtualFile targetFolder;
					try {
						targetFolder = input.getTargetDirectory().getVirtualFile();
					} catch (ConfigurationException e) {
						conditionallyLog(e);
						return;
					}

					String fileNameWithExt = target.getSimpleName() + ".java";
					createJavaFile(project, target, targetFolder, fileNameWithExt);
				});
	}

	private void createJavaFile(Project project, DBJavaClass target, VirtualFile targetFolder, String fileName) {
		String code;
		try {
			code = SourceCodeManager.getInstance(project).loadSourceFromDatabase(target, DBContentType.CODE).exportContent();
		} catch (SQLException e) {
			Messages.showErrorDialog(project, "File download error",
					"Error downloading " + target.getName() + ".\n" + e.getMessage().trim());
			return;
		}

		AtomicBoolean fileWritten = new AtomicBoolean(false);
		WriteCommandAction.runWriteCommandAction(project, () -> {
			try {
				VirtualFile newFile = targetFolder.createChildData(null, fileName);
				VfsUtil.saveText(newFile, code);
				fileWritten.set(true);
			} catch (IOException e) {
				conditionallyLog(e);
			}
		});

		if(fileWritten.get()) {
			Messages.showInfoDialog(project, "File Download","File downloaded successfully");
		} else {
			Messages.showErrorDialog(project, "File Create error", "Error creating file " + fileName + ".\n");
		}
	}

	/**
	 * Implementations should do all necessary preparations of the code generation destination location.
	 * e.g. create directories, prompt overwrite confirmations aso...
	 *
	 * @param input the input to prepare destination for
	 * @return true if destination is prepared and code download can proceed, false otherwise
	 */
	protected abstract boolean prepareDestination(Project project, DBJavaClass targetClass, JavaDownloaderInput input);
}
