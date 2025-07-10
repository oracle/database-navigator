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
import com.dbn.common.state.StateAttributes;
import com.dbn.common.state.StateCategory;
import com.dbn.common.state.StateContainer;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.prerequisite.definition.PrerequisiteDefinition;
import com.dbn.prerequisite.definition.PrerequisiteDefinitionProvider;
import com.dbn.prerequisite.evaluation.PrerequisiteRequirementEvaluator;
import com.dbn.prerequisite.model.Prerequisite;
import com.dbn.prerequisite.model.PrerequisiteBundle;
import com.dbn.prerequisite.model.PrerequisiteType;
import com.dbn.prerequisite.ui.PrerequisitesDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

	@NotNull
	public StateAttributes getState(StateCategory category) {
		return states.ensureAttributes(category);
	}

    private PrerequisiteBundle getPrerequisiteBundle(ConnectionHandler connection, DatabaseOperation operation) {
        ConnectionId connectionId = connection.getConnectionId();
        PrerequisiteData prerequisiteData = this.prerequisiteData.computeIfAbsent(connectionId, c -> new PrerequisiteData(connection));
        return prerequisiteData.computeIfAbsent(operation, (c, o) -> createPrerequisiteBundle(c, o));
    }

    private PrerequisiteBundle createPrerequisiteBundle(ConnectionHandler connection, DatabaseOperation operation) {
        Set<PrerequisiteType> types = resolvePrerequisiteTypes(connection, operation);
        List<PrerequisiteDefinition> definitions = loadPrerequisiteDefinitions(types);
        List<Prerequisite> prerequisites = createPrerequisites(definitions);
        return new PrerequisiteBundle(connection, operation, prerequisites);
    }


    private static Set<PrerequisiteType> resolvePrerequisiteTypes(ConnectionHandler connection, DatabaseOperation operation) {
        Set<PrerequisiteType> types = new LinkedHashSet<>();
        List<PrerequisiteRequirementEvaluator> evaluators = PrerequisiteRequirementEvaluator.EP.getExtensionList();
        for (PrerequisiteRequirementEvaluator evaluator : evaluators) {
            List<PrerequisiteType> applicableTypes = evaluator.resolvePrerequisites(connection, operation);
            types.addAll(applicableTypes);
        }
        return types;
    }

    private static List<Prerequisite> createPrerequisites(List<PrerequisiteDefinition> definitions) {
        List<Prerequisite> prerequisites = new ArrayList<>();
        for (PrerequisiteDefinition definition : definitions) {
            Prerequisite prerequisite = definition.createPrerequisite();
            prerequisites.add(prerequisite);
        }
        return prerequisites;
    }

    private static List<PrerequisiteDefinition> loadPrerequisiteDefinitions(Set<PrerequisiteType> types) {
        List<PrerequisiteDefinition> definitions = new ArrayList<>();
        List<PrerequisiteDefinitionProvider> providers = PrerequisiteDefinitionProvider.EP.getExtensionList();
        for (PrerequisiteDefinitionProvider provider : providers) {
            PrerequisiteDefinition definition = provider.getDefinition();
            if (types.contains(definition.getType())) {
                definitions.add(definition);
            }
        }
        return definitions;
    }

    public void evaluatePrerequisites(ConnectionHandler connection, DatabaseOperation operation) {
        PrerequisiteBundle prerequisites = getPrerequisiteBundle(connection, operation);
        Dialogs.show(() -> new PrerequisitesDialog(prerequisites));
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
