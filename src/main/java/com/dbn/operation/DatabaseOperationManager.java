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

package com.dbn.operation;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.connection.ConnectionId;
import com.dbn.operation.model.OperationBundle;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.operation.DatabaseOperationManager.COMPONENT_NAME;

@State(name = COMPONENT_NAME, storages = @Storage(DatabaseNavigator.STORAGE_FILE))
public class DatabaseOperationManager extends ProjectComponentBase implements PersistentState {
	public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseOperationManager";
	private final Map<ConnectionId, OperationBundle> operationBundles = new ConcurrentHashMap<>();

	private DatabaseOperationManager(Project project) {
		super(project, COMPONENT_NAME);
	}

	public static DatabaseOperationManager getInstance(@NotNull Project project) {
		return projectService(project, DatabaseOperationManager.class);
	}




	public OperationBundle getOperationBundle(ConnectionId connectionId) {
		return operationBundles.computeIfAbsent(connectionId,(c->new OperationBundle()));
	}





	/****************************************
	 *       PersistentStateComponent       *
	 *****************************************/
	@Nullable
	@Override
	public Element getComponentState() {
		Element element = newStateElement();
		Element statesElement = newElement(element, "batch-states");
//		for (String category : states.keySet()) {
//			Element stateElement = newElement(statesElement, "state");
//			setStringAttribute(stateElement, "category", category);
//
//			GenericStateHolder state = states.get(category);
//			state.writeState(stateElement);
//		}
		return element;
	}

	@Override
	public void loadComponentState(@NotNull Element element) {
		Element statesElement = element.getChild("batch-states");
//		if (statesElement != null) {
//			for (Element stateElement : statesElement.getChildren("state")) {
//				String category = stringAttribute(stateElement, "category");
//				GenericStateHolder state = new GenericStateHolder();
//				state.readState(stateElement);
//				states.put(category, state);
//			}
//		}
	}
}
