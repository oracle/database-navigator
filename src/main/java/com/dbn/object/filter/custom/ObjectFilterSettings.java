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

package com.dbn.object.filter.custom;

import com.dbn.common.expression.ExpressionEvaluator;
import com.dbn.common.expression.GroovyExpressionEvaluator;
import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionFilterSettings;
import com.dbn.object.common.DBObject;
import com.dbn.object.filter.custom.ui.ObjectFilterSettingsForm;
import com.dbn.object.type.DBObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@EqualsAndHashCode(callSuper = false)
public class ObjectFilterSettings extends BasicProjectConfiguration<ConnectionFilterSettings, ObjectFilterSettingsForm> {
    private final ConnectionId connectionId;
    private final EnumMap<DBObjectType, ObjectFilter<?>> filters = new EnumMap<>(DBObjectType.class);
    private final transient ExpressionEvaluator expressionEvaluator = new GroovyExpressionEvaluator();

    public ObjectFilterSettings(ConnectionFilterSettings parent, ConnectionId connectionId) {
        super(parent);
        this.connectionId = connectionId;
    }

    public <T extends DBObject> ObjectFilter<T> getFilter(DBObjectType objectType) {
        return cast(filters.get(objectType));
    }

    public void addFilter(ObjectFilter<?> filter) {
        filters.put(filter.getObjectType(), filter);
    }

    public <T extends DBObject> ObjectFilter<T> createFilter(DBObjectType objectType, String expression) {
        ObjectFilter<T> filter = new ObjectFilter<>(this);
        filter.setObjectType(objectType);
        filter.setExpression(expression);

        filters.put(objectType, filter);
        return filter;
    }
    public <T extends DBObject> ObjectFilter<?> deleteFilter(DBObjectType objectType) {
        return filters.remove(objectType);
    }

    public void setFilters(List<ObjectFilter<?>> filters) {
        this.filters.clear();
        filters.forEach(f -> this.filters.put(f.getObjectType(), f));
    }

    public List<ObjectFilter<?>> getFilters() {
        return new ArrayList<>(filters.values());
    }

    public Set<DBObjectType> getFilterObjectTypes() {
        return filters.keySet();
    }

    /*********************************************************
     *                     Configuration                     *
     *********************************************************/
    @NotNull
    @Override
    public ObjectFilterSettingsForm createConfigurationEditor() {
        return new ObjectFilterSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "object-filters";
    }

    @Override
    public void readConfiguration(Element element) {
        filters.clear();
        for (Element child: element.getChildren()) {
            ObjectFilter<?> filter = new ObjectFilter<>(this);
            filter.readConfiguration(child);

            filters.put(filter.getObjectType(), filter);
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        for (ObjectFilter<?> filter : filters.values()) {
            Element child = newElement(element, "filter");
            filter.writeConfiguration(child);
        }

    }

    public ConnectionHandler getConnection() {
        return ConnectionHandler.get(connectionId);
    }
}
