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

package com.dbn.assistant.tool;

import com.dbn.common.component.ConnectionComponent;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;


@Getter
public abstract class AssistantToolBase extends ConnectionComponent implements AssistantTool{
    private AssistantToolType type;
    private AssistantToolCategory category;
    private String description;

    @Override
    public void initialize(ConnectionHandler connection, AssistantToolType type, AssistantToolCategory category, String description) {
        initialize(connection);
        this.type = type;
        this.category = category;
        this.description = description;
    }

    protected static <T extends DBObject> List<String> getObjectNames(List<T> objects, boolean qualified) {
        return getObjectNames(objects, qualified, o -> true);
    }

    protected static <T extends DBObject> List<String> getObjectNames(List<T> objects, boolean qualified, Predicate<T> filter) {
        return objects
                .stream()
                .filter(filter)
                .map(o -> qualified ? o.getQualifiedName() : o.getName())
                .collect(toList());
    }

    @NotNull
    protected DBSchema getSchema(String schemaName) {
        DBSchema schema = getConnection().getObjectBundle().getSchema(schemaName);
        return resolved(schema, DBObjectType.SCHEMA, schemaName);
    }

    protected static <T extends DBObject> T resolved(T object, DBObjectType objectType, String objectName) {
        if (object == null) throw new IllegalArgumentException(objectType.getCapitalizedName() + " not found: " + objectName);
        return object;
    }

    protected static <T extends DBObject> T undisposed(T object) {
        DBObjectRef<T> ref = DBObjectRef.of(object);
        return ref.get();
    }

    @Override
    public String toString() {
        return "AssistantTool{" +
                "type=" + type + ", " +
                "category=" + category + ", " +
                "description='" + description + '\'' +
                '}';
    }
}
