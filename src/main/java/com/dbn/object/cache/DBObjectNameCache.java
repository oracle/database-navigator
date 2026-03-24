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

package com.dbn.object.cache;

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.filter.Filter;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.thread.Background;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jdom.Element;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.constantAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.intellij.util.containers.ContainerUtil.newConcurrentSet;

@NoArgsConstructor
public class DBObjectNameCache<T extends DBSchemaObject> implements PersistentStateElement, Filter<T> {
    private @Getter ConnectionId connectionId;
    private @Getter DBObjectType objectType;
    private @Getter DBObjectFilterType filterType;
    private final Map<SchemaId, Set<String>> cache = new ConcurrentHashMap<>();

    public DBObjectNameCache(ConnectionId connectionId, DBObjectType objectType, DBObjectFilterType filterType) {
        this.connectionId = connectionId;
        this.objectType = objectType;
        this.filterType = filterType;
    }

    public boolean accepts(T object) {
        if (object == null) return false;

        SchemaId schemaId = object.getSchemaId();
        Set<String> objectNames = cache.computeIfAbsent(schemaId, s -> load(schemaId));
        return objectNames.contains(object.getName());
    }

    public void addObjectName(SchemaId schemaId, String objectName) {
        Set<String> objectNames = cache.computeIfAbsent(schemaId, s -> newConcurrentSet());
        objectNames.add(objectName);
    }

    private Set<String> getObjectNames(SchemaId schemaId) {
        return cache.computeIfAbsent(schemaId, s -> newConcurrentSet());
    }

    public void refresh(SchemaId schemaId) {
        Background.run(() -> reload(schemaId));
    }

    private void reload(SchemaId schemaId) {
        Set<String> objectNames = load(schemaId);
        cache.put(schemaId, objectNames);

        ProjectEvents.notify(getProject(),
                DBObjectNameCacheListener.TOPIC,
                (l) -> l.contentsChanged(connectionId, schemaId, objectType, filterType));

    }

    private Set<String> load(SchemaId schemaId) {
        DBObjectFilter<DBObject> filter = DBObjectFilters.get(filterType);
        ConnectionHandler connection = getConnection();

        DBSchema schema = nd(connection.getSchema(schemaId));

        List<DBObject> objects = schema.getChildObjects(objectType);
        Set<String> objectNames = newConcurrentSet();
        for (DBObject object : objects) {
            if (filter.accepts(object)) {
                objectNames.add(object.getName());
            }
        }

        return objectNames;
    }

    public List<T> filter(List<T> objects) {
        return Lists.filter(objects, o -> accepts(o));
    }

    private ConnectionHandler getConnection() {
        return ConnectionHandler.ensure(connectionId);
    }

    private Project getProject() {
        return getConnection().getProject();
    }

    @Override
    public void readState(Element element) {
        connectionId = connectionIdAttribute(element, "connection-id");
        objectType = enumAttribute(element, "object-type", DBObjectType.class);
        filterType = constantAttribute(element, "filter-type", DBObjectFilterType.class);
        List<Element> schemaElements = childrenOf(element, "schema");
        for (Element schemaElement : schemaElements) {
            SchemaId schemaId = constantAttribute(schemaElement, "schema-id", SchemaId.class);
            List<Element> tableElements = childrenOf(schemaElement, "table");
            for (Element tableElement : tableElements) {
                String tableName = stringAttribute(tableElement, "name");
                Set<String> tableNames = cache.computeIfAbsent(schemaId, s -> new HashSet<>());
                tableNames.add(tableName);
            }
            refresh(schemaId);
        }
    }

    @Override
    public void writeState(Element element) {
        setConstantAttribute(element, "connection-id", connectionId);
        setEnumAttribute(element, "object-type", objectType);
        setConstantAttribute(element, "filter-type", filterType);

        for (SchemaId schemaId : cache.keySet()) {
            Element schemaElement = newElement(element, "schema");
            setConstantAttribute(schemaElement, "schema-id", schemaId);
            Set<String> tableNames = cache.get(schemaId);
            for (String tableName : tableNames) {
                Element tableElement = newElement(schemaElement, "table");
                setStringAttribute(tableElement, "name", tableName);
            }
        }
    }
}
