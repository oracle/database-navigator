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
import com.dbn.common.ui.list.Enableable;
import com.dbn.common.ui.list.Selectable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Objects;

@Getter
@Setter
public class JavaUploadElement implements Selectable<JavaUploadElement>, Enableable<JavaUploadElement> {
	private VirtualFile javaClass;
	private final String jarPath;
	private boolean enabled;
	private boolean selected;
	private Project project;

	public JavaUploadElement(Project project, VirtualFile javaClass, String jarPath) {
		this.javaClass = javaClass;
		this.jarPath = jarPath;
		this.enabled = true;
		this.selected = true;
		this.project = project;
	}

	public String getPackageName() {
		PsiFile psiFile = PsiManager.getInstance(project).findFile(javaClass);
		if (psiFile instanceof PsiJavaFile) {
			return ((PsiJavaFile) psiFile).getPackageName();
		}
		return null;
	}

	public String getQualifiedName(){
		String packageName = getJavaClassName();
		return packageName.replace(".", "/");
	}

	public String getJavaClassName() {
		return getPackageName() + "." + javaClass.getNameWithoutExtension();
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof JavaUploadElement)) return false;
		JavaUploadElement that = (JavaUploadElement) o;
		return Objects.equals(jarPath, that.jarPath) || Objects.equals(javaClass, that.javaClass);
	}

	@Override
	public int hashCode() {
		return Objects.hash(javaClass, jarPath);
	}
}
