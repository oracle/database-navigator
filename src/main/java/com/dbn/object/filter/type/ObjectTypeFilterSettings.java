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

package com.dbn.object.filter.type;

import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.browser.options.DatabaseBrowserSettings;
import com.dbn.common.filter.Filter;
import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.common.options.ProjectConfiguration;
import com.dbn.common.options.setting.BooleanSetting;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionId;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.filter.type.ui.ObjectTypeFilterSettingsForm;
import com.dbn.object.type.DBObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ObjectTypeFilterSettings extends BasicProjectConfiguration<ProjectConfiguration, ObjectTypeFilterSettingsForm> {
    private final List<ObjectTypeFilterSetting> settings = Lists.convert(
            DBObjectType.BROWSABLE_TYPES,
            t -> new ObjectTypeFilterSetting(this, t));

    private Map<DBObjectType, ObjectTypeFilterSetting> cache = new ConcurrentHashMap<>(settings.size());

    private final BooleanSetting useMasterSettings = new BooleanSetting("use-master-settings", true);

    private transient final ConnectionId connectionId;

    public ObjectTypeFilterSettings(ProjectConfiguration parent, @Nullable ConnectionId connectionId) {
        super(parent);
        this.connectionId = connectionId;
    }

    public ObjectTypeFilterSettings getMasterSettings() {
        if (isProjectLevel()) { // is project level
            return null;
        } else {
            DatabaseBrowserSettings databaseBrowserSettings = DatabaseBrowserSettings.getInstance(getProject());
            return databaseBrowserSettings.getFilterSettings().getObjectTypeFilterSettings();
        }
    }

    private boolean isProjectLevel() {
        return connectionId == null;
    }

    public boolean isUsingMasterSettings() {
        return useMasterSettings.value();
    }

    public void hideObjectType(DBObjectType objectType) {
        if (useMasterSettings.value()) {
            // copy master settings and switch off
            ObjectTypeFilterSettings masterSettings = getMasterSettings();
            for (ObjectTypeFilterSetting setting : masterSettings.getSettings()) {
                setObjectTypeVisible(setting.getObjectType(), setting.isSelected() );
            }
            useMasterSettings.setValue(false);
        }

        setObjectTypeVisible(objectType, false);
    }

    private void setObjectTypeVisible(DBObjectType objectType, boolean visible) {
        ObjectTypeFilterSetting setting = Lists.first(settings, s -> s.getObjectType() == objectType);
        if (setting == null) return;
        setting.setSelected(visible);
    }


    @NotNull
    @Override
    public ObjectTypeFilterSettingsForm createConfigurationEditor() {
        return new ObjectTypeFilterSettingsForm(this);
    }

    private final Filter<BrowserTreeNode> elementFilter = treeNode -> {
        if (treeNode == null) {
            return false;
        }

        if (treeNode instanceof DBObject object) {
            DBObjectType objectType = object.getObjectType();
            return isVisible(objectType);
        }

        if (treeNode instanceof DBObjectList objectList) {
            return isVisible(objectList.getObjectType());
        }

        return true;
    };

    private final Filter<DBObjectType> typeFilter = objectType -> objectType != null && isVisible(objectType);

    public boolean isVisible(DBObjectType objectType) {
        if (isProjectLevel()) return isSelected(objectType);

        ObjectTypeFilterSettings masterSettings = getMasterSettings();
        return useMasterSettings.value() ?
                masterSettings.isSelected(objectType) :
                isSelected(objectType);
    }

    private boolean isSelected(DBObjectType objectType) {
        ObjectTypeFilterSetting objectTypeEntry = getSetting(objectType);
        return objectTypeEntry == null || objectTypeEntry.isSelected();
    }

    public boolean isSelectable(DBObjectType objectType) {
        ObjectTypeFilterSetting objectTypeEntry = getSetting(objectType);
        return objectTypeEntry != null;
    }

    private void setVisible(DBObjectType objectType, boolean visible) {
        ObjectTypeFilterSetting objectTypeEntry = getSetting(objectType);
        if (objectTypeEntry != null) {
            objectTypeEntry.setSelected(visible);
        }
    }

    private ObjectTypeFilterSetting getSetting(DBObjectType objectType) {
        return cache.computeIfAbsent(objectType, k -> findSetting(k));
    }

    private ObjectTypeFilterSetting findSetting(DBObjectType objectType) {
        for (ObjectTypeFilterSetting objectTypeEntry : getSettings()) {
            DBObjectType visibleObjectType = objectTypeEntry.getObjectType();
            if (visibleObjectType == objectType || objectType.isInheriting(visibleObjectType)) {
                return objectTypeEntry;
            }
        }
        return null;
    }

    @Override
    public String getConfigElementName() {
        return "object-type-filter";
    }

    @Override
    public void readConfiguration(Element element) {
        useMasterSettings.readConfigurationAttribute(element);
        for (Element child : element.getChildren()) {
            String typeName = stringAttribute(child, "name");
            DBObjectType objectType = DBObjectType.get(typeName);
            if (objectType != null) {
                boolean enabled = Boolean.parseBoolean(stringAttribute(child, "enabled"));
                setVisible(objectType, enabled);
            }
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        if (!isProjectLevel()) {
            useMasterSettings.writeConfigurationAttribute(element);
        }

        for (ObjectTypeFilterSetting objectTypeEntry : getSettings()) {
            Element child = newElement(element, "object-type");
            child.setAttribute("name", objectTypeEntry.getObjectType().name());
            child.setAttribute("enabled", Boolean.toString(objectTypeEntry.isSelected()));
        }
    }
}
