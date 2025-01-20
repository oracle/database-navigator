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

package com.dbn.editor.data.filter;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Dialogs.DialogCallback;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.data.model.ColumnInfo;
import com.dbn.editor.data.DatasetEditorManager;
import com.dbn.editor.data.filter.ui.DatasetFilterDialog;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import lombok.val;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;

@State(
    name = DatasetFilterManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatasetFilterManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatasetFilterManager";

    public static final DatasetFilter EMPTY_FILTER = new DatasetEmptyFilter();
    private final Map<ConnectionId, Map<String, DatasetFilterGroup>> filters = new ConcurrentHashMap<>();

    private DatasetFilterManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    public void switchActiveFilter(DBDataset dataset, DatasetFilter filter){
        Project project = dataset.getProject();
        DatasetFilterManager filterManager = DatasetFilterManager.getInstance(project);
        DatasetFilter activeFilter = filterManager.getActiveFilter(dataset);
        if (activeFilter != filter) {
            filterManager.setActiveFilter(dataset, filter);

        }
    }

    public void openFiltersDialog(DBDataset dataset, boolean automaticPrompt, boolean createNewFilter, DatasetFilterType defaultFilterType, DialogCallback<DatasetFilterDialog> callback) {
        Dialogs.show(() -> new DatasetFilterDialog(dataset, automaticPrompt, createNewFilter, defaultFilterType), callback);
    }

    public void createBasicFilter(@NotNull DBDataset dataset, String columnName, Object columnValue, ConditionOperator operator, boolean interactive) {
        DatasetFilterGroup filterGroup = getFilterGroup(dataset);
        DatasetBasicFilter filter = filterGroup.createBasicFilter(columnName, columnValue, operator, interactive);

        if (interactive) {
            Dialogs.show(() -> new DatasetFilterDialog(dataset, filter));
        } else {
            filter.setPersisted(true);
            filter.setTemporary(true);
            setActiveFilter(dataset, filter);
            DatasetEditorManager.getInstance(getProject()).reloadEditorData(dataset);
        }
    }

    public void createBasicFilter(@NotNull DBDataset dataset, String columnName, Object columnValue, ConditionOperator operator) {
        DatasetFilterGroup filterGroup = getFilterGroup(dataset);
        DatasetBasicFilter filter = filterGroup.createBasicFilter(columnName, columnValue, operator);

        filter.setPersisted(true);
        filter.setTemporary(true);
        setActiveFilter(dataset, filter);
        DatasetEditorManager.getInstance(getProject()).reloadEditorData(dataset);
    }

    public void createBasicFilter(DatasetFilterInput filterInput) {
        DBDataset dataset = filterInput.getDataset();
        DatasetFilterGroup filterGroup = getFilterGroup(dataset);
        DatasetBasicFilter filter = null;

        for (DBColumn column : filterInput.getColumns()) {
            Object value = filterInput.getColumnValue(column);
            if (filter == null) {
                filter = filterGroup.createBasicFilter(column.getName(), value, ConditionOperator.EQUAL);
            } else {
                filter.addCondition(column.getName(), value, ConditionOperator.EQUAL);
            }
        }

        if (filter != null) {
            filter.setPersisted(true);
            filter.setTemporary(true);
            setActiveFilter(dataset, filter);
            DatasetEditorManager.getInstance(getProject()).reloadEditorData(dataset);
        }
    }

    public void addConditionToFilter(DatasetBasicFilter filter, DBDataset dataset, ColumnInfo columnInfo, Object value, boolean interactive) {
        DatasetFilterGroup filterGroup = getFilterGroup(dataset);
        DatasetBasicFilterCondition condition = interactive ?
                new DatasetBasicFilterCondition(filter, columnInfo.getName(), value, ConditionOperator.EQUAL, true) :
                new DatasetBasicFilterCondition(filter, columnInfo.getName(), value, null);

        filter.addCondition(condition);
        filter.generateName();
        filterGroup.setActiveFilter(filter);
        if (interactive) {
            Dialogs.show(() -> new DatasetFilterDialog(dataset, false, false, DatasetFilterType.NONE));
        } else {
            DatasetEditorManager.getInstance(getProject()).reloadEditorData(dataset);
        }

    }



    public DatasetFilter getActiveFilter(@NotNull DBDataset dataset) {
        DatasetFilterGroup filterGroup = getFilterGroup(dataset);
        return filterGroup.getActiveFilter();
    }

    public void setActiveFilter(@NotNull DBDataset dataset, DatasetFilter filter) {
        DatasetFilterGroup filterGroup = getFilterGroup(dataset);
        filterGroup.setActiveFilter(filter);
    }

    private void addFilterGroup(@NotNull DatasetFilterGroup filterGroup) {
        ConnectionId connectionId = filterGroup.getConnectionId();
        String datasetName = filterGroup.getDatasetName();
        Map<String, DatasetFilterGroup> filterGroups = getFilterGroups(connectionId);

        filterGroups.put(datasetName, filterGroup);
    }

    public DatasetFilterGroup getFilterGroup(@NotNull DBDataset dataset) {
        ConnectionHandler connection = Failsafe.nn(dataset.getConnection());
        ConnectionId connectionId = connection.getConnectionId();
        String datasetName = dataset.getQualifiedName();
        return getFilterGroup(connectionId, datasetName);
    }

    public DatasetFilterGroup getFilterGroup(@NotNull DatasetFilter filter) {
        ConnectionId connectionId = filter.getConnectionId();
        String datasetName = filter.getDatasetName();
        return getFilterGroup(connectionId, datasetName);
    }

    @NotNull
    private Map<String, DatasetFilterGroup> getFilterGroups(ConnectionId connectionId) {
        return filters.computeIfAbsent(connectionId, id -> new ConcurrentHashMap<>());
    }

    @NotNull
    public DatasetFilterGroup getFilterGroup(ConnectionId connectionId, String datasetName) {
        Map<String, DatasetFilterGroup> filterGroups = getFilterGroups(connectionId);
        return filterGroups.computeIfAbsent(datasetName, n -> new DatasetFilterGroup(getProject(), connectionId, n));
    }

    public static DatasetFilterManager getInstance(@NotNull Project project) {
        return projectService(project, DatasetFilterManager.class);
    }

    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();

        for (val entry : filters.entrySet()) {
            ConnectionId connectionId = entry.getKey();
            ConnectionHandler connection = ConnectionHandler.get(connectionId);
            if (connection == null) continue;

            val filterLists = entry.getValue();
            for (val groupEntry : filterLists.entrySet()) {
                DatasetFilterGroup filterGroup = groupEntry.getValue();
                Element filterListElement = newElement(element, "filter-actions");
                filterGroup.writeConfiguration(filterListElement);
            }
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        for (Element child : element.getChildren()) {
            DatasetFilterGroup filterGroup = new DatasetFilterGroup(getProject());
            filterGroup.readConfiguration(child);
            addFilterGroup(filterGroup);
        }
    }

}
