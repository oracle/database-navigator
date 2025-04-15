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
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.state.GenericStateHolder;
import com.dbn.common.state.StateHolder;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.sync.java.upload.ui.JavaUploadResultDialog;
import com.dbn.sync.java.upload.ui.JavaUploaderInputDialog;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JdkOrderEntry;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
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

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.sync.java.upload.JavaUploadManager.COMPONENT_NAME;

@State(name = COMPONENT_NAME, storages = @Storage(DatabaseNavigator.STORAGE_FILE))
public class JavaUploadManager extends ProjectComponentBase implements PersistentState {
	public static final String COMPONENT_NAME = "DBNavigator.Project.JavaUploaderManager";

	private final Map<String, GenericStateHolder> states = new ConcurrentHashMap<>();

	private JavaUploadManager(Project project) {
		super(project, COMPONENT_NAME);
	}

	public static JavaUploadManager getInstance(@NotNull Project project) {
		return projectService(project, JavaUploadManager.class);
	}

	public void openCodeUploader(List<VirtualFile> javaClass) {
		Progress.prompt(getProject(), null, true, "Preparing Java Upload", "Loading dependencies for selected classes ...", progress -> prepareUploadDialog(javaClass));
	}


	public void openCodeUploader(VirtualFile javaClass) {
		Progress.prompt(getProject(), null, true, "Preparing Java Upload", "Loading dependencies for " + javaClass.getPresentableName() + "...", progress -> prepareUploadDialog(javaClass));
	}

	private void prepareUploadDialog(VirtualFile sourceObject) {
		try {
			List<JavaUploadElement> dependencies = loadUploadDependencies(sourceObject);
			JavaUploadInput input = new JavaUploadInput(getProject(), sourceObject, dependencies);
			JavaUploadContext context = new JavaUploadContext(input);

			Dialogs.show(() -> new JavaUploaderInputDialog(context));
		} catch (SQLException e) {
			Messages.showErrorDialog(getProject(), "Error Loading Java Dependencies", "Failed to load dependencies for " + sourceObject.getPresentableName(), e);
		}
	}

	private void prepareUploadDialog(List<VirtualFile> sourceObject) {
		try {
			List<JavaUploadElement> dependencies = new ArrayList<>();
			for (VirtualFile sourceFile : sourceObject) {
				dependencies.addAll(loadUploadDependencies(sourceFile));
			}

			JavaUploadInput input = new JavaUploadInput(getProject(), sourceObject, dependencies);
			JavaUploadContext context = new JavaUploadContext(input);

			Dialogs.show(() -> new JavaUploaderInputDialog(context));
		} catch (SQLException e) {
			Messages.showErrorDialog(getProject(), "Error Loading Java Dependencies", "Failed to load dependencies", e);
		}
	}

	public void uploadFile(JavaUploadContext context) {
		JavaUploadInput input = context.getInput();
		DatabaseContext databaseContext = input.getDatabaseContext();
		Progress.prompt(getProject(), databaseContext, true,
				"Uploading Java Classes",
				"Uploading java classes and dependencies to " + databaseContext.getConnection().getName(),
				progress -> performUpload(context));

	}

	private void performUpload(JavaUploadContext context) {
		JavaUploader.INSTANCE.uploadJavaClasses(context);
		Dialogs.show(() -> new JavaUploadResultDialog(getProject(), context));
	}

	@NotNull
	public StateHolder getState(String category) {
		return states.computeIfAbsent(category, k -> new GenericStateHolder());
	}

	private List<JavaUploadElement> loadUploadDependencies(VirtualFile virtualFile) throws SQLException {
		Project project = getProject();

		ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();

		Set<JavaUploadElement> results = ApplicationManager.getApplication().runReadAction((Computable<Set<JavaUploadElement>>) () -> {
			Set<JavaUploadElement> resultList = new HashSet<>();

			PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
			if (!(psiFile instanceof PsiClassOwner)) return resultList;

			PsiClass[] classes = ((PsiClassOwner) psiFile).getClasses();

			for (PsiClass psiClass : classes) {
				psiClass.accept(new JavaRecursiveElementWalkingVisitor() {
					@Override
					public void visitReferenceElement(@NotNull PsiJavaCodeReferenceElement reference) {
						super.visitReferenceElement(reference);

						PsiElement resolved = reference.resolve();
						if (resolved instanceof PsiClass) {
							PsiClass referencedClass = (PsiClass) resolved;
							VirtualFile refFile = PsiUtilCore.getVirtualFile(referencedClass);

							if (refFile == null) return;

							List<OrderEntry> entries = fileIndex.getOrderEntriesForFile(refFile);
							boolean isJdk = false;
							String jarPath = null;
							for (OrderEntry entry : entries) {
								if (entry instanceof JdkOrderEntry) {
									isJdk = true;
									break;
								}
								if (entry instanceof LibraryOrderEntry) {
									VirtualFile jarRoot = fileIndex.getClassRootForFile(refFile);
									if (jarRoot != null) {
										VirtualFileSystem fs = jarRoot.getFileSystem();
										if ("jar".equals(fs.getProtocol())) {
											String fullPath = jarRoot.getPath();
											jarPath = fullPath.replaceAll("!/$", "");
										}
									}
								}
							}

							if (!isJdk) {
								resultList.add(new JavaUploadElement(getProject(), refFile, jarPath));
							}
						}
					}
				});
			}
			return resultList;
		});

		return new ArrayList<>(results);
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
