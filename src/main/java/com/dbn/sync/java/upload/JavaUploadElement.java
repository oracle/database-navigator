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

import com.dbn.common.icon.Icons;
import com.dbn.sync.common.impl.SyncElementBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

@Data
@EqualsAndHashCode(callSuper = false)
public class JavaUploadElement extends SyncElementBase {
	private VirtualFile javaFile;
	private final String jarPath;
	private transient Project project;

	public JavaUploadElement(Project project, VirtualFile javaFile, String jarPath) {
		this.javaFile = javaFile;
		this.jarPath = jarPath;
		this.project = project;
	}

	public String getPackageName() {
		PsiFile psiFile = PsiManager.getInstance(project).findFile(javaFile);
		if (psiFile instanceof PsiClassOwner) {
			return ((PsiClassOwner) psiFile).getPackageName();
		}
		return null;
	}

	public String getJavaClassName() {
		return getPackageName() + "." + javaFile.getNameWithoutExtension();
	}

	@NotNull
	@Override
	public String getName() {
		return jarPath != null ? jarPath : getJavaClassName();
	}

	@Override
	public @Nullable Icon getIcon() {
		return Icons.DBO_JAVA_CLASS;
	}
}
