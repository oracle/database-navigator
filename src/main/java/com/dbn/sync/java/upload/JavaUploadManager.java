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
import com.dbn.batch.DatabaseBatchManager;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.state.StateCategory;
import com.dbn.common.state.StateContainer;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.Read;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Files;
import com.dbn.sync.java.upload.ui.JavaUploadResultDialog;
import com.dbn.sync.java.upload.ui.JavaUploaderInputDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileDocumentManager;
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
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.file.util.ProjectFiles.isModuleDependency;
import static com.dbn.common.file.util.ProjectFiles.isProjectSourceFile;
import static com.dbn.common.file.util.VirtualFiles.isArchive;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.common.util.Messages.showInfoDialog;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.sync.java.upload.JavaUploadManager.COMPONENT_NAME;

@State(name = COMPONENT_NAME, storages = @Storage(DatabaseNavigator.STORAGE_FILE))
public class JavaUploadManager extends ProjectComponentBase implements PersistentState {
	public static final String COMPONENT_NAME = "DBNavigator.Project.JavaUploadManager";

	@NonNls
	private static final List<String> EXCLUDED_LIBRARIES = Arrays.asList(
			"ojdbc",
			"dbjava",
			"oraclepki",
			"orai18n",
			"oci-java-sdk",
			"xmlparserv2",
			"xdb",
			"ucp"
	);

    private final StateContainer states = new StateContainer();

	private JavaUploadManager(Project project) {
		super(project, COMPONENT_NAME);
	}

	public static JavaUploadManager getInstance(@NotNull Project project) {
		return projectService(project, JavaUploadManager.class);
	}

	public void openCodeUploader(VirtualFile file) {
		FileDocumentManager.getInstance().saveAllDocuments();
		Progress.prompt(getProject(), null, true,
				txt("prc.java.title.PreparingJavaUpload"),
				txt("prc.java.text.LoadingJavaDependencies", file.getPresentableName()),
				progress -> prepareUploadDialog(file));
	}


	private void prepareUploadDialog(VirtualFile rootFile) {
		Project project = getProject();
		String rootFilePath = Files.convertToRelativePath(project, rootFile.getPath());
		try {
			Set<VirtualFile> dependencies = loadDependencies(rootFile);
			if (dependencies.isEmpty()) {
				showInfoDialog(project, txt("msg.java.title.NoJavaResources"), txt("msg.java.info.NoJavaResources", rootFilePath));
				return;
			}

			JavaUploadBatch batch = createBatch(rootFile, project, dependencies);
			Dialogs.show(() -> new JavaUploaderInputDialog(batch));
		} catch (Exception e) {
			showErrorDialog(project, txt("msg.java.title.ErrorLoadingJavaResources"), txt("msg.java.error.JavaResourcesLoadFailed", rootFilePath), e);
		}
	}

	private Set<VirtualFile> loadDependencies(VirtualFile rootFile) {
		List<VirtualFile> files = new ArrayList<>();
		collectFiles(rootFile, files);

		Set<VirtualFile> dependencies = new TreeSet<>(Comparator.comparing(f -> f.getPath()));
		for (VirtualFile file : files) {
			Set<VirtualFile> fileDependencies = resolveDependencies(file);
			dependencies.addAll(fileDependencies);
		}
		return dependencies;
	}

	private static JavaUploadBatch createBatch(VirtualFile rootFile, Project project, Set<VirtualFile> dependencies) {
		JavaUploadInput input = new JavaUploadInput(project, rootFile);
		JavaUploadBatch batch = new JavaUploadBatch(input);
		dependencies.forEach(f -> batch.createTask(f));
		return batch;
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

	private static boolean isExcludedDependency(VirtualFile file) {
		String name = file.getName();
		// TODO look for a better way to identify oracle driver libraries
		for (String pattern : EXCLUDED_LIBRARIES) {
			if (name.startsWith(pattern)) return true;
		}
		return false;
	}

	public boolean isUploadSupported(VirtualFile file) {
		Project project = getProject();
		if (isArchive(file)) {
			if (isExcludedDependency(file)) return false;
			if (!isModuleDependency(project, file)) return false;
			return true;
		}

		if (isProjectSourceFile(project, file)) {
			return true;
		}
		return false;
	}

	public void startUpload(JavaUploadBatch batch) {
		DatabaseBatchManager databaseBatchManager = DatabaseBatchManager.getInstance(getProject());
		databaseBatchManager.startBatchProcess(batch);
	}

	public void openBatchResult(JavaUploadBatch batch) {
		Dialogs.show(() -> new JavaUploadResultDialog(batch));
	}

    @NotNull
    public StateAttributes getState(String category) {
        StateCategory stateCategory = StateCategory.get(category);
        return states.ensureAttributes(stateCategory);
    }


	private Set<VirtualFile> resolveDependencies(VirtualFile virtualFile) {
		Project project = getProject();
		PsiManager psiManager = PsiManager.getInstance(project);

		return Read.call(() -> {
			Set<VirtualFile> dependencies = new HashSet<>();
			dependencies.add(virtualFile);

			PsiFile psiFile = psiManager.findFile(virtualFile);
            if (psiFile instanceof PsiClassOwner classOwner) {
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
				if (psiElement instanceof PsiClass psiClass) {
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
					if (isExcludedDependency(file)) return;
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
		if (fileSystem instanceof JarFileSystem jarFileSystem) {
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
        states.writeState(element, "download-states");
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        states.readState(element, "download-states");
    }

}
