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

package com.dbn.browser.options;

import com.dbn.browser.options.ui.DatabaseBrowserSortingSettingsForm;
import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.sorting.DBObjectComparator;
import com.dbn.object.common.sorting.DBObjectComparators;
import com.dbn.object.common.sorting.SortingType;
import com.dbn.object.type.DBObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.cachedUpperCase;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DatabaseBrowserSortingSettings
        extends BasicProjectConfiguration<DatabaseBrowserSettings, DatabaseBrowserSortingSettingsForm> {

    private Map<DBObjectType, DBObjectComparator> comparators = new LinkedHashMap<>();

    DatabaseBrowserSortingSettings(DatabaseBrowserSettings parent) {
        super(parent);
        comparators.put(DBObjectType.COLUMN, DBObjectComparators.predefined(DBObjectType.COLUMN, SortingType.NAME));
        comparators.put(DBObjectType.FUNCTION, DBObjectComparators.predefined(DBObjectType.FUNCTION, SortingType.NAME));
        comparators.put(DBObjectType.PROCEDURE, DBObjectComparators.predefined(DBObjectType.PROCEDURE, SortingType.NAME));
        comparators.put(DBObjectType.ARGUMENT, DBObjectComparators.predefined(DBObjectType.ARGUMENT, SortingType.POSITION));
        comparators.put(DBObjectType.TYPE_ATTRIBUTE, DBObjectComparators.predefined(DBObjectType.TYPE_ATTRIBUTE, SortingType.POSITION));
    }

    public <T extends DBObject> DBObjectComparator<T> getComparator(DBObjectType objectType) {
        for (DBObjectType key : comparators.keySet()) {
            if (key.matches(objectType)) {
                return comparators.get(key);
            }
        }
        return null;
    }

    public Collection<DBObjectComparator> getComparators() {
        return comparators.values();
    }

    public void setComparators(Collection<DBObjectComparator> comparators) {
        Map<DBObjectType, DBObjectComparator> newComparators = new LinkedHashMap<>();
        for (DBObjectComparator comparator : comparators) {
            newComparators.put(comparator.getObjectType(), comparator);
        }

        this.comparators = newComparators;
    }

    @NotNull
    @Override
    public DatabaseBrowserSortingSettingsForm createConfigurationEditor() {
        return new DatabaseBrowserSortingSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "sorting";
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.browser.title.SortingSettings");
    }

    @Override
    public String getHelpTopic() {
        return "browserSettings";
    }

    /*********************************************************
     *                     Configuration                     *
     *********************************************************/

    @Override
    public void readConfiguration(Element element) {
        Map<DBObjectType, DBObjectComparator> newComparators = new LinkedHashMap<>();
        List<Element> children = element.getChildren();
        for (Element child : children) {
            String objectTypeName = stringAttribute(child, "name");
            String sortingTypeName = stringAttribute(child, "sorting-type");
            DBObjectType objectType = DBObjectType.get(objectTypeName);
            SortingType sortingType = SortingType.valueOf(sortingTypeName);
            DBObjectComparator comparator = DBObjectComparators.predefined(objectType, sortingType);
            if (comparator != null) {
                newComparators.put(comparator.getObjectType(), comparator);
            }
        }
        for (DBObjectComparator comparator : comparators.values()) {
            DBObjectType objectType = comparator.getObjectType();
            if (!newComparators.containsKey(objectType)) {
                newComparators.put(objectType, comparator);
            }
        }
        comparators = newComparators;
    }

    @Override
    public void writeConfiguration(Element element) {
        for (DBObjectComparator comparator : comparators.values()) {
            Element child = newElement(element, "object-type");
            child.setAttribute("name", cachedUpperCase(comparator.getObjectType().getName()));
            child.setAttribute("sorting-type", comparator.getSortingType().name());
        }
    }
}
