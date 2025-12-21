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

package com.dbn.object.factory.model.generic;

import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.util.Unsafe.cast;
import static java.util.Collections.emptyList;

@Getter
@Setter
public class DBObjectDefinition {
    private final DBObjectType objectType;
    private DBObjectDefinition parent;
    private String objectName;
    private boolean readonly;

    private final Map<DBObjectType, List<DBObjectDefinition>> children = new EnumMap<>(DBObjectType.class);
    private final Map<DBObjectAttribute, Object> attributes = new HashMap<>();

    public DBObjectDefinition(DBObjectType objectType) {
        this.objectType = objectType;
    }

    public <T> void addAttribute(DBObjectAttribute<T> attribute, @NonNls T value) {
        attributes.put(attribute, value);
    }

    @Nullable
    public <T> T getAttribute(DBObjectAttribute<T> attribute) {
        return cast(attributes.get(attribute));
    }

    public void addChild(DBObjectDefinition child) {
        DBObjectType objectType = child.getObjectType();
        List<DBObjectDefinition> children = this.children.computeIfAbsent(objectType, t -> new ArrayList<>());
        child.setParent(this);
        children.add(child);
    }

    public List<DBObjectDefinition> getChildren(DBObjectType type) {
        List<DBObjectDefinition> children = this.children.get(type);
        return children == null ? emptyList() : children;
    }

    @Override
    public String toString() {
        return objectType.getName() + " " + getObjectName();
    }
}
