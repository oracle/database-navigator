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
import com.dbn.common.util.Dialogs;
import com.dbn.execution.java.wrapper.ui.WrapperResultDialog;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.component.Components.projectService;

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

    @NotNull
    public WrapperModel createExecutionWrappers(DBJavaMethod method, boolean useFriendlyNames, boolean compileInDebugMode) throws SQLException {
        WrapperModelInput modelInput = new WrapperModelInput(method, useFriendlyNames, compileInDebugMode);
        return createExecutionWrappers(modelInput);
    }

    @NotNull
    public WrapperModel createExecutionWrappers(DBJavaClass javaClass, List<DBJavaMethod> methods, boolean useFriendlyNames, boolean compileInDebugMode) throws SQLException {
        WrapperModelInput modelInput = new  WrapperModelInput(javaClass, methods, useFriendlyNames, compileInDebugMode);
        return createExecutionWrappers(modelInput);
    }

    @NotNull
    private static WrapperModel createExecutionWrappers(WrapperModelInput modelInput) throws SQLException {
        WrapperModelBuilder builder = WrapperModelBuilder.getInstance();
        WrapperModel model = builder.buildModel(modelInput);
        WrapperStatementExecutor.createExecutionWrappers(model);
        return model;
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
