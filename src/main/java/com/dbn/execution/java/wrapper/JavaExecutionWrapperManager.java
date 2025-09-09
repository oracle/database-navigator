/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.execution.java.wrapper;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.execution.java.wrapper.ui.WrapperNamesEditorDialog;
import com.dbn.execution.java.wrapper.ui.WrapperResultDialog;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.common.DBObject;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

@State(
		name = JavaExecutionWrapperManager.COMPONENT_NAME,
		storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
@Getter
public class JavaExecutionWrapperManager extends ProjectComponentBase implements PersistentState {
	public static final String COMPONENT_NAME = "DBNavigator.Project.JavaExecutionWrapperManager";

	private JavaExecutionWrapperManager(Project project) {
		super(project, COMPONENT_NAME);
	}

	public static JavaExecutionWrapperManager getInstance(@NotNull Project project) {
		return projectService(project, JavaExecutionWrapperManager.class);
	}

    public void createExecutionWrappers(DBJavaMethod javaMethod, boolean useFriendlyNames, boolean compileInDebugMode) {
        WrapperModelInput modelInput = new WrapperModelInput(javaMethod, useFriendlyNames, compileInDebugMode);
        createExecutionWrappers(modelInput);
    }

    public void createExecutionWrappers(DBJavaClass javaClass, List<DBJavaMethod> javaMethods, boolean useFriendlyNames, boolean compileInDebugMode) {
        WrapperModelInput modelInput = new WrapperModelInput(javaClass, javaMethods, useFriendlyNames, compileInDebugMode);
        createExecutionWrappers(modelInput);
    }

    private void createExecutionWrappers(WrapperModelInput modelInput) {
        DBObject sourceObject = modelInput.getSourceObject();
        Progress.prompt(getProject(), sourceObject, true,
                txt("prc.java.title.CreatingExecutionWrappers"),
                txt("prc.java.text.CreatingExecutionWrappers",
                        sourceObject.getTypeName(),
                        sourceObject.getPresentableName()),
                progress -> {
                    progress.setText2("Building execution wrapper model");
                    WrapperModelBuilder builder = WrapperModelBuilder.getInstance();
                    WrapperModel model = builder.buildModel(modelInput);
                    if(model.isFullyCompatible()) {
                        verifyAndCreateExecutionWrappers(model);
                    } else {
                        Messages.showErrorDialog(getProject(), "Wrapper Creation Error", String.join("\n", model.getCompatibilityIssues()));
                    }
                });
    }

    private void verifyAndCreateExecutionWrappers(WrapperModel model) {
        if (model.verifyIdentifierLengths()) {
            createExecutionWrappers(model);
            return;
        }

        Dialogs.show(() -> new WrapperNamesEditorDialog(getProject(), model), (dialog, exitCode) -> {
            if (exitCode != DialogWrapper.OK_EXIT_CODE) return;

            DBObject sourceObject = model.getSourceObject();
            Progress.prompt(getProject(), sourceObject, true,
                    txt("prc.java.title.CreatingExecutionWrappers"),
                    txt("prc.java.text.CreatingExecutionWrappers",
                            sourceObject.getTypeName(),
                            sourceObject.getPresentableName()),
                    progress -> createExecutionWrappers(model));
        });
    }

    private void createExecutionWrappers(WrapperModel model) {
        ProgressMonitor.setProgressDetail("Creating execution wrapper objects");
        try {
            WrapperStatementExecutor.createExecutionWrappers(model);
            if (model.getInput().isUseFriendlyNames()) {
                showWrapperResult(model);
            }
        } catch (Throwable e) {
            conditionallyLog(e);
            Messages.showErrorDialog(getProject(), "Failed to create execution wrappers", e);
        }
    }

    public void showWrapperResult(WrapperModel model) {
        Dialogs.show(() -> new WrapperResultDialog(getProject(), model));
    }

	/****************************************
	 *       PersistentStateComponent       *
	 *****************************************/
	@Nullable
	@Override
	public Element getComponentState() {
		Element element = new Element("state");
		return element;
	}

	@Override
	public void loadComponentState(@NotNull Element element) {
	}
}
