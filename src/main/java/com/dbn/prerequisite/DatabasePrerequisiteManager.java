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
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.operation.DatabaseOperation;
import com.dbn.common.option.OptionBroker;
import com.dbn.common.state.StateContainer;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.prerequisite.model.PrerequisiteData;
import com.dbn.prerequisite.model.PrerequisiteGroup;
import com.dbn.prerequisite.resolution.PrerequisiteOption;
import com.dbn.prerequisite.ui.PrerequisitesDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.prerequisite.DatabasePrerequisiteManager.COMPONENT_NAME;

@State(name = COMPONENT_NAME, storages = @Storage(DatabaseNavigator.STORAGE_FILE))
public class DatabasePrerequisiteManager extends ProjectComponentBase implements PersistentState {
	public static final String COMPONENT_NAME = "DBNavigator.Project.DatabasePrerequisiteManager";

    private final StateContainer states = new StateContainer();
    private final Map<ConnectionId, PrerequisiteData> prerequisiteData = new ConcurrentHashMap<>();

	private DatabasePrerequisiteManager(Project project) {
		super(project, COMPONENT_NAME);
        ProjectEvents.subscribe(project, this, ConnectionConfigListener.TOPIC, createConnectionConfigListener());
    }

    private ConnectionConfigListener createConnectionConfigListener() {
        return ConnectionConfigListener.whenRemoved(connectionId -> prerequisiteData.remove(connectionId));
    }

    public static DatabasePrerequisiteManager getInstance(@NotNull Project project) {
		return projectService(project, DatabasePrerequisiteManager.class);
	}

    private PrerequisiteGroup getPrerequisiteGroup(ConnectionHandler connection, DatabaseOperation operation) {
        ConnectionId connectionId = connection.getConnectionId();
        PrerequisiteData prerequisiteData = this.prerequisiteData.computeIfAbsent(connectionId, c -> new PrerequisiteData(connection));
        return prerequisiteData.getPrerequisiteGroup(operation);
    }

    public void showPrerequisiteDetails(ConnectionHandler connection, DatabaseOperation operation) {
        PrerequisiteGroup prerequisiteGroup = getPrerequisiteGroup(connection, operation);
        Dialogs.show(() -> new PrerequisitesDialog(prerequisiteGroup));
    }

    public void startOperation(ConnectionHandler connection, DatabaseOperation operation, Runnable operationRunner) {
        Project project = connection.getProject();
        Progress.prompt(
                project, connection, true,
                "Verifying prerequisites",
                "Verifying prerequisites for operation \"" + operation.getName() + "\"",
                indicator -> verifyOperation(connection, operation, operationRunner));
    }

    public void verifyOperation(ConnectionHandler connection, DatabaseOperation operation, Runnable operationRunner) {
        PrerequisiteGroup prerequisiteGroup = getPrerequisiteGroup(connection, operation);
        // evaluate if not yet done
        if (!prerequisiteGroup.isEvaluated()) {
            prerequisiteGroup.evaluateAll(false);
        }

        // all green - continue with operation
        if (prerequisiteGroup.arePrerequisitesMet()) {
            operationRunner.run();
            return;
        }

        // check "do not ask" option
        Project project = connection.getProject();
        OptionBroker<PrerequisiteOption> optionBroker = prerequisiteGroup.getOptionBroker();
        optionBroker.resolve(project, null,
                option -> brokerOption(connection, operation, operationRunner, option));
    }

    private void brokerOption(ConnectionHandler connection, DatabaseOperation operation, Runnable operationRunner, PrerequisiteOption option) {
        if (option == PrerequisiteOption.CANCEL) return;
        if (option == PrerequisiteOption.CONTINUE) operationRunner.run();
        if (option == PrerequisiteOption.RESOLVE) showPrerequisiteDetails(connection, operation);
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
