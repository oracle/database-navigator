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

package com.dbn.prerequisite;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.state.StateCategory;
import com.dbn.common.state.StateContainer;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.prerequisite.DatabasePrerequisiteManager.COMPONENT_NAME;

@State(name = COMPONENT_NAME, storages = @Storage(DatabaseNavigator.STORAGE_FILE))
public class DatabasePrerequisiteManager extends ProjectComponentBase implements PersistentState {
	public static final String COMPONENT_NAME = "DBNavigator.Project.DatabasePrerequisiteManager";

	private final StateContainer states = new StateContainer();

	private DatabasePrerequisiteManager(Project project) {
		super(project, COMPONENT_NAME);
        initDefinitionProviders();
    }

    private void initDefinitionProviders() {
        //List<PrerequisiteDefinitionProvider> extensionList = PrerequisiteDefinitionProvider.EP.getExtensionList();
    }

    public static DatabasePrerequisiteManager getInstance(@NotNull Project project) {
		return projectService(project, DatabasePrerequisiteManager.class);
	}

	@NotNull
	public StateAttributes getState(StateCategory category) {
		return states.ensureAttributes(category);
	}


    public void evaluatePrerequisite() {

    }


	/****************************************
	 *       PersistentStateComponent       *
	 *****************************************/
	@Nullable
	@Override
	public Element getComponentState() {
		Element element = newStateElement();
        states.writeState(element, "prerequisite-states");
		return element;
	}

	@Override
	public void loadComponentState(@NotNull Element element) {
        states.readState(element, "prerequisite-states");
	}
}
