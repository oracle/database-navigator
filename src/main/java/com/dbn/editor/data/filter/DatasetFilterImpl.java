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

import com.dbn.common.options.BasicConfiguration;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.connection.ConnectionId;
import com.dbn.object.DBDataset;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.text.StringUtil;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
public abstract class DatasetFilterImpl extends BasicConfiguration<DatasetFilterGroup, ConfigurationEditorForm> implements DatasetFilter {
    private final DatasetFilterGroup filterGroup;
    private final DatasetFilterType filterType;

    private String id;
    private String name;
    private String error;
    private boolean temporary = false;
    private boolean customNamed = false;
    private boolean persisted = false;


    private DatasetFilterImpl(DatasetFilterGroup filterGroup, String name, String id, DatasetFilterType filterType) {
        super(filterGroup);
        this.filterGroup = filterGroup;
        this.name = name;
        this.id = id;
        this.filterType = filterType;
    }

    DatasetFilterImpl(DatasetFilterGroup filterGroup, String name, DatasetFilterType filterType) {
        this(filterGroup, name, UUID.randomUUID().toString(), filterType);
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    public void setName(String name) {
        this.name = StringUtil.first(name, 40, true);
    }

    @Override
    public ConnectionId getConnectionId() {
        return filterGroup.getConnectionId();
    }

    @Override
    public String getDatasetName() {
        return filterGroup.getDatasetName();
    }

    public abstract void generateName();

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof DatasetFilter) {
            DatasetFilter remote = (DatasetFilter) obj;
            return Objects.equals(remote.getFilterGroup(), filterGroup) &&
                   Objects.equals(remote.getId(), id);
        }
        return false;
    }

    @Nullable
    public DBDataset lookupDataset() {
        return filterGroup.lookupDataset();
    }

    @Override
    public void apply() throws ConfigurationException {
        super.apply();
        temporary = false;
        persisted = true;
    }

    /****************************************************
     *                   Configuration                  *
     ****************************************************/
    @Override
    public void readConfiguration(Element element) {
        id = stringAttribute(element, "id");
        name = stringAttribute(element, "name");
        temporary = booleanAttribute(element, "temporary", false);
        customNamed = booleanAttribute(element, "custom-name", false);
        persisted = true;
    }

    @Override
    public void writeConfiguration(Element element) {
        element.setAttribute("id", id);
        element.setAttribute("name", name);
        element.setAttribute("temporary", Boolean.toString(temporary));
        element.setAttribute("custom-name", Boolean.toString(customNamed));
    }

}
