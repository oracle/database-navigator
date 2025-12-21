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

package com.dbn.object.factory.model;

import com.dbn.common.data.Data;
import com.dbn.language.common.QuotePair;
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
public class DBObjectSpec extends DBObjectSpecBase{
    private final DBObjectType objectType;

    @NonNls
    private String objectName;
    private int index;
    private boolean readonly;

    private final Map<DBObjectType, List<DBObjectSpec>> children = new EnumMap<>(DBObjectType.class);
    private final Map<DBObjectAttribute, Object> attributes = new HashMap<>();

    public DBObjectSpec(DBObjectType objectType) {
        this.objectType = objectType;
    }

    public <T> void setAttribute(DBObjectAttribute<T> attribute, @NonNls T value) {
        attributes.put(attribute, value);
    }

    @Nullable
    public <T> String getStringAttribute(DBObjectAttribute<T> attribute) {
        T value = getAttribute(attribute);
        return Data.asString(value);
    }

    public <T> T getAttribute(DBObjectAttribute<T> attribute) {
        return cast(attributes.get(attribute));
    }

    public void addChild(DBObjectSpec child) {
        DBObjectType objectType = child.getObjectType();
        List<DBObjectSpec> children = this.children.computeIfAbsent(objectType, t -> new ArrayList<>());
        child.setParent(this);
        children.add(child);
    }

    public List<DBObjectSpec> getChildren(DBObjectType type) {
        List<DBObjectSpec> children = this.children.get(type);
        return children == null ? emptyList() : children;
    }

    public String getObjectPath() {
        return objectName;
    }

    public String getObjectTypeName() {
        return getObjectType().getName();
    }

    public String getObjectName(boolean quoted) {
        String objectName = getObjectName();
        if (!quoted) return objectName;

        QuotePair quotes = getConnection().getCompatibilityInterface().getDefaultIdentifierQuotes();
        return quotes.quote(objectName);
    }

    public String getObjectDescription() {
        return getObjectTypeName() + " \"" + getObjectPath() + "\"";
    }

    @Override
    public String toString() {
        return objectType.getName() + " " + getObjectName();
    }
}
