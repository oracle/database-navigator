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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.constantAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;
import static com.dbn.common.util.TimeUtil.isOlderThan;
import static com.dbn.prerequisite.DatabasePrerequisiteManager.COMPONENT_NAME;
import static java.util.concurrent.TimeUnit.MINUTES;

@State(name = COMPONENT_NAME, storages = @Storage(DatabaseNavigator.STORAGE_FILE))
public class DatabasePrerequisiteManager extends ProjectComponentBase implements PersistentState {
	public static final String COMPONENT_NAME = "DBNavigator.Project.DatabasePrerequisiteManager";

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
        PrerequisiteData prerequisiteData = getPrerequisiteData(connection);
        return prerequisiteData.getPrerequisiteGroup(operation);
    }

    @Nullable
    private PrerequisiteData getPrerequisiteData(ConnectionId connectionId) {
        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        return connection != null ? getPrerequisiteData(connection) : null;
    }

    private PrerequisiteData getPrerequisiteData(ConnectionHandler connection) {
        ConnectionId connectionId = connection.getConnectionId();
        return this.prerequisiteData.computeIfAbsent(connectionId, c -> new PrerequisiteData(connection));
    }

    public void showPrerequisiteDetails(ConnectionHandler connection, DatabaseOperation operation) {
        PrerequisiteGroup prerequisiteGroup = getPrerequisiteGroup(connection, operation);
        Dialogs.show(() -> new PrerequisitesDialog(prerequisiteGroup));
    }

    public void startOperation(ConnectionHandler connection, DatabaseOperation operation, Runnable operationRunner) {
        PrerequisiteGroup prerequisiteGroup = getPrerequisiteGroup(connection, operation);
        resetPrerequisites(prerequisiteGroup);

        Project project = connection.getProject();
        Progress.prompt(
                project, connection, true,
                "Verifying prerequisites",
                "Verifying prerequisites for operation \"" + operation.getName() + "\"",
                indicator -> verifyOperation(connection, operation, operationRunner));
    }

    private static void resetPrerequisites(PrerequisiteGroup prerequisiteGroup) {
        if (!prerequisiteGroup.isEvaluated()) return;
        if (prerequisiteGroup.arePrerequisitesMet()) {
            // reevaluate if older than 10 minutes
            // (environment states and user privileges may change)
            long evaluationTimestamp = prerequisiteGroup.getEvaluationTimestamp();
            if (!isOlderThan(evaluationTimestamp, 10, MINUTES)) return;
        }

        // reset the prerequisite group - force reevaluation
        prerequisiteGroup.reset();
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
        PrerequisiteData prerequisiteData = getPrerequisiteData(connection);
        OptionBroker<PrerequisiteOption> optionBroker = prerequisiteData.getOptionBroker(operation);
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

        Element prerequisitesElement = newElement(element, "prerequisites");
        Set<ConnectionId> connectionIds = prerequisiteData.keySet();
        for (ConnectionId connectionId : connectionIds) {
            Element dataElement = newElement(prerequisitesElement, "prerequisite-data");

            PrerequisiteData prerequisiteData = this.prerequisiteData.get(connectionId);

            setConstantAttribute(dataElement, "connection-id", connectionId);
            prerequisiteData.writeState(dataElement);
        }

        return element;
	}

	@Override
	public void loadComponentState(@NotNull Element element) {
        Element prerequisitesElement = element.getChild("prerequisites");
        for (Element dataElement : childrenOf(prerequisitesElement)) {
            ConnectionId connectionId = constantAttribute(dataElement, "connection-id", ConnectionId.class);

            PrerequisiteData prerequisiteData = getPrerequisiteData(connectionId);
            if (prerequisiteData == null) continue;

            prerequisiteData.readState(dataElement);
        }
    }
}
