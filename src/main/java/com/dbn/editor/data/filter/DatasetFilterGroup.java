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

import com.dbn.common.dispose.AlreadyDisposedException;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.common.options.ProjectConfiguration;
import com.dbn.common.ui.util.Listeners;
import com.dbn.common.ui.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.editor.data.filter.ui.DatasetFilterForm;
import com.dbn.object.DBDataset;
import com.dbn.object.DBSchema;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Naming.nextNumberedIdentifier;
import static java.util.stream.Collectors.toSet;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DatasetFilterGroup extends BasicProjectConfiguration<ProjectConfiguration, DatasetFilterForm> implements ListModel {
    private ConnectionId connectionId;
    private String datasetName;
    private transient DatasetFilter activeFilter;
    private transient final List<DatasetFilter> filters = new ArrayList<>();

    private transient final State state = new State();

    @Getter
    @Setter
    private static class State {
        private boolean changed;
        private final List<DatasetFilter> filtersCapture = new ArrayList<>();
        private final Listeners<ListDataListener> listeners = Listeners.create();
    }

    public DatasetFilterGroup(@NotNull Project project) {
        super(project);
    }

    public DatasetFilterGroup(@NotNull Project project, ConnectionId connectionId, String datasetName) {
        super(project);
        this.connectionId = connectionId;
        this.datasetName = datasetName;
    }

    public DatasetBasicFilter createBasicFilter(boolean interactive) {
        String name = createFilterName("Filter");
        DatasetBasicFilter filter = new DatasetBasicFilter(this, name);
        filter.addCondition(new DatasetBasicFilterCondition(filter, null, null, ConditionOperator.EQUAL));
        initChange();
        addFilter(filter, interactive);
        return filter;
    }

    @NotNull
    DatasetBasicFilter createBasicFilter(String columnName, Object columnValue, ConditionOperator operator, boolean interactive) {
        String name = createFilterName("Filter");
        DatasetBasicFilter filter = new DatasetBasicFilter(this, name);
        if (columnValue != null) filter.setName(columnValue.toString());
        DatasetBasicFilterCondition condition = interactive ?
                new DatasetBasicFilterCondition(filter, columnName, columnValue, operator, true) :
                new DatasetBasicFilterCondition(filter, columnName, columnValue, operator);
        filter.addCondition(condition);

        if (interactive) initChange();
        addFilter(filter, interactive);
        return filter;
    }

    @NotNull
    DatasetBasicFilter createBasicFilter(String columnName, Object columnValue, ConditionOperator operator) {
        String name = createFilterName("Filter");
        DatasetBasicFilter filter = new DatasetBasicFilter(this, name);
        if (columnValue != null) filter.setName(columnValue.toString());
        DatasetBasicFilterCondition condition = new DatasetBasicFilterCondition(filter, columnName, columnValue, operator, true);
        filter.addCondition(condition);
        addFilter(filter, false);
        return filter;
    }


    public String createFilterName(String baseName) {
        return nextNumberedIdentifier(baseName, true, () -> getFilterNames());
    }

    private Set<String> getFilterNames() {
        return getFilters().stream().map(f -> f.getName()).collect(toSet());
    }

    public DatasetCustomFilter createCustomFilter(boolean interactive) {
        String name = createFilterName("Filter");
        DatasetCustomFilter filter = new DatasetCustomFilter(this, name);
        initChange();
        addFilter(filter, interactive);
        return filter;
    }

    public DatasetFilter getFilter(String filterId) {
        for (DatasetFilter filter : filters) {
            if (Objects.equals(filter.getId(), filterId)) {
                return filter;
            }
        }
        if (Objects.equals(filterId, DatasetFilterManager.EMPTY_FILTER.getId())) {
            return DatasetFilterManager.EMPTY_FILTER;            
        }
        return null;
    }


    public void deleteFilter(DatasetFilter filter) {
        initChange();
        int index = getFilters().indexOf(filter);
        getFilters().remove(index);
        filter.disposeUIResources();
        Lists.notifyListDataListeners(this, state.listeners, index, index, ListDataEvent.INTERVAL_REMOVED);

    }

    private void addFilter(DatasetFilter filter, boolean interactive) {
        int index = getFilters().size();
        if (!interactive) {
            // allow only one temporary filter
            clearTemporaryFilters();
        }
        getFilters().add(filter);
        if (interactive) {
            Lists.notifyListDataListeners(this, state.listeners, index, index, ListDataEvent.INTERVAL_ADDED);
        }
    }

    private void clearTemporaryFilters() {
        getFilters().removeIf(DatasetFilter::isTemporary);
    }

    public void moveFilterUp(DatasetFilter filter) {
        initChange();
        int index = getFilters().indexOf(filter);
        if (index > 0) {
            getFilters().remove(filter);
            getFilters().add(index-1, filter);
            Lists.notifyListDataListeners(this, state.listeners, index-1, index, ListDataEvent.CONTENTS_CHANGED);
        }
    }

    public void moveFilterDown(DatasetFilter filter) {
        initChange();
        int index = getFilters().indexOf(filter);
        if (index < getFilters().size()-1) {
            getFilters().remove(filter);
            getFilters().add(index + 1, filter);
            Lists.notifyListDataListeners(this, state.listeners, index, index + 1, ListDataEvent.CONTENTS_CHANGED);
        }

    }


    @NotNull
    public DBDataset lookupDataset() {
        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        if (connection != null) {
            int index = datasetName.lastIndexOf('.');
            String schemaName = datasetName.substring(0, index);
            DBSchema schema = connection.getObjectBundle().getSchema(schemaName);
            if (schema != null) {
                String name = datasetName.substring(index + 1);
                DBDataset dataset = schema.getDataset(name);
                return Failsafe.nn(dataset);
            }
        }
        throw AlreadyDisposedException.INSTANCE;
    }

    private void initChange() {
        if (!state.changed) {
            state.filtersCapture.addAll(filters);
            state.changed = true;
        }
    }

    /****************************************************
     *                   Configuration                  *
     ****************************************************/
    @Override
    public void apply() throws ConfigurationException {
        if (state.changed) {
            filters.clear();
            filters.addAll(state.filtersCapture);
            state.filtersCapture.clear();
            state.changed = false;
            if (!filters.contains(activeFilter)) {
                activeFilter = null;
            }
        }
        for (DatasetFilter filter : filters) {
            filter.apply();
        }
    }

    @Override
    public void reset() {
        if (state.changed) {
            state.filtersCapture.clear();
            state.changed = false;
        }
        for (DatasetFilter filter : filters) {
            filter.reset();
        }
    }

    @Override
    public void disposeUIResources() {
        for (DatasetFilter filter :filters) {
            filter.disposeUIResources();
        }
        for (DatasetFilter filter :state.filtersCapture) {
            filter.disposeUIResources();
        }
        state.listeners.clear();
        super.disposeUIResources();
    }

   @Override
   @NotNull
   public DatasetFilterForm createConfigurationEditor() {
       return new DatasetFilterForm(this, lookupDataset());
   }

    @Override
    public void readConfiguration(Element element) {
        connectionId = connectionIdAttribute(element, "connection-id");
        datasetName = stringAttribute(element, "dataset");
        for (Element child : element.getChildren()){
            String type = stringAttribute(child, "type");
            if (Objects.equals(type, "basic")) {
                DatasetFilter filter = new DatasetBasicFilter(this, null);
                filters.add(filter);
                filter.readConfiguration(child);
            } else if (Objects.equals(type, "custom")) {
                DatasetFilter filter = new DatasetCustomFilter(this, null);
                filters.add(filter);
                filter.readConfiguration(child);
            }
        }
        String activeFilterId = stringAttribute(element, "active-filter-id");
        activeFilter = getFilter(activeFilterId);
    }

    @Override
    public void writeConfiguration(Element element) {
        element.setAttribute("connection-id", connectionId.id());
        element.setAttribute("dataset", datasetName);
        for (DatasetFilter filter : filters) {
            Element filterElement = newElement(element, "filter");
            filter.writeConfiguration(filterElement);
        }
        element.setAttribute("active-filter-id", activeFilter == null ? "" : activeFilter.getId());
    }

   /*************************************************
    *                     ListModel                 *
    *************************************************/
   public List<DatasetFilter> getFilters() {
        return state.changed ? state.filtersCapture : filters;
   }

   @Override
   public int getSize() {
        return getFilters().size();
    }

    @Override
    public Object getElementAt(int index) {
        return getFilters().get(index);
    }

    @Override
    public void addListDataListener(ListDataListener listener) {
        state.listeners.add(listener);
    }

    @Override
    public void removeListDataListener(ListDataListener listener) {
        state.listeners.remove(listener);
    }
}
