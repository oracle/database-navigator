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

import com.dbn.common.file.FileTypes;
import com.dbn.common.thread.Read;
import com.dbn.common.util.Strings;
import com.dbn.framework.batch.impl.BatchElementBase;
import com.dbn.language.common.psi.PsiUtil;
import com.intellij.ide.highlighter.ArchiveFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.swing.Icon;

import static com.dbn.common.file.util.ProjectFiles.getProjectRelativePath;
import static com.dbn.common.util.Commons.nvl;

@Data
@EqualsAndHashCode(callSuper = false)
public class JavaUploadElement extends BatchElementBase {
	private final VirtualFile file;
	private final PsiFile psiFile;
	private final String name;
	private final Icon icon;
	private transient Project project;

	public JavaUploadElement(Project project, VirtualFile file) {
		this.file = file;
		this.psiFile = PsiUtil.getPsiFile(project, file);
		this.project = project;
		this.name = resolveName();
		this.icon = resolveIcon();
		setSelected(file.getFileType() == FileTypes.getJavaFileType());
	}

	@Override
	public Object getSubject() {
		return nvl(psiFile, file);
	}

	private String resolveName() {
		return getProjectRelativePath(getProject(), file);
	}

	private Icon resolveIcon() {
		return psiFile == null ? file.getFileType().getIcon() : Read.call(psiFile, f -> f.getIcon(0));
	}

	public boolean isArchive() {
		return file.getFileType() == ArchiveFileType.INSTANCE;
	}

	public boolean isJavaClass() {
		return file.getFileType() == FileTypes.getJavaFileType();
	}

	public String getJavaClassName() {
		if (!isJavaClass()) return null;

		String packageName = null;
		if (psiFile instanceof PsiClassOwner) {
			PsiClassOwner classOwner = (PsiClassOwner) psiFile;
			packageName = Read.call(classOwner, o-> o.getPackageName());
		}

		String packagePrefix = Strings.isEmpty(packageName) ? "" : packageName + ".";
		return packagePrefix + file.getNameWithoutExtension();
	}
}
