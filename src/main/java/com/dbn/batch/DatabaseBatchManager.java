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

package com.dbn.batch;

import com.dbn.DatabaseNavigator;
import com.dbn.batch.ui.BatchMonitorDialog;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.message.ui.MessageBundleDialog;
import com.dbn.common.message.ui.MessageBundleDialogConfig;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.state.StateCategory;
import com.dbn.common.state.StateContainer;
import com.dbn.common.util.Dialogs;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.batch.DatabaseBatchManager.COMPONENT_NAME;
import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.message.MessageType.ERROR;
import static com.dbn.common.message.MessageType.WARNING;
import static com.dbn.common.options.setting.Settings.newStateElement;

@State(name = COMPONENT_NAME, storages = @Storage(DatabaseNavigator.STORAGE_FILE))
public class DatabaseBatchManager extends ProjectComponentBase implements PersistentState {
	public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseBatchManager";

	private final StateContainer states = new StateContainer();

	private DatabaseBatchManager(Project project) {
		super(project, COMPONENT_NAME);
	}

	public static DatabaseBatchManager getInstance(@NotNull Project project) {
		return projectService(project, DatabaseBatchManager.class);
	}

	@NotNull
	public StateAttributes getState(String category) {
        StateCategory stateCategory = StateCategory.get(category);
        return states.ensureAttributes(stateCategory);
	}


	public void startBatchProcess(Batch<?, ?> batch) {
		batch.init();
		openBatchMonitor(batch);
	}


	private void openBatchMonitor(Batch<?, ?> batch) {
		Dialogs.show(() -> new BatchMonitorDialog(batch));
	}

	public void showErrorDialog(Batch<?, ?> batch) {
		Dialogs.show(() -> createErrorDialog(batch));
	}

	private MessageBundleDialog createErrorDialog(Batch<?, ?> batch) {
		Project project = getProject();
		Object contextObject = batch.getContextObject();

		MessageBundleDialogConfig config = MessageBundleDialogConfig
				.create(project, "Errors")
				.withContextObject(contextObject)
				.withMessageTypes(ERROR, WARNING);

		return new MessageBundleDialog(config, batch.getMessages());
	}

	/****************************************
	 *       PersistentStateComponent       *
	 *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        states.writeState(element, "batch-states");
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        states.readState(element, "batch-states");
    }
}
