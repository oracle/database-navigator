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

package com.dbn.object.factory;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.state.GenericStateHolder;
import com.dbn.common.state.StateHolder;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.object.factory.ObjectFactoryManager.COMPONENT_NAME;

@State(
        name = COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class ObjectFactoryManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.ObjectFactoryManager";

    private final Map<DBObjectType, GenericStateHolder> states = new ConcurrentHashMap<>();

    private ObjectFactoryManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static ObjectFactoryManager getInstance(@NotNull Project project) {
        return projectService(project, ObjectFactoryManager.class);
    }

    @NotNull
    public StateHolder getState(DBObjectType category) {
        return states.computeIfAbsent(category, k -> new GenericStateHolder());
    }

    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        Element statesElement = newElement(element, "factory-states");
        for (DBObjectType category : states.keySet()) {
            Element stateElement = newElement(statesElement, "factory-state");
            setEnumAttribute(stateElement, "object-type", category);

            GenericStateHolder state = states.get(category);
            state.writeState(stateElement);
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element statesElement = element.getChild("factory-states");
        if (statesElement != null) {
            for (Element stateElement : statesElement.getChildren("factory-state")) {
                DBObjectType category = enumAttribute(stateElement, "object-type", DBObjectType.class);
                GenericStateHolder state = new GenericStateHolder();
                state.readState(stateElement);
                states.put(category, state);
            }
        }
    }
}
