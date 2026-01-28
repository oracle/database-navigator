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

package com.dbn.sync.java.upload;

import com.dbn.batch.impl.BatchTaskBase;
import com.dbn.common.file.FileTypes;
import com.dbn.common.thread.Read;
import com.dbn.common.util.Strings;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.object.DBJavaEntity;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.ide.highlighter.ArchiveFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.common.file.util.ProjectFiles.getProjectRelativePath;
import static com.dbn.common.util.Commons.nvl;

@Getter
@Setter
public class JavaUploadTask extends BatchTaskBase {
	private final JavaUploadBatch batch;

	private final VirtualFile archiveFile;
	private final VirtualFile file;
	private final PsiFile psiFile;
	private final String name;
	private final Icon icon;

	private DBObjectRef<DBJavaEntity> databaseEntity;

	public JavaUploadTask(JavaUploadBatch batch, VirtualFile file) {
		this.batch = batch;
		Project project = batch.getProject();

		this.file = file;
		this.archiveFile = initArchiveFile();
		this.psiFile = initPsiFile(project, file);
		this.name = initName(project);
		this.icon = initIcon();
		setSelected(true);
	}

	private static @Nullable PsiFile initPsiFile(Project project, VirtualFile file) {
		return PsiUtil.getPsiFile(project, file);
	}

	private VirtualFile initArchiveFile() {
		VirtualFileSystem fileSystem = file.getFileSystem();
		if (fileSystem instanceof JarFileSystem jarFileSystem) {
            return jarFileSystem.getRootByEntry(file);
		}
		return null;
	}

	@SneakyThrows
	public byte[] getFileContent() {
		return file.contentsToByteArray();
	}

	@Override
	public Object getSubject() {
		return nvl(psiFile, file);
	}

	private String initName(Project project) {
		return getProjectRelativePath(project, file);
	}

	private Icon initIcon() {
		return psiFile == null ? file.getFileType().getIcon() : Read.call(psiFile, f -> f.getIcon(0));
	}

	private DBObjectRef<DBJavaEntity> initDatabaseEntity() {
		if (isJavaLibrary()) return null;

		String filePath = archiveFile == null ?
				getProjectRelativePath(getProject(), file) :
				VfsUtilCore.getRelativePath(file, archiveFile);
		filePath = nvl(filePath, file.getPath());

		boolean isJavaObject = isJavaSource() || isJavaClass();

		DBObjectType objectType = isJavaObject ? DBObjectType.JAVA_CLASS : DBObjectType.JAVA_RESOURCE;
		String objectName = isJavaObject ? FileUtil.getNameWithoutExtension(filePath) : filePath;
		objectName = objectName.replace("\\", "/");

		DBSchema schema = getBatch().getInput().getTargetSchema();
		return new DBObjectRef<>(schema == null ? null : schema.ref(), objectType, objectName);
	}


	public synchronized DBObjectRef<DBJavaEntity> getDatabaseEntity() {
		if (databaseEntity == null) {
			databaseEntity = initDatabaseEntity();
		}
		return databaseEntity;
	}

	private Project getProject() {
		return getBatch().getProject();
	}

	public boolean isJavaLibrary() {
		return file.getFileType() == ArchiveFileType.INSTANCE;
	}

	public boolean isJavaSource() {
		return file.getFileType() == FileTypes.getJavaFileType();
	}

	public boolean isJavaClass() {
		return file.getFileType() == FileTypes.getClassFileType();
	}

	public boolean isJavaResource() {
		if (isJavaLibrary()) return false;
		if (isJavaSource()) return false;
		if (isJavaClass()) return false;
		return true;
	}

	public String getJavaClassName() {
		if (!isJavaSource() && !isJavaClass()) return null;

		String packageName = null;
		if (psiFile instanceof PsiClassOwner classOwner) {
            packageName = Read.call(classOwner, o-> o.getPackageName());
		}

		String packagePrefix = Strings.isEmpty(packageName) ? "" : packageName + ".";
		return packagePrefix + file.getNameWithoutExtension();
	}
}
