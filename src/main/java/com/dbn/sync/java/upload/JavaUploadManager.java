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

import com.dbn.DatabaseNavigator;
import com.dbn.batch.BatchManager;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.state.GenericStateHolder;
import com.dbn.common.state.StateHolder;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.Read;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.sync.java.upload.ui.JavaUploadResultDialog;
import com.dbn.sync.java.upload.ui.JavaUploaderInputDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JdkOrderEntry;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiUtilCore;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.file.util.ProjectFiles.isProjectSourceFile;
import static com.dbn.common.file.util.VirtualFiles.isArchive;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.sync.java.upload.JavaUploadManager.COMPONENT_NAME;

@State(name = COMPONENT_NAME, storages = @Storage(DatabaseNavigator.STORAGE_FILE))
public class JavaUploadManager extends ProjectComponentBase implements PersistentState {
	public static final String COMPONENT_NAME = "DBNavigator.Project.JavaUploadManager";

	private final Map<String, GenericStateHolder> states = new ConcurrentHashMap<>();

	private JavaUploadManager(Project project) {
		super(project, COMPONENT_NAME);
	}

	public static JavaUploadManager getInstance(@NotNull Project project) {
		return projectService(project, JavaUploadManager.class);
	}

	public void openCodeUploader(VirtualFile file) {
		Progress.prompt(getProject(), null, true,
				"Preparing Java Upload",
				"Loading java dependencies for " + file.getPresentableName() + "...",
				progress -> prepareUploadDialog(file));
	}


	private void prepareUploadDialog(VirtualFile rootFile) {
		try {
			List<VirtualFile> files = new ArrayList<>();
			collectFiles(rootFile, files);

			Set<VirtualFile> dependencies = new HashSet<>();
			for (VirtualFile file : files) {
				Set<VirtualFile> fileDependencies = resolveDependencies(file);
				dependencies.addAll(fileDependencies);
			}

			List<JavaUploadTask> uploadElements = dependencies.stream().map(f -> new JavaUploadTask(getProject(), f)).sorted().collect(Collectors.toList());

			JavaUploadInput input = new JavaUploadInput(getProject(), rootFile, uploadElements);
			JavaUploadBatch batch = new JavaUploadBatch(input);

			Dialogs.show(() -> new JavaUploaderInputDialog(batch));
		} catch (SQLException e) {
			Messages.showErrorDialog(getProject(), "Error Loading Java Dependencies", "Failed to load dependencies for " + rootFile.getPresentableName(), e);
		}
	}

	private void collectFiles(VirtualFile rootFile, List<VirtualFile> files) {
		if (rootFile.isDirectory()) {
			for (VirtualFile file : rootFile.getChildren()) {
				if (file.isDirectory()) {
					collectFiles(file, files);
				} else {
					if (isUploadSupported(file)) {
						files.add(file);
					}
				}
			}
		} else {
			if (isUploadSupported(rootFile)) {
				files.add(rootFile);
			}
		}
	}

	private boolean isUploadSupported(VirtualFile file) {
		if (isArchive(file)) return true;
		if (isProjectSourceFile(getProject(), file)) return true;
		return false;
	}

	public void startUpload(JavaUploadBatch batch) {
		BatchManager batchManager = BatchManager.getInstance(getProject());
		batchManager.startBatchProcess(batch);
	}

	public void openBatchResult(JavaUploadBatch batch) {
		Dialogs.show(() -> new JavaUploadResultDialog(batch));
	}

	@NotNull
	public StateHolder getState(String category) {
		return states.computeIfAbsent(category, k -> new GenericStateHolder());
	}

	private Set<VirtualFile> resolveDependencies(VirtualFile virtualFile) throws SQLException {
		Project project = getProject();
		PsiManager psiManager = PsiManager.getInstance(project);

		return Read.call(() -> {
			Set<VirtualFile> dependencies = new HashSet<>();
			dependencies.add(virtualFile);

			PsiFile psiFile = psiManager.findFile(virtualFile);
            if (psiFile instanceof PsiClassOwner) {
				PsiClassOwner classOwner = (PsiClassOwner) psiFile;
				PsiClass[] classes = classOwner.getClasses();

                for (PsiClass psiClass : classes) {
					PsiElementVisitor collector = createDependenciesCollector(dependencies);
					psiClass.accept(collector);
                }
            }
			return dependencies;
        });
	}

	private PsiElementVisitor createDependenciesCollector(Set<VirtualFile> dependencies) {
		return new JavaRecursiveElementWalkingVisitor() {
			@Override
			public void visitReferenceElement(@NotNull PsiJavaCodeReferenceElement reference) {
				super.visitReferenceElement(reference);

				PsiElement psiElement = reference.resolve();
				if (psiElement instanceof PsiClass) {
					PsiClass psiClass = (PsiClass) psiElement;
					VirtualFile file = PsiUtilCore.getVirtualFile(psiClass);
					if (file == null) return;

					List<OrderEntry> entries = getFileIndex().getOrderEntriesForFile(file);
					for (OrderEntry entry : entries) {
						if (entry instanceof JdkOrderEntry) return;

						if (entry instanceof LibraryOrderEntry) {
							file = getArchiveFile(file);
						}
					}

					if (file == null) return;
					dependencies.add(file);
				}
			}
		};
	}

	private ProjectFileIndex getFileIndex() {
		Project project = getProject();
		ProjectRootManager rootManager = ProjectRootManager.getInstance(project);
		return rootManager.getFileIndex();
	}

	@Nullable
	private VirtualFile getArchiveFile(VirtualFile file) {
		ProjectFileIndex fileIndex = getFileIndex();
		VirtualFile jarRoot = fileIndex.getClassRootForFile(file);
		if (jarRoot == null) return null;

		VirtualFileSystem fileSystem = jarRoot.getFileSystem();
		if (fileSystem instanceof JarFileSystem) {
			JarFileSystem jarFileSystem = (JarFileSystem) fileSystem;
			return jarFileSystem.getLocalByEntry(jarRoot);
		}

		return null;
	}

	/****************************************
	 *       PersistentStateComponent       *
	 *****************************************/
	@Nullable
	@Override
	public Element getComponentState() {
		Element element = newStateElement();
		Element statesElement = newElement(element, "uploader-states");
		for (String category : states.keySet()) {
			Element stateElement = newElement(statesElement, "state");
			setStringAttribute(stateElement, "category", category);

			GenericStateHolder state = states.get(category);
			state.writeState(stateElement);
		}
		return element;
	}

	@Override
	public void loadComponentState(@NotNull Element element) {
		Element statesElement = element.getChild("uploader-states");
		if (statesElement != null) {
			for (Element stateElement : statesElement.getChildren("state")) {
				String category = stringAttribute(stateElement, "category");
				GenericStateHolder state = new GenericStateHolder();
				state.readState(stateElement);
				states.put(category, state);
			}
		}
	}
}
