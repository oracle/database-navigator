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

package com.dbn.object.filter.quick;

import com.dbn.DatabaseNavigator;
import com.dbn.browser.model.BrowserTreeEventListener;
import com.dbn.common.component.Components;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.options.setting.Settings;
import com.dbn.common.ui.tree.TreeEventType;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionManager;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.filter.ConditionOperator;
import com.dbn.object.filter.quick.ui.ObjectQuickFilterDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
@State(
    name = ObjectQuickFilterManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class ObjectQuickFilterManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.ObjectQuickFilterManager";

    private final Map<ObjectQuickFilterKey, ObjectQuickFilter<?>> quickFilters = new HashMap<>();
    private ConditionOperator lastUsedOperator = ConditionOperator.EQUAL;

    private ObjectQuickFilterManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static ObjectQuickFilterManager getInstance(@NotNull Project project) {
        return Components.projectService(project, ObjectQuickFilterManager.class);
    }

    public void openFilterDialog(DBObjectList<?> objectList) {
        Dialogs.show(() -> new ObjectQuickFilterDialog(getProject(), objectList));
    }

    public void applyFilter(DBObjectList<?> objectList, @Nullable ObjectQuickFilter filter) {
        objectList.setQuickFilter(filter);
        ProjectEvents.notify(getProject(),
                BrowserTreeEventListener.TOPIC,
                (listener) -> listener.nodeChanged(objectList, TreeEventType.STRUCTURE_CHANGED));

        ObjectQuickFilterKey key = ObjectQuickFilterKey.from(objectList);
        if (filter == null || filter.isEmpty()) {
            quickFilters.remove(key);
        } else {
            quickFilters.put(key, filter);
        }
    }

    public void restoreQuickFilter(DBObjectList<?> objectList) {
        ObjectQuickFilterKey key = ObjectQuickFilterKey.from(objectList);
        objectList.setQuickFilter(cast(quickFilters.get(key)));
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        Settings.setEnum(element, "last-used-operator", lastUsedOperator);
        Element filtersElement = newElement(element, "filters");

        ConnectionManager connectionManager = ConnectionManager.getInstance(getProject());
        for (val entry : quickFilters.entrySet()) {
            ObjectQuickFilterKey key = entry.getKey();
            if (connectionManager.isValidConnectionId(key.getConnectionId())) {
                ObjectQuickFilter<?> filter = entry.getValue();
                Element filterElement = newElement(filtersElement, "filter");

                key.writeState(filterElement);
                filter.writeState(filterElement);
            }
        }

        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element filtersElement = element.getChild("filters");
        lastUsedOperator = Settings.getEnum(element, "last-used-operator", lastUsedOperator);
        if (filtersElement != null) {
            for (Element child : filtersElement.getChildren()) {
                ObjectQuickFilterKey key = new ObjectQuickFilterKey();
                key.readState(child);

                ObjectQuickFilter<?> filter = new ObjectQuickFilter<>(key.getObjectType());
                filter.readState(child);

                quickFilters.put(key, filter);
            }
        }
    }
}
